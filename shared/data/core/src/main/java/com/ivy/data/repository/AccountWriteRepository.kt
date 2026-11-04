package com.ivy.data.repository

import androidx.room.withTransaction
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.db.IvyRoomDatabase
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.repository.mapper.AccountMapper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/** Inherited through dispatcher changes; Room thread-local checks alone are insufficient. */
internal class AccountWriteReads(account: Account) : AbstractCoroutineContextElement(Key) {
    object Key : CoroutineContext.Key<AccountWriteReads>
    private val reads = ConcurrentHashMap<AccountId, Account>().apply { put(account.id, account) }
    fun cached(id: AccountId): Account? = reads[id]
    fun remember(account: Account) { reads.putIfAbsent(account.id, account) }
}

fun accountOpeningTransactionId(id: UUID): UUID =
    UUID.nameUUIDFromBytes("jvy-account-opening:$id".toByteArray(Charsets.UTF_8))

sealed class AccountWriteConflict : IllegalStateException() {
    class AlreadySaved : AccountWriteConflict()
    class MissingAccount : AccountWriteConflict()
}

/** Commits metadata and its balance adjustment before publishing any account event. */
@Singleton
class AccountWriteRepository internal constructor(
    private val transaction: suspend (suspend () -> Unit) -> Unit,
    private val writeAccount: suspend (AccountEntity) -> Unit,
    private val accounts: AccountRepository,
    private val mapper: AccountMapper,
    private val dispatchers: DispatchersProvider,
    private val readAccount: suspend (UUID) -> AccountEntity? = { null },
    private val readOpening: suspend (UUID) -> TransactionEntity? = { null },
) {
    @Inject constructor(
        db: IvyRoomDatabase,
        accounts: AccountRepository,
        mapper: AccountMapper,
        dispatchers: DispatchersProvider,
    ) : this({ block -> db.withTransaction { block() } }, db.writeAccountDao::save, accounts, mapper, dispatchers,
        db.accountDao::findById, db.transactionDao::findById)

    private val writes = Mutex()

    suspend fun save(account: Account, adjustBalance: suspend () -> Unit) = persist(account, false, null, adjustBalance)

    suspend fun create(account: Account, openingBalance: Double, initializeBalance: suspend () -> Unit) {
        require(openingBalance.isFinite()) { "Opening balance must be finite." }
        persist(account, false, openingBalance, initializeBalance)
    }

    suspend fun edit(account: Account, adjustBalance: suspend () -> Unit) = persist(account, true, null, adjustBalance)

    private suspend fun persist(account: Account, requireExisting: Boolean, openingBalance: Double?,
        adjustBalance: suspend () -> Unit) = withContext(dispatchers.io) {
        writes.withLock {
            var committed = account
            try {
                transaction {
                    if (requireExisting || openingBalance != null) {
                        val row = readAccount(account.id.value)
                        if (requireExisting && (row == null || row.isDeleted)) throw AccountWriteConflict.MissingAccount()
                        if (openingBalance != null && row != null) {
                            if (row.isDeleted) throw AccountWriteConflict.MissingAccount()
                            val existing = with(mapper) { row.toDomain() }.getOrNull()
                                ?: throw AccountWriteConflict.AlreadySaved()
                            val opening = readOpening(accountOpeningTransactionId(account.id.value))
                            if (account.copy(orderNum = existing.orderNum) != existing ||
                                !matchesOpening(opening, account.id.value, openingBalance)) {
                                throw AccountWriteConflict.AlreadySaved()
                            }
                            // An earlier creation committed. Never reset a balance changed by
                            // later activity or append a second opening transaction on retry.
                            committed = existing
                            return@transaction
                        }
                    }
                    withContext(AccountWriteReads(account)) {
                        writeAccount(with(mapper) { account.toEntity() })
                        // Reads must see the staged account without caching it. The existing
                        // balance/transaction mapper remains authoritative for ledger semantics.
                        adjustBalance()
                        if (openingBalance != null) {
                            check(matchesOpening(readOpening(accountOpeningTransactionId(account.id.value)), account.id.value, openingBalance)) {
                                "Could not verify account opening balance."
                            }
                        }
                    }
                }
            } finally {
                // This also runs when cancellation races the commit acknowledgement. Never
                // keep a fully memoized account list that may disagree with durable storage.
                accounts.invalidateReadCache()
            }
            accounts.publishCommitted(committed)
        }
    }

    private fun matchesOpening(row: TransactionEntity?, accountId: UUID, balance: Double): Boolean {
        if (balance == 0.0) return row == null
        if (row == null || row.accountId != accountId || row.isDeleted || row.dateTime == null ||
            !row.amount.isFinite() || row.amount <= 0) return false
        return row.amount == kotlin.math.abs(balance) &&
            row.type == if (balance > 0) TransactionType.INCOME else TransactionType.EXPENSE
    }
}
