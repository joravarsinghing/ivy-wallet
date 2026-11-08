package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration132to133_SavedAnalytics : Migration(132, 133) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `saved_analytics` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `definition` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `isSynced` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_saved_analytics_kind` " +
                "ON `saved_analytics` (`kind`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_saved_analytics_isDeleted` " +
                "ON `saved_analytics` (`isDeleted`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_saved_analytics_updatedAt` " +
                "ON `saved_analytics` (`updatedAt`)"
        )
    }
}
