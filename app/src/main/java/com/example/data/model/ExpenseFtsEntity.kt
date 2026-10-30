package com.example.data.model

import androidx.room.Entity
import androidx.room.Fts4

/**
 * Room Full-Text Search (FTS4) virtual entity backing the [expenses] table.
 * Allows high-performance search across merchant title, category, notes, tag, bank name,
 * and credit card name using SQLite FTS MATCH queries.
 */
@Entity(tableName = "expenses_fts")
@Fts4(contentEntity = ExpenseEntity::class)
data class ExpenseFtsEntity(
    val title: String,
    val category: String,
    val notes: String,
    val tag: String,
    val bankName: String,
    val creditCardName: String
)
