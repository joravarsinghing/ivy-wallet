package com.ivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import com.ivy.data.db.entity.SettingsEntity
import com.ivy.data.repository.requireSafeWalletCurrencyChange
import java.util.UUID

@Dao
interface WriteSettingsDao {
    @Transaction
    suspend fun save(value: SettingsEntity) {
        validateCurrencyChange(value.currency)
        saveInitialCurrency(value)
    }

    @Transaction
    suspend fun saveMany(value: List<SettingsEntity>) {
        require(value.map { it.currency.uppercase(java.util.Locale.ROOT) }.distinct().size <= 1) {
            "The backup has conflicting wallet currencies. Nothing was imported."
        }
        value.forEach { validateCurrencyChange(it.currency) }
        value.forEach { saveInitialCurrency(it) }
    }

    /** Only the explicit first-run currency confirmation may establish imported implicit units. */
    @Upsert
    suspend fun saveInitialCurrency(value: SettingsEntity)

    @Query("SELECT currency FROM settings LIMIT 1")
    suspend fun currentCurrencyForWrite(): String?

    // Archived/deleted records remain financially meaningful; do not silently rebase them.
    @Query("""SELECT EXISTS(SELECT 1 FROM goals) OR EXISTS(SELECT 1 FROM budgets)
        OR EXISTS(SELECT 1 FROM loans WHERE accountId IS NULL)
        OR EXISTS(SELECT 1 FROM accounts WHERE currency IS NULL OR TRIM(currency) = '')""")
    suspend fun hasImplicitCurrencyData(): Boolean

    suspend fun validateCurrencyChange(requested: String) {
        requireSafeWalletCurrencyChange(currentCurrencyForWrite(), requested, hasImplicitCurrencyData())
    }

    @Query("DELETE FROM settings WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("DELETE FROM settings")
    suspend fun deleteAll()
}
