package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseFtsEntity
import com.example.data.model.RecurringExpenseEntity
import com.example.data.model.SyncLogEntity

@Database(
    entities = [
        ExpenseEntity::class,
        ExpenseFtsEntity::class,
        BudgetEntity::class,
        SyncLogEntity::class,
        RecurringExpenseEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN bankName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expenses ADD COLUMN creditCardName TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recurring_expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL,
                        paymentMethod TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        dayOfMonth INTEGER NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER,
                        lastExecutedDate INTEGER,
                        nextDueDate INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        bankName TEXT NOT NULL,
                        creditCardName TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        tag TEXT NOT NULL,
                        autoCreateExpense INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS expenses_fts USING fts4(
                        content="expenses",
                        title,
                        category,
                        notes,
                        tag,
                        bankName,
                        creditCardName
                    )
                """.trimIndent())
                try {
                    db.execSQL("INSERT INTO expenses_fts(expenses_fts) VALUES('rebuild')")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            try {
                                db.execSQL("INSERT INTO expenses_fts(expenses_fts) VALUES('rebuild')")
                            } catch (_: Exception) {}
                        }
                    })
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
