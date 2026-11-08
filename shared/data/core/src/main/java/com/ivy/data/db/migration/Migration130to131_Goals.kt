package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration130to131_Goals : Migration(130, 131) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goals` (
                `name` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `targetAmount` REAL NOT NULL,
                `deadline` INTEGER,
                `isActive` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `id` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_isActive` ON `goals` (`isActive`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_deadline` ON `goals` (`deadline`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goal_allocations` (
                `goalId` TEXT NOT NULL,
                `accountId` TEXT NOT NULL,
                `reservedAmount` REAL NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`goalId`, `accountId`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_goal_allocations_accountId` " +
                "ON `goal_allocations` (`accountId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_goal_allocations_goalId` " +
                "ON `goal_allocations` (`goalId`)"
        )
    }
}
