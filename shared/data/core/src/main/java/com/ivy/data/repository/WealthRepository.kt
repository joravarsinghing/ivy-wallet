package com.ivy.data.repository

import androidx.room.withTransaction
import com.ivy.data.db.IvyRoomDatabase
import com.ivy.data.db.entity.WealthRecordEntity
import com.ivy.data.model.wealth.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class WealthRepository @Inject constructor(private val db: IvyRoomDatabase) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val changes get() = db.wealthDao.observe().map { decode(it) }
    suspend fun read(): WealthBackup = withContext(Dispatchers.IO) { decode(db.wealthDao.findAll()) }
    private fun decode(rows: List<WealthRecordEntity>): WealthBackup = WealthBackup(
        items = rows.filter { it.kind == "item" }.map { json.decodeFromString<WealthItem>(it.payload) },
        policies = rows.filter { it.kind == "policy" }.map { json.decodeFromString<WealthPolicy>(it.payload) },
        valuations = rows.filter { it.kind == "valuation" }.map { json.decodeFromString<WealthValuation>(it.payload) },
        rates = rows.filter { it.kind == "fx" }.map { json.decodeFromString<WealthFx>(it.payload) },
        snapshots = rows.filter { it.kind == "snapshot" }.map { json.decodeFromString<WealthSnapshotRecord>(it.payload) },
        reviews = rows.filter { it.kind == "review" }.map { json.decodeFromString<WealthReview>(it.payload) },
    )
    private fun records(data: WealthBackup): List<WealthRecordEntity> = buildList {
        data.items.forEach { add(WealthRecordEntity(it.id, "item", it.id, it.createdDate, json.encodeToString(it))) }
        data.policies.forEach { add(WealthRecordEntity(it.id, "policy", it.itemId, it.effectiveDate, json.encodeToString(it))) }
        data.valuations.forEach { add(WealthRecordEntity(it.id, "valuation", it.itemId, it.effectiveDate, json.encodeToString(it))) }
        data.rates.forEach { add(WealthRecordEntity(it.id, "fx", it.from + "/" + it.to, it.effectiveDate, json.encodeToString(it))) }
        data.snapshots.forEach { add(WealthRecordEntity(it.id, "snapshot", it.currency, it.asOf, json.encodeToString(it))) }
        data.reviews.forEach { add(WealthRecordEntity(it.id, "review", "setup", it.effectiveDate, json.encodeToString(it))) }
    }
    suspend fun merge(data: WealthBackup) = withContext(Dispatchers.IO) {
        validate(data)
        db.withTransaction {
            val existing = read()
            val merged = WealthBackup(
                items = (existing.items + data.items).associateBy { it.id }.values.toList(),
                policies = (existing.policies + data.policies).associateBy { it.id }.values.toList(),
                valuations = (existing.valuations + data.valuations).associateBy { it.id }.values.toList(),
                rates = (existing.rates + data.rates).associateBy { it.id }.values.toList(),
                snapshots = (existing.snapshots + data.snapshots).associateBy { it.id }.values.toList(),
                reviews = (existing.reviews + data.reviews).associateBy { it.id }.values.toList(),
            )
            validate(merged)
            validateReferences(merged)
            val oldRecords = records(existing).associateBy { it.id }
            records(data).forEach { row ->
                val old = oldRecords[row.id]
                val sameItemDefinition = old?.kind == "item" && row.kind == "item" &&
                    json.decodeFromString<WealthItem>(old.payload).hasSameFinancialDefinition(json.decodeFromString(row.payload))
                require(old == null || sameItemDefinition || old == row) { "Historical setup cannot be overwritten. Save a dated replacement or a valuation correction instead." }
            }
            db.wealthDao.save(records(data))
        }
    }
    suspend fun save(item: WealthItem, policy: WealthPolicy, valuation: WealthValuation? = null) = withContext(Dispatchers.IO) {
        db.withTransaction { merge(planWealthSave(read(), item, policy, valuation)) }
    }
    fun validateReferences(data: WealthBackup) {
        val items = data.items.associateBy { it.id }
        data.items.forEach { item ->
            if (item.parentId != null) {
                val parent = items[item.parentId]
                require(item.source == WealthSource.MANUAL && item.parentId != item.id && parent != null && parent.parentId == null && parent.source in listOf(WealthSource.MANUAL, WealthSource.HOLDINGS)) { "A holding must belong to one existing portfolio." }
            }
            item.securedById?.let { require(it != item.id && items[it] != null) { "The linked property or vehicle does not exist." } }
        }
        data.policies.forEach { require(items[it.itemId] != null) { "Inclusion decision has no asset or liability." } }
        data.valuations.forEach { value ->
            require(items[value.itemId] != null) { "Valuation has no asset or liability." }
            value.supersedesId?.let { id ->
                val previous = data.valuations.firstOrNull { it.id == id }
                require(previous != null && previous.itemId == value.itemId && previous.effectiveDate == value.effectiveDate && previous.id != value.id) { "A correction must refer to the same item and valuation date." }
            }
        }
    }
    fun validate(data: WealthBackup) {
        require(data.version == 1) { "Unsupported wealth backup version." }
        val allIds = records(data).map { it.id }
        require(allIds.all { it.isNotBlank() } && allIds.distinct().size == allIds.size) { "Duplicate wealth record IDs." }
        data.items.forEach { item ->
            require(item.name.isNotBlank() && item.currency.matches(Regex("[A-Z0-9]{2,12}"))) { "Enter a name and currency code." }
            LocalDate.parse(item.createdDate)
            require(item.reviewDays in 1..3650) { "Review interval must be between 1 and 3650 days." }
            if (item.source == WealthSource.ACCOUNT || item.source == WealthSource.LOAN) require(!item.sourceId.isNullOrBlank())
            item.apr?.let { require(BigDecimal(it) >= BigDecimal.ZERO) }
            item.minimumPayment?.let { require(BigDecimal(it) >= BigDecimal.ZERO) }
        }
        data.policies.forEach {
            LocalDate.parse(it.effectiveDate); Instant.parse(it.recordedAt)
            require(BigDecimal(it.ownership) >= BigDecimal.ZERO && BigDecimal(it.ownership) <= BigDecimal("100"))
        }
        data.valuations.forEach {
            LocalDate.parse(it.effectiveDate); Instant.parse(it.recordedAt)
            require(BigDecimal(it.amount) >= BigDecimal.ZERO) { "Value must not be negative." }
            listOfNotNull(it.quantity, it.unitPrice, it.contribution, it.withdrawal).forEach { n -> require(BigDecimal(n) >= BigDecimal.ZERO) }
            require((it.quantity == null) == (it.unitPrice == null)) { "Quantity and unit price are both required." }
        }
        data.rates.forEach {
            LocalDate.parse(it.effectiveDate); Instant.parse(it.recordedAt)
            require(it.from != it.to && it.from.matches(Regex("[A-Z0-9]{2,12}")) && it.to.matches(Regex("[A-Z0-9]{2,12}"))) { "Invalid exchange-rate currencies." }
            require(BigDecimal(it.rate) > BigDecimal.ZERO) { "Exchange rate must be positive." }
        }
        data.snapshots.forEach {
            LocalDate.parse(it.asOf); Instant.parse(it.recordedAt)
            require(BigDecimal(it.assets) >= BigDecimal.ZERO && BigDecimal(it.liabilities) >= BigDecimal.ZERO)
            val hash = java.security.MessageDigest.getInstance("SHA-256").digest(it.inputs.toByteArray()).joinToString("") { byte -> "%02x".format(byte) }
            require(hash.equals(it.inputFingerprint, ignoreCase = true)) { "Historical snapshot checksum is invalid." }
        }
        data.reviews.forEach { LocalDate.parse(it.effectiveDate); Instant.parse(it.recordedAt) }
        validateWealthSourceLinks(data)
    }
}
