package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration131to132_CategoryRules : Migration(131, 132) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `category_rules` (
                `id` TEXT NOT NULL,
                `categoryId` TEXT NOT NULL,
                `pattern` TEXT NOT NULL,
                `field` TEXT NOT NULL,
                `matchType` TEXT NOT NULL,
                `priority` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_category_rules_categoryId` " +
                "ON `category_rules` (`categoryId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_category_rules_isActive` " +
                "ON `category_rules` (`isActive`)"
        )
    }
}
