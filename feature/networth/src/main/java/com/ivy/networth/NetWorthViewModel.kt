package com.ivy.networth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.SharedPrefs
import com.ivy.data.model.wealth.*
import com.ivy.data.repository.WealthRepository
import com.ivy.domain.usecase.networth.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NetWorthViewModel @Inject constructor(
    private val reader: WealthWorkspaceUseCase,
    private val repository: WealthRepository,
    private val prefs: SharedPrefs,
) : ViewModel() {
    var workspace by mutableStateOf<WealthWorkspace?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var date by mutableStateOf(LocalDate.now())
        private set
    var currency by mutableStateOf<String?>(null)
        private set
    var months by mutableStateOf(6)
        private set
    val hidden get() = prefs.getBoolean(SharedPrefs.HIDE_CURRENT_BALANCE, false)
    private var loadingJob: Job? = null
    fun load() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            loading = true; error = null
            try {
                val result = reader.load(date, currency, months)
                workspace = result
                recordSnapshot(result)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { error = "Could not load net worth. Your financial data has not been changed." }
            finally { loading = false }
        }
    }
    private suspend fun recordSnapshot(workspace: WealthWorkspace) {
        val result = workspace.current
        if (result.lines.isEmpty()) return
        val day = result.asOf.toString()
        val itemIds = result.lines.map { it.item.id }.toSet()
        val policies = workspace.data.policies.filter { it.itemId in itemIds && it.effectiveDate <= day }.groupBy { it.itemId }
            .values.map { values -> values.maxWith(compareBy<WealthPolicy> { it.effectiveDate }.thenBy { Instant.parse(it.recordedAt) }.thenBy { it.id }) }
        val valuations = result.lines.mapNotNull { line -> workspace.data.valuations.filter { it.itemId == line.item.id && it.effectiveDate == line.valuationDate }
            .maxWithOrNull(compareBy<WealthValuation> { Instant.parse(it.recordedAt) }.thenBy { it.id }) }
        val rateIds = result.lines.flatMap { it.rateId?.split('+').orEmpty() }.toMutableSet()
        result.lines.forEach { line ->
            val parent = workspace.data.items.firstOrNull { it.id == line.item.parentId }
            if (parent != null) WealthCalculator.rate(workspace.data.rates, line.item.currency, parent.currency, day)?.second?.id?.split('+')?.let(rateIds::addAll)
        }
        val input = WealthSnapshotInputs(asOf = day, currency = result.currency, accountingZone = java.time.ZoneId.systemDefault().id,
            items = result.lines.map { it.item }.sortedBy { it.id }, policies = policies.sortedBy { it.id }, valuations = valuations.sortedBy { it.id },
            rates = workspace.data.rates.filter { it.id in rateIds }.sortedBy { it.id },
            positions = result.lines.sortedBy { it.item.id }.map { WealthSnapshotPosition(it.item.id, it.nativeAmount?.toPlainString(), it.reportingAmount?.toPlainString(),
                it.valuationDate, it.exchangeRate?.toPlainString(), it.rateId?.split('+').orEmpty(), it.included, it.issue) }, issues = result.issues.sorted())
        val inputs = Json.encodeToString(input)
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(inputs.toByteArray()).joinToString("") { "%02x".format(it) }
        val previous = workspace.data.snapshots.filter { it.asOf == date.toString() && it.currency == result.currency }.maxByOrNull { Instant.parse(it.recordedAt) }
        if (previous?.inputFingerprint == fingerprint) return
        repository.merge(WealthBackup(snapshots = listOf(WealthSnapshotRecord(UUID.randomUUID().toString(), date.toString(), result.currency,
            result.assets.toPlainString(), result.liabilities.toPlainString(), result.complete, fingerprint, inputs, Instant.now().toString(), previous?.id))))
    }
    fun range(months: Int) { this.months = months; load() }
    fun setDate(value: String) {
        val parsed = runCatching { LocalDate.parse(value) }.getOrNull()
        if (parsed == null || parsed > LocalDate.now()) { error = "Use an existing date in YYYY-MM-DD format."; return }
        date = parsed; load()
    }
    fun chooseCurrency(value: String) {
        if (!value.matches(Regex("[A-Z0-9]{2,12}"))) { error = "Enter a valid currency code."; return }
        currency = value; load()
    }
    fun save(item: WealthItem, policy: WealthPolicy, valuation: WealthValuation?, completed: () -> Unit) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            try { repository.save(item, policy, valuation); completed(); load() }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Could not save this item." }
            finally { saving = false }
        }
    }
    fun policy(item: WealthItem, include: Boolean, archive: Boolean = false) = viewModelScope.launch {
        try {
            val prior = workspace?.data?.policies?.filter { it.itemId == item.id && it.effectiveDate <= date.toString() }?.maxWithOrNull(compareBy<WealthPolicy> { it.effectiveDate }.thenBy { Instant.parse(it.recordedAt) })
            repository.merge(WealthBackup(policies = listOf(WealthPolicy(UUID.randomUUID().toString(), item.id, date.toString(), include, prior?.ownership ?: "100", archive, Instant.now().toString()))))
            load()
        } catch (_: Exception) { error = "Could not update inclusion." }
    }
    fun addRate(from: String, to: String, amount: String, effectiveDate: String, done: () -> Unit) = viewModelScope.launch {
        try {
            require(from != to && from.matches(Regex("[A-Z0-9]{2,12}")) && to.matches(Regex("[A-Z0-9]{2,12}"))) { "Use two different currency codes." }
            require(LocalDate.parse(effectiveDate) <= LocalDate.now()) { "Exchange rate date cannot be in the future." }
            require(BigDecimal(amount) > BigDecimal.ZERO) { "Exchange rate must be positive." }
            repository.merge(WealthBackup(rates = listOf(WealthFx(UUID.randomUUID().toString(), from, to, amount, effectiveDate, recordedAt = Instant.now().toString()))))
            done(); load()
        } catch (e: Exception) { error = e.message ?: "Enter a valid exchange rate." }
    }
    fun confirmCoverage(assets: Boolean, debts: Boolean) = viewModelScope.launch {
        try {
            repository.merge(WealthBackup(reviews = listOf(WealthReview(UUID.randomUUID().toString(), date.toString(), assets, debts, Instant.now().toString()))))
            load()
        } catch (_: Exception) { error = "Could not save the setup review." }
    }
    fun useWalletRates() = viewModelScope.launch {
        try { reader.captureWalletRates(); load() }
        catch (_: Exception) { error = "Could not read the existing wallet exchange rates." }
    }
    fun csv(history: Boolean): String {
        val w = workspace ?: return ""
        fun quote(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
        val rows = if (history) listOf(listOf("Date", "Item ID", "Name", "Native currency", "Value", "Quantity", "Unit price", "Unit", "Contribution", "Withdrawal", "Recorded at", "Corrects valuation")) + w.data.valuations.sortedBy { it.effectiveDate }.map { value ->
            val item = w.data.items.first { it.id == value.itemId }
            listOf(value.effectiveDate, value.itemId, item.name, item.currency, value.amount, value.quantity.orEmpty(), value.unitPrice.orEmpty(), item.unit, value.contribution.orEmpty(), value.withdrawal.orEmpty(), value.recordedAt, value.supersedesId.orEmpty())
        } else listOf(listOf("Name", "Category", "Native currency", "Signed native value", "Reporting currency", "Signed reporting value", "Valued on", "Included", "Issue")) + w.current.lines.map {
            listOf(it.item.name, it.item.kind.name, it.item.currency, it.nativeAmount?.toPlainString().orEmpty(), w.current.currency, it.reportingAmount?.toPlainString().orEmpty(), it.valuationDate.orEmpty(), it.included.toString(), it.issue.orEmpty())
        }
        return rows.joinToString("\r\n") { it.joinToString(",", transform = ::quote) }
    }
}
