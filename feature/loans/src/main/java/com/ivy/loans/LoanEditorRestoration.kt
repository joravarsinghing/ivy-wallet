package com.ivy.loans

import com.ivy.design.system.ProcessDraftState
import com.ivy.design.system.draftUuid
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Loan
import com.ivy.legacy.datamodel.LoanRecord
import com.ivy.wallet.ui.theme.modal.LoanModalData
import com.ivy.wallet.ui.theme.modal.LoanRecordModalData
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime

/** Transient descriptors only. Fresh records supply all financial metadata on recovery. */
internal object LoanEditorRestoration {
    private fun digest(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    private fun Loan.signature() = digest(copy(isSynced = false, orderNum = 0.0).toString())
    private fun LoanRecord.signature() = digest(copy(isSynced = false).toString())
    data class Recovered(val loan: LoanModalData?, val record: LoanRecordModalData?, val date: Instant, val tab: String?)

    fun save(loan: LoanModalData?, originalLoan: Loan?, record: LoanRecordModalData?, originalRecord: LoanRecord?,
        date: Instant, tab: String? = null): List<String>? = ProcessDraftState.encode(buildMap {
        put("date", date.toString()); put("tab", tab)
        loan?.let {
            put("loanSession", it.id.toString()); put("loanRecord", it.loan?.id?.toString()); put("loanBaseline", originalLoan?.signature())
            put("loanDate", it.loan?.dateTime?.toString()); put("loanCurrency", it.baseCurrency)
            put("loanAccount", it.selectedAccount?.id?.toString()); put("loanAccountCurrency", it.selectedAccount?.currency ?: it.baseCurrency)
            put("loanFocus", it.autoFocusKeyboard.toString()); put("loanAmount", it.autoOpenAmountModal.toString())
            put("loanTransaction", it.createLoanTransaction.toString())
        }
        record?.let {
            put("recordSession", it.id.toString()); put("recordRecord", it.loanRecord?.id?.toString()); put("recordBaseline", originalRecord?.signature())
            put("recordDate", it.loanRecord?.dateTime?.toString()); put("recordCurrency", it.baseCurrency)
            put("recordLoanCurrency", it.loanAccountCurrencyCode); put("recordAccount", it.selectedAccount?.id?.toString())
            put("recordAccountCurrency", it.selectedAccount?.currency ?: it.baseCurrency)
            put("recordTransaction", it.createLoanRecordTransaction.toString()); put("recordInterest", it.isLoanInterest.toString())
        }
    })

    fun restore(payload: List<String>, currency: String, loans: List<Loan>, records: List<LoanRecord>, accounts: List<Account>): Recovered? = runCatching {
        val f = requireNotNull(ProcessDraftState.decode(payload))
        fun selected(key: String): Account? {
            val id = f.draftUuid("${key}Account") ?: return null
            return requireNotNull(accounts.firstOrNull { it.id == id }).also {
                require((it.currency ?: currency) == f["${key}AccountCurrency"])
            }
        }
        val loan = f.draftUuid("loanSession")?.let { session ->
            require(f["loanCurrency"] == currency)
            val id = f.draftUuid("loanRecord")
            val original = id?.let { key -> requireNotNull(loans.firstOrNull { it.id == key }) }
            require(original == null || original.signature() == f["loanBaseline"])
            require(original != null || loans.none { it.id == session })
            LoanModalData(original?.copy(dateTime = f["loanDate"]?.let(LocalDateTime::parse)), currency, selected("loan"),
                f["loanFocus"] == "true", f["loanAmount"] == "true", f["loanTransaction"] == "true", session)
        }
        val record = f.draftUuid("recordSession")?.let { session ->
            require(f["recordCurrency"] == currency)
            require(f["recordLoanCurrency"] == null || f["recordLoanCurrency"] == currency)
            val id = f.draftUuid("recordRecord")
            val original = id?.let { key -> requireNotNull(records.firstOrNull { it.id == key }) }
            require(original == null || original.signature() == f["recordBaseline"])
            require(original != null || records.none { it.id == session })
            LoanRecordModalData(original?.copy(dateTime = requireNotNull(f["recordDate"]).let(Instant::parse)), currency,
                f["recordLoanCurrency"], selected("record"), f["recordTransaction"] == "true", f["recordInterest"] == "true", session)
        }
        Recovered(loan, record, Instant.parse(requireNotNull(f["date"])), f["tab"])
    }.getOrNull()
}
