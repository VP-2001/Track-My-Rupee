package com.example.ai

data class ExtractedTransaction(
    val title: String,
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val date: Long,
    val notes: String = "",
    val tag: String = "Personal",
    val referenceId: String = "",
    val bankName: String = "",
    val creditCardName: String = ""
)

data class StatementParseResult(
    val bankName: String,
    val transactions: List<ExtractedTransaction> = emptyList(),
    val totalDebits: Double = 0.0,
    val totalCredits: Double = 0.0,
    val accountOrCardNumber: String = "",
    val statementPeriod: String = ""
)
