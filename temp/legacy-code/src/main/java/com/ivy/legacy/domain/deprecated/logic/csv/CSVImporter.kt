package com.ivy.legacy.domain.deprecated.logic.csv

import androidx.compose.ui.graphics.toArgb
import arrow.core.raise.either
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.data.backup.CSVRow
import com.ivy.data.backup.importFingerprint
import com.ivy.data.backup.ImportResult
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.dao.read.TransactionDao
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.AtomicWriteRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.design.IVY_COLOR_PICKER_COLORS_FREE
import com.ivy.design.l0_system.Green
import com.ivy.design.l0_system.IvyDark
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.legacy.datamodel.toEntity
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.csv.model.RowMapping
import com.ivy.wallet.domain.pure.util.nextOrderNum
import com.opencsv.CSVReaderBuilder
import com.opencsv.validators.LineValidator
import com.opencsv.validators.RowValidator
import kotlinx.collections.immutable.persistentListOf
import java.io.StringReader
import java.util.UUID
import javax.inject.Inject
import kotlin.math.absoluteValue
import com.ivy.legacy.datamodel.Account as LegacyAccount

class CSVImporter @Inject constructor(
    private val settingsDao: SettingsDao,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val transactionMapper: TransactionMapper,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val timeConverter: TimeConverter,
    private val atomicWrite: AtomicWriteRepository,
) {

    lateinit var accounts: List<LegacyAccount>
    lateinit var categories: List<Category>

    private var newCategoryColorIndex = 0
    private var newAccountColorIndex = 0

    suspend fun import(
        csv: String,
        rowMapping: RowMapping,
        onProgress: suspend (progressPercent: Double) -> Unit,
    ): ImportResult {
        val csvReader = CSVReaderBuilder(StringReader(csv))
            .withSkipLines(1)
            .withLineValidator(object : LineValidator {
                override fun isValid(line: String?): Boolean {
                    return true
                }

                override fun validate(line: String?) {
                    // do nothing
                }
            })
            .withRowValidator(object : RowValidator {
                override fun isValid(row: Array<out String>?): Boolean {
                    return true
                }

                override fun validate(row: Array<out String>?) {
                    // do nothing
                }
            })
            .build()

        val rows = csvReader.use { it.readAll().map { row -> row.toList() } }
        return atomicWrite.run { importRows(rows, rowMapping, onProgress) }
    }

    private suspend fun importRows(
        rows: List<List<String>>,
        rowMapping: RowMapping,
        onProgress: suspend (Double) -> Unit,
    ): ImportResult {
        val rowsCount = rows.size

        newCategoryColorIndex = 0
        newAccountColorIndex = 0

        accounts = accountDao.findAll().map { it.toLegacyDomain() }
        val initialAccountsCount = accounts.size

        categories = categoryRepository.findAll()
        val initialCategoriesCount = categories.size

        val baseCurrency = settingsDao.findFirst().currency
        val importedFingerprints = transactionDao.findAll()
            .map { it.importFingerprint() }
            .toMutableSet()

        val failedRows = persistentListOf<CSVRow>().builder()
        val sourceRows = mutableMapOf<UUID, CSVRow>()
        val signedAmounts = mutableMapOf<UUID, Double>()

        // Some importers require postprocessing transactions
        // Example: Financisto separates in and out transfers
        val mappedTransactions = rows.mapIndexedNotNull { index, row ->
            val progressPercent = if (rowsCount > 0) {
                index / rowsCount.toDouble()
            } else {
                0.0
            }
            onProgress(progressPercent / 2)

            val mapped = mapToTransaction(
                baseCurrency = baseCurrency,
                row = row,
                rowMapping = rowMapping
            )

            val transaction = mapped?.first
            if (transaction == null) {
                failedRows.add(
                    CSVRow(
                        index = index + 2, // Header is line 1; source lines are one-based.
                        content = row
                    )
                )
            } else {
                sourceRows[transaction.id] = CSVRow(index + 2, row)
                signedAmounts[transaction.id] = mapped.second
            }
            transaction
        }
        val joinResult = rowMapping.joinTransactions(mappedTransactions, signedAmounts)

        val transactions = joinResult.transactions

        var importedCount = 0
        var duplicatesSkipped = 0
        for ((index, transaction) in transactions.withIndex()) {
            val progressPercent = if (rowsCount > 0) {
                index / transactions.size.toDouble()
            } else {
                0.0
            }
            onProgress(0.5 + progressPercent / 2)
            with(transactionMapper) {
                val entity = transaction.toEntity()
                if (!importedFingerprints.add(entity.importFingerprint())) {
                    duplicatesSkipped++
                } else {
                    entity.toDomain().getOrNull()?.let {
                        transactionRepository.save(it)
                        importedCount++
                    } ?: run {
                        importedFingerprints.remove(entity.importFingerprint())
                        sourceRows[transaction.id]?.let(failedRows::add)
                    }
                }
            }
        }

        return ImportResult(
            rowsFound = rowsCount - joinResult.mergedCount,
            transactionsImported = importedCount,
            accountsImported = accounts.size - initialAccountsCount,
            categoriesImported = categories.size - initialCategoriesCount,
            failedRows = failedRows.build(),
            duplicatesSkipped = duplicatesSkipped
        )
    }

    private suspend fun mapToTransaction(
        baseCurrency: String,
        row: List<String>,
        rowMapping: RowMapping
    ): Pair<Transaction, Double>? {
        val type = mapType(
            row = row,
            rowMapping = rowMapping
        ) ?: return null

        val csvAmount = if (type != TransactionType.TRANSFER) {
            parseImportedCsvAmount(row.extract(rowMapping.amount))
        } else {
            parseImportedCsvAmount(row.extract(rowMapping.transferAmount))
        } ?: return null
        val amount = csvAmount.absoluteValue

        if (amount <= 0) {
            // Cannot save transactions with zero amount
            return null
        }

        val toAmount = if (type == TransactionType.TRANSFER) {
            parseImportedCsvAmount(row.extract(rowMapping.toAmount))
        } else {
            null
        }

        if (type == TransactionType.TRANSFER &&
            !row.extract(rowMapping.toAmount).isNullOrBlank() &&
            (toAmount == null || toAmount <= 0)) return null

        val sourceName = row.extract(rowMapping.account)?.trim()?.takeIf(String::isNotEmpty)
            ?: return null
        val destinationName = if (type == TransactionType.TRANSFER && rowMapping.toAccount != null) {
            row.extract(rowMapping.toAccount)?.trim()?.takeIf(String::isNotEmpty)
                ?: return null
        } else null
        if (destinationName != null) {
            if (sourceName.equals(destinationName, ignoreCase = true)) return null
        }
        val sourceCurrency = resolveImportedCsvCurrency(sourceName,
            row.extract(rowMapping.accountCurrency), baseCurrency, accounts) ?: return null
        val destinationCurrency = if (destinationName != null) {
            resolveImportedCsvCurrency(destinationName, row.extract(rowMapping.toAccountCurrency),
                baseCurrency, accounts) ?: return null
        } else null
        if (destinationCurrency != null && sourceCurrency != destinationCurrency && toAmount == null) return null

        val dateString = if (rowMapping.timeOnly != null) {
            // Date and time are separated in CSV; join only supplied values.
            listOfNotNull(row.extract(rowMapping.date), row.extract(rowMapping.timeOnly))
                .filter(String::isNotBlank).joinToString(" ")
        } else {
            row.extract(rowMapping.date)
        }
        val dueDateString = row.extract(rowMapping.dueDate)
        val dateTime = parseImportedCsvTimestamp(rowMapping, dateString, timeConverter)
        val dueDate = parseImportedCsvTimestamp(rowMapping, dueDateString, timeConverter)
        // A bad nonempty field is not an absent field: do not silently turn a
        // completed record into a planned one, or discard an invalid due date.
        if ((!dateString.isNullOrBlank() && dateTime == null) ||
            (!dueDateString.isNullOrBlank() && dueDate == null)) return null
        if (dateTime == null && dueDate == null) {
            // Cannot save transactions without any date
            return null
        }

        val toAccount = if (type == TransactionType.TRANSFER) {
            mapAccount(
                baseCurrency = baseCurrency,
                accountNameString = destinationName,
                currencyRawString = destinationCurrency,
                color = row.extract(rowMapping.toAccountColor)?.toIntOrNull(),
                icon = row.extract(rowMapping.toAccountIcon),
                orderNum = row.extract(rowMapping.toAccountOrderNum)?.toDoubleOrNull()
            )
        } else {
            null
        }

        val account = mapAccount(
            baseCurrency = baseCurrency,
            accountNameString = sourceName,
            currencyRawString = sourceCurrency,
            color = row.extract(rowMapping.accountColor)?.toIntOrNull(),
            icon = row.extract(rowMapping.accountIcon),
            orderNum = row.extract(rowMapping.accountOrderNum)?.toDoubleOrNull()
        ) ?: return null

        val category = mapCategory(
            categoryNameString = row.extract(rowMapping.category),
            color = row.extract(rowMapping.categoryColor)?.toIntOrNull(),
            icon = row.extract(rowMapping.categoryIcon),
            orderNum = row.extract(rowMapping.categoryOrderNum)?.toDoubleOrNull()
        )
        val title = row.extract(rowMapping.title)
        val description = row.extract(rowMapping.description)
        val id = mapId(row.extract(rowMapping.id))

        return rowMapping.transformTransaction(
            Transaction(
                id = id,
                type = type,
                amount = amount.toBigDecimal(),
                accountId = account.id,
                toAccountId = toAccount?.id,
                toAmount = toAmount?.toBigDecimal() ?: amount.toBigDecimal(),
                dateTime = dateTime,
                dueDate = dueDate,
                categoryId = category?.id?.value,
                title = title,
                description = description
            ),
            category,
            csvAmount
        ) to csvAmount
    }

    private fun mapType(
        row: List<String>,
        rowMapping: RowMapping
    ): TransactionType? {
        // Return Expense for intentionally set Type mapping to null
        // Example: Fortune City
        if (rowMapping.type == null) return TransactionType.EXPENSE

        val type = row.extract(rowMapping.type) ?: return null
        // default is expense as some apps only declare transfers
        if (type.isBlank()) return TransactionType.EXPENSE

        val normalizedType = type.toLowerCaseLocal()

        return when {
            normalizedType.contains("income") -> TransactionType.INCOME
            normalizedType.contains("expense") -> TransactionType.EXPENSE
            normalizedType.contains("transfer") -> TransactionType.TRANSFER
            else -> {
                // Default to Expense because Financisto messed up its CSV Export
                // and mixes it with another (ignored) column
                if (rowMapping.defaultTypeToExpense) TransactionType.EXPENSE else null
            }
        }
    }


    @Suppress("ReturnCount")
    private suspend fun mapAccount(
        baseCurrency: String,
        accountNameString: String?,
        color: Int?,
        icon: String?,
        orderNum: Double?,
        currencyRawString: String?,
    ): LegacyAccount? {
        if (accountNameString == null || accountNameString.isBlank()) return null

        val existingAccount = accounts.firstOrNull {
            accountNameString.toLowerCaseLocal() == it.name.toLowerCaseLocal()
        }
        if (existingAccount != null) {
            return existingAccount
        }

        // create new account
        val colorArgb = color ?: when {
            accountNameString.toLowerCaseLocal().contains("cash") -> {
                Green
            }

            accountNameString.toLowerCaseLocal().contains("revolut") -> {
                IvyDark
            }

            else -> IVY_COLOR_PICKER_COLORS_FREE.getOrElse(newAccountColorIndex++) {
                newAccountColorIndex = 0
                IVY_COLOR_PICKER_COLORS_FREE.first()
            }
        }.toArgb()

        val newAccount = LegacyAccount(
            name = accountNameString,
            currency = mapCurrency(
                baseCurrency = baseCurrency,
                currencyCode = currencyRawString
            ),
            color = colorArgb,
            icon = icon,
            orderNum = orderNum ?: accountDao.findMaxOrderNum().nextOrderNum()
        )
        val domainAccount = newAccount.toDomainAccount(currencyRepository).getOrNull()
            ?: return null
        accountRepository.save(domainAccount)
        accounts = accountDao.findAll().map { it.toLegacyDomain() }

        return newAccount
    }

    private fun mapCurrency(
        baseCurrency: String,
        currencyCode: String?
    ): String {
        return try {
            if (currencyCode != null && currencyCode.isNotBlank()) {
                IvyCurrency.fromCode(currencyCode)?.code ?: baseCurrency
            } else {
                baseCurrency
            }
        } catch (e: Exception) {
            baseCurrency
        }
    }

    private suspend fun mapCategory(
        categoryNameString: String?,
        color: Int?,
        icon: String?,
        orderNum: Double?
    ): Category? {
        if (categoryNameString == null || categoryNameString.isBlank()) return null

        val existingCategory = categories.firstOrNull {
            categoryNameString.toLowerCaseLocal() == it.name.value.toLowerCaseLocal()
        }
        if (existingCategory != null) {
            return existingCategory
        }

        // create new category
        val colorArgb = color ?: IVY_COLOR_PICKER_COLORS_FREE.getOrElse(newCategoryColorIndex++) {
            newCategoryColorIndex = 0
            IVY_COLOR_PICKER_COLORS_FREE.first()
        }.toArgb()

        val newCategory: Category? = either {
            Category(
                name = NotBlankTrimmedString.from(categoryNameString).bind(),
                color = ColorInt(colorArgb),
                icon = icon?.let(IconAsset::from)?.getOrNull(),
                orderNum = orderNum ?: categoryRepository.findMaxOrderNum().nextOrderNum(),
                id = CategoryId(UUID.randomUUID()),
            )
        }.getOrNull()

        if (newCategory != null) {
            categoryRepository.save(newCategory)
            categories = categoryRepository.findAll()
        }

        return newCategory
    }

    private fun mapId(idString: String?): UUID {
        return try {
            if (idString.isNullOrBlank()) UUID.randomUUID()

            UUID.fromString(idString)
        } catch (e: Exception) {
            UUID.randomUUID()
        }
    }

    private fun List<String>.extract(index: Int?): String? =
        index?.let { this.getOrNull(index) }
}
