package com.ivy.data.backup

import androidx.room.withTransaction
import com.ivy.data.db.IvyRoomDatabase
import javax.inject.Inject

interface BackupTransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomBackupTransactionRunner @Inject constructor(
    private val database: IvyRoomDatabase,
) : BackupTransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T =
        database.withTransaction(block)
}
