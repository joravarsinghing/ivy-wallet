package com.ivy.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ivy.data.db.migration.Migration130to131_Goals
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration130to131GoalsTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        IvyRoomDatabase::class.java,
        listOf(IvyRoomDatabase.DeleteSEMigration()),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate130to131_createsGoalTablesWithoutTouchingExistingTables() {
        helper.createDatabase(TestDb, 130).apply {
            query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'accounts'").use {
                it.moveToFirst() shouldBe true
            }
            close()
        }

        val newDb = helper.runMigrationsAndValidate(
            TestDb,
            131,
            true,
            Migration130to131_Goals()
        )

        newDb.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'goals'").use {
            it.moveToFirst() shouldBe true
        }
        newDb.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'goal_allocations'"
        ).use {
            it.moveToFirst() shouldBe true
        }
        newDb.query("SELECT COUNT(*) FROM accounts").use {
            it.moveToFirst() shouldBe true
            it.getInt(0) shouldBe 0
        }
        newDb.close()
    }

    companion object {
        private const val TestDb = "migration-goals-test"
    }
}
