package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.RecurringExpenseEntity
import com.example.data.model.SyncLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpensesFlow(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    suspend fun getAllExpensesList(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getExpensesByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getExpensesByDateRangeList(startDate: Long, endDate: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR tag LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchExpensesFlow(query: String): Flow<List<ExpenseEntity>>

    // Room Full-Text Search (FTS4) Queries using MATCH operator
    @Query("""
        SELECT expenses.* FROM expenses
        JOIN expenses_fts ON expenses.id = expenses_fts.rowid
        WHERE expenses_fts MATCH :ftsQuery
        ORDER BY expenses.date DESC, expenses.id DESC
    """)
    fun searchExpensesFtsFlow(ftsQuery: String): Flow<List<ExpenseEntity>>

    @Query("""
        SELECT expenses.* FROM expenses
        JOIN expenses_fts ON expenses.id = expenses_fts.rowid
        WHERE expenses_fts MATCH :ftsQuery
        ORDER BY expenses.date DESC, expenses.id DESC
    """)
    suspend fun searchExpensesFtsList(ftsQuery: String): List<ExpenseEntity>

    @Query("""
        SELECT expenses.* FROM expenses
        JOIN expenses_fts ON expenses.id = expenses_fts.rowid
        WHERE expenses_fts MATCH :ftsQuery
          AND (:category IS NULL OR :category = 'All' OR expenses.category = :category)
          AND (:startDate IS NULL OR expenses.date >= :startDate)
          AND (:endDate IS NULL OR expenses.date <= :endDate)
        ORDER BY expenses.date DESC, expenses.id DESC
    """)
    fun searchExpensesFtsWithFiltersFlow(
        ftsQuery: String,
        category: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<ExpenseEntity>>

    @Query("""
        SELECT expenses.* FROM expenses
        JOIN expenses_fts ON expenses.id = expenses_fts.rowid
        WHERE expenses_fts MATCH :ftsQuery
          AND (:category IS NULL OR :category = 'All' OR expenses.category = :category)
          AND (:startDate IS NULL OR expenses.date >= :startDate)
          AND (:endDate IS NULL OR expenses.date <= :endDate)
        ORDER BY expenses.date DESC, expenses.id DESC
    """)
    suspend fun searchExpensesFtsWithFiltersList(
        ftsQuery: String,
        category: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): List<ExpenseEntity>

    @Query("""
        SELECT * FROM expenses
        WHERE (:category IS NULL OR :category = 'All' OR category = :category)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY date DESC, id DESC
    """)
    fun getExpensesWithFiltersFlow(
        category: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<ExpenseEntity>>

    @Query("""
        SELECT * FROM expenses
        WHERE (:category IS NULL OR :category = 'All' OR category = :category)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY date DESC, id DESC
    """)
    suspend fun getExpensesWithFiltersList(
        category: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>): List<Long>

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id IN (:ids)")
    suspend fun deleteExpensesByIds(ids: List<Long>)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgetsFlow(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsList(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 30")
    fun getRecentSyncLogsFlow(): Flow<List<SyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SyncLogEntity): Long

    @Query("DELETE FROM sync_logs")
    suspend fun clearLogs()
}

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY nextDueDate ASC, id DESC")
    fun getAllRecurringExpensesFlow(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses ORDER BY nextDueDate ASC, id DESC")
    suspend fun getAllRecurringExpensesList(): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveRecurringExpensesFlow(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 AND nextDueDate <= :currentTime")
    suspend fun getDueRecurringExpenses(currentTime: Long): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getRecurringExpenseById(id: Long): RecurringExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(item: RecurringExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpenses(items: List<RecurringExpenseEntity>): List<Long>

    @Update
    suspend fun updateRecurringExpense(item: RecurringExpenseEntity)

    @Delete
    suspend fun deleteRecurringExpense(item: RecurringExpenseEntity)

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteRecurringExpenseById(id: Long)

    @Query("DELETE FROM recurring_expenses")
    suspend fun deleteAllRecurringExpenses()
}

