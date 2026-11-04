package com.ivy.data.db.dao.fake

import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.dao.write.WriteSettingsDao
import com.ivy.data.db.entity.SettingsEntity
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

@VisibleForTesting
class FakeSettingsDao(var implicitCurrencyData: Boolean = false) : SettingsDao, WriteSettingsDao {
    private val items = mutableListOf<SettingsEntity>()

    override suspend fun findFirst(): SettingsEntity {
        return items.first()
    }

    override suspend fun findFirstOrNull(): SettingsEntity? {
        return items.firstOrNull()
    }

    override suspend fun findAll(): List<SettingsEntity> {
        return items
    }

    override suspend fun findById(id: UUID): SettingsEntity? {
        return items.firstOrNull { it.id == id }
    }

    override suspend fun save(value: SettingsEntity) {
        validateCurrencyChange(value.currency)
        saveInitialCurrency(value)
    }

    override suspend fun saveInitialCurrency(value: SettingsEntity) {
        items.removeAll { it.id == value.id }
        items.add(value)
    }

    override suspend fun currentCurrencyForWrite() = items.firstOrNull()?.currency
    override suspend fun hasImplicitCurrencyData() = implicitCurrencyData

    override suspend fun saveMany(value: List<SettingsEntity>) {
        require(value.map { it.currency.uppercase(java.util.Locale.ROOT) }.distinct().size <= 1)
        value.forEach { validateCurrencyChange(it.currency) }
        value.forEach { saveInitialCurrency(it) }
    }

    override suspend fun deleteById(id: UUID) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAll() {
        items.clear()
    }
}
