package com.ivy.wallet.domain.deprecated.logic

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.db.dao.read.LoanDao
import com.ivy.data.db.dao.read.LoanRecordDao
import com.ivy.data.db.dao.write.WriteLoanRecordDao
import com.ivy.data.repository.AtomicWriteRepository
import com.ivy.legacy.datamodel.LoanRecord
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.legacy.domain.creation.acknowledgedWrite
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanRecordData
import java.util.UUID
import javax.inject.Inject

class LoanRecordCreator @Inject constructor(
    private val loanRecordWriter: WriteLoanRecordDao,
    private val records: LoanRecordDao,
    private val loans: LoanDao,
    private val atomic: AtomicWriteRepository,
    private val dispatchers: DispatchersProvider,
) {
    private suspend fun validate(item: LoanRecord) {
        require(item.amount.isFinite() && item.amount > 0)
        require(item.convertedAmount == null || (item.convertedAmount.isFinite() && item.convertedAmount > 0))
        val parent = loans.findById(item.loanId) ?: throw LoanWriteConflict()
        if (parent.isDeleted) throw LoanWriteConflict()
    }

    suspend fun create(
        loanId: UUID,
        data: CreateLoanRecordData,
        onResult: (Result<Unit>) -> Unit = {},
        prepare: suspend (CreateLoanRecordData) -> CreateLoanRecordData = { it },
        relatedWrite: suspend (LoanRecord) -> Unit = {},
        onRefreshUI: suspend (LoanRecord) -> Unit,
    ): UUID? = acknowledgedWrite(dispatchers, onResult, {
        atomic.run {
            var item = LoanRecord(loanId = loanId, note = data.note?.trim(), amount = data.amount,
                dateTime = data.dateTime, isSynced = false, interest = data.interest,
                accountId = data.account?.id, convertedAmount = data.convertedAmount,
                loanRecordType = data.loanRecordType, id = data.id)
            val existing = records.findById(item.id)
            if (existing != null) {
                // A replay does not obtain a new exchange quote or rewrite the committed conversion.
                validate(existing.toLegacyDomain())
                if (existing.isDeleted || existing.toLegacyDomain().copy(isSynced = false) != item.copy(convertedAmount = existing.convertedAmount)) throw LoanWriteConflict()
                existing.toLegacyDomain()
            } else {
                val prepared = prepare(data)
                require(prepared.copy(convertedAmount = data.convertedAmount) == data)
                item = item.copy(convertedAmount = prepared.convertedAmount)
                validate(item)
                loanRecordWriter.save(item.toEntity())
                relatedWrite(item)
                item
            }
        }
    }, onRefreshUI)?.id

    suspend fun edit(
        updatedItem: LoanRecord,
        onResult: (Result<Unit>) -> Unit = {},
        relatedWrite: suspend () -> Unit = {},
        onRefreshUI: suspend (LoanRecord) -> Unit,
    ) = acknowledgedWrite(dispatchers, onResult, {
        atomic.run {
            validate(updatedItem)
            val existing = records.findById(updatedItem.id) ?: throw LoanWriteConflict()
            if (existing.isDeleted || existing.loanId != updatedItem.loanId) throw LoanWriteConflict()
            relatedWrite()
            updatedItem.copy(isSynced = false).also { loanRecordWriter.save(it.toEntity()) }
        }
    }, onRefreshUI)

    suspend fun delete(
        item: LoanRecord,
        onResult: (Result<Unit>) -> Unit = {},
        relatedWrite: suspend () -> Unit = {},
        onRefreshUI: suspend () -> Unit,
    ) = acknowledgedWrite(dispatchers, onResult, {
        atomic.run {
            relatedWrite()
            loanRecordWriter.deleteById(item.id)
        }
    }, { onRefreshUI() })
}
