package com.ivy.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ivy.data.db.migration.Migration131to132_CategoryRules
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration131to132CategoryRulesTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        IvyRoomDatabase::class.java,
        listOf(IvyRoomDatabase.DeleteSEMigration()),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate131to132_createsLocalCategoryRulesWithoutTouchingTransactions() {
        helper.createDatabase(TestDb, 131).apply {
            query("SELECT COUNT(*) FROM transactions").use {
                it.moveToFirst()
                it.getInt(0) shouldBe 0
            }
            close()
        }

        val newDb = helper.runMigrationsAndValidate(
            TestDb,
            132,
            true,
            Migration131to132_CategoryRules(),
        )

        newDb.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'category_rules'"
        ).use {
            it.moveToFirst() shouldBe true
        }
        newDb.query("SELECT COUNT(*) FROM transactions").use {
            it.moveToFirst()
            it.getInt(0) shouldBe 0
        }
        newDb.close()
    }

    companion object {
        private const val TestDb = "migration-category-rules-test"
    }
}
