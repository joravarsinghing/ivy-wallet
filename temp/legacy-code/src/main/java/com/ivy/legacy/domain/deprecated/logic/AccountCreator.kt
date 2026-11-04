package com.ivy.legacy.domain.deprecated.logic

import androidx.compose.ui.graphics.toArgb
import arrow.core.raise.either
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.AccountWriteRepository
import com.ivy.data.repository.AccountWriteConflict
import com.ivy.data.repository.accountOpeningTransactionId
import com.ivy.base.threading.DispatchersProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import com.ivy.data.repository.CurrencyRepository
import com.ivy.legacy.utils.ioThread
import com.ivy.wallet.domain.deprecated.logic.WalletAccountLogic
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.pure.util.nextOrderNum
import javax.inject.Inject
import com.ivy.legacy.datamodel.Account as LegacyAccount

class AccountCreator @Inject constructor(
    private val accountLogic: WalletAccountLogic,
    private val accountDao: AccountDao,
    private val accountWriter: AccountWriteRepository,
    private val currencyRepository: CurrencyRepository,
    private val dispatchers: DispatchersProvider,
) {

    suspend fun createAccount(data: CreateAccountData, onRefreshUI: suspend () -> Unit) =
        createAccount(data, { it.getOrThrow() }, onRefreshUI)

    suspend fun createAccount(
        data: CreateAccountData,
        onResult: (Result<Unit>) -> Unit,
        onRefreshUI: suspend () -> Unit
    ) = acknowledge(onResult, onRefreshUI) {
        require(data.balance.isFinite()) { "Account balance must be finite." }
        ioThread {
            val account = either {
                Account(
                    id = AccountId(value = data.id),
                    name = NotBlankTrimmedString.from(data.name).bind(),
                    asset = AssetCode.from(data.currency).bind(),
                    color = ColorInt(data.color.toArgb()),
                    icon = data.icon?.let(IconAsset::from)?.getOrNull(),
                    includeInBalance = data.includeBalance,
                    orderNum = accountDao.findMaxOrderNum().nextOrderNum(),
                )
            }.getOrNull() ?: throw IllegalArgumentException("Invalid account details.")

            val legacyAccount = LegacyAccount(
                name = data.name,
                currency = data.currency,
                color = data.color.toArgb(),
                icon = data.icon,
                includeInBalance = data.includeBalance,
                orderNum = account.orderNum,
                isSynced = false,
                id = account.id.value
            )
            accountWriter.create(account, data.balance) {
                accountLogic.initializeBalance(
                    account = legacyAccount,
                    newBalance = data.balance,
                    transactionId = accountOpeningTransactionId(account.id.value)
                )
            }
        }

    }

    suspend fun editAccount(legacyAccount: LegacyAccount, newBalance: Double, onRefreshUI: suspend () -> Unit) =
        editAccount(legacyAccount, newBalance, { it.getOrThrow() }, onRefreshUI)

    suspend fun editAccount(
        legacyAccount: LegacyAccount,
        newBalance: Double,
        onResult: (Result<Unit>) -> Unit,
        onRefreshUI: suspend () -> Unit
    ) = acknowledge(onResult, onRefreshUI) {
        require(newBalance.isFinite()) { "Account balance must be finite." }
        val updatedLegacyAccount = legacyAccount.copy(
            isSynced = false
        )
        ioThread {
            val account = legacyAccount.toDomainAccount(currencyRepository).getOrNull()
                ?: throw IllegalArgumentException("Invalid account details.")
            accountWriter.edit(account) {
                accountLogic.adjustBalance(
                    account = updatedLegacyAccount,
                    actualBalance = accountLogic.calculateAccountBalance(updatedLegacyAccount),
                    newBalance = newBalance
                )
            }
        }

    }

    private suspend fun acknowledge(onResult: (Result<Unit>) -> Unit, refresh: suspend () -> Unit,
        write: suspend () -> Unit) {
        try { write() } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            if (failure is AccountWriteConflict) refreshSafely(refresh)
            withContext(dispatchers.main) { onResult(Result.failure(failure)) }
            return
        }
        refreshSafely(refresh)
        withContext(dispatchers.main) { onResult(Result.success(Unit)) }
    }

    private suspend fun refreshSafely(refresh: suspend () -> Unit) {
        try { refresh() } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* A committed write is not undone by a failed refresh. */ }
    }
}
