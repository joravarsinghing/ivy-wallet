package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration133to134_Wealth : Migration(133, 134) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `wealth_records` (`id` TEXT NOT NULL, `kind` TEXT NOT NULL, `ownerId` TEXT NOT NULL, `effectiveDate` TEXT NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_wealth_records_kind` ON `wealth_records` (`kind`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_wealth_records_ownerId_effectiveDate` ON `wealth_records` (`ownerId`, `effectiveDate`)")
    }
}
