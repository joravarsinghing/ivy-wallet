package com.ivy.wallet.domain.deprecated.logic

import androidx.compose.ui.graphics.toArgb
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.db.dao.read.LoanDao
import com.ivy.data.db.dao.read.LoanRecordDao
import com.ivy.data.db.dao.write.WriteLoanDao
import com.ivy.data.db.dao.write.WriteLoanRecordDao
import com.ivy.data.repository.AtomicWriteRepository
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.legacy.domain.creation.acknowledgedWrite
import com.ivy.wallet.domain.deprecated.logic.model.CreateLoanData
import com.ivy.wallet.domain.pure.util.nextOrderNum
import java.util.UUID
import javax.inject.Inject

class LoanWriteConflict : IllegalStateException()

class LoanCreator @Inject constructor(
    private val dao: LoanDao,
    private val loanWriter: WriteLoanDao,
    private val records: LoanRecordDao,
    private val recordWriter: WriteLoanRecordDao,
    private val atomic: AtomicWriteRepository,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun create(
        data: CreateLoanData,
        onResult: (Result<Unit>) -> Unit = {},
        relatedWrite: suspend (Loan) -> Unit = {},
        onRefreshUI: suspend (Loan) -> Unit,
    ): UUID? = acknowledgedWrite(dispatchers, onResult, {
        atomic.run {
            require(data.name.isNotBlank() && data.amount.isFinite() && data.amount > 0)
            val existing = dao.findById(data.id)
            val item = Loan(name = data.name.trim(), amount = data.amount, type = data.type,
                color = data.color.toArgb(), icon = data.icon, note = data.note,
                orderNum = existing?.orderNum ?: dao.findMaxOrderNum().nextOrderNum(),
                isSynced = false, accountId = data.account?.id, dateTime = data.dateTime, id = data.id)
            if (existing != null) {
                if (existing.isDeleted || existing.toLegacyDomain().copy(isSynced = false) != item) throw LoanWriteConflict()
                // Already committed together; don't rewrite subsequent wallet activity.
                existing.toLegacyDomain()
            } else {
                loanWriter.save(item.toEntity())
                relatedWrite(item)
                item
            }
        }
    }, onRefreshUI)?.id

    suspend fun edit(
        updatedItem: Loan,
        onResult: (Result<Unit>) -> Unit = {},
        relatedWrite: suspend (Loan) -> Unit = {},
        onRefreshUI: suspend (Loan) -> Unit,
    ) = acknowledgedWrite(dispatchers, onResult, {
        atomic.run {
            require(updatedItem.name.isNotBlank() && updatedItem.amount.isFinite() && updatedItem.amount > 0)
            val existing = dao.findById(updatedItem.id) ?: throw LoanWriteConflict()
            if (existing.isDeleted) throw LoanWriteConflict()
            relatedWrite(existing.toLegacyDomain())
            updatedItem.copy(name = updatedItem.name.trim(), isSynced = false).also { loanWriter.save(it.toEntity()) }
        }
    }, onRefreshUI)

    suspend fun delete(
        item: Loan,
        onResult: (Result<Unit>) -> Unit = {},
        relatedWrite: suspend () -> Unit = {},
        onRefreshUI: suspend () -> Unit,
    ) = acknowledgedWrite(dispatchers, onResult, {
        atomic.run {
            relatedWrite()
            records.findAllByLoanId(item.id).forEach { recordWriter.deleteById(it.id) }
            loanWriter.deleteById(item.id)
        }
    }, { onRefreshUI() })
}
