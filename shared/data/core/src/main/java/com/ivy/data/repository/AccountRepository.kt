package com.ivy.data.repository

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.DataWriteEvent
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.write.WriteAccountDao
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.repository.mapper.AccountMapper
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val mapper: AccountMapper,
    private val accountDao: AccountDao,
    private val writeAccountDao: WriteAccountDao,
    private val dispatchersProvider: DispatchersProvider,
    memoFactory: RepositoryMemoFactory,
) {
    private val memo = memoFactory.createMemo(
        getDataWriteSaveEvent = DataWriteEvent::SaveAccounts,
        getDateWriteDeleteEvent = DataWriteEvent::DeleteAccounts
    )

    suspend fun findById(id: AccountId): Account? {
        val transactionReads = coroutineContext[AccountWriteReads.Key]
        if (transactionReads != null) {
            return transactionReads.cached(id) ?: readById(id)?.also(transactionReads::remember)
        }
        return memo.findById(
            id = id,
            findByIdOperation = { readById(id) }
        )
    }

    private suspend fun readById(id: AccountId): Account? = accountDao.findById(id.value)?.let {
        with(mapper) { it.toDomain() }.getOrNull()
    }

    suspend fun findAll(): List<Account> = if (coroutineContext[AccountWriteReads.Key] != null) {
        readAll()
    } else memo.findAll(
        findAllOperation = {
            readAll()
        },
        sortMemo = { sortedBy(Account::orderNum) }
    )

    private suspend fun readAll(): List<Account> = accountDao.findAll().mapNotNull {
        with(mapper) { it.toDomain() }.getOrNull()
    }

    suspend fun findMaxOrderNum(): Double = if (coroutineContext[AccountWriteReads.Key] == null && memo.findAllMemoized) {
        memo.items.maxOfOrNull { (_, acc) -> acc.orderNum } ?: 0.0
    } else {
        withContext(dispatchersProvider.io) {
            accountDao.findMaxOrderNum() ?: 0.0
        }
    }

    suspend fun save(value: Account): Unit = memo.save(value) {
        writeAccountDao.save(
            with(mapper) { it.toEntity() }
        )
    }

    internal fun invalidateReadCache() = memo.invalidate()

    // The coordinator already committed the row and related ledger write.
    internal suspend fun publishCommitted(value: Account) = memo.save(value) { }

    suspend fun saveMany(values: List<Account>): Unit = memo.saveMany(values) {
        writeAccountDao.saveMany(
            it.map { with(mapper) { it.toEntity() } }
        )
    }

    suspend fun deleteById(id: AccountId): Unit = memo.deleteById(id) {
        writeAccountDao.deleteById(id.value)
    }

    suspend fun deleteAll(): Unit = memo.deleteAll(
        deleteAllOperation = writeAccountDao::deleteAll
    )
}
