package com.ivy.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ivy.data.db.entity.WealthRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WealthDao {
    @Query("SELECT * FROM wealth_records ORDER BY effectiveDate, id")
    suspend fun findAll(): List<WealthRecordEntity>
    @Query("SELECT * FROM wealth_records ORDER BY effectiveDate, id")
    fun observe(): Flow<List<WealthRecordEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(records: List<WealthRecordEntity>)
}
