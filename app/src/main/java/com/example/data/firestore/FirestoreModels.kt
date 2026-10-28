package com.example.data.firestore

import com.example.data.model.ExpenseEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreExpense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val localId: Long = 0L,
    val title: String = "",
    val amount: Double = 0.0,
    val date: Long = 0L,
    val category: String = "Miscellaneous",
    val paymentMethod: String = "UPI",
    val notes: String = "",
    val tag: String = "Personal",
    val bankName: String = "",
    val creditCardName: String = "",
    val receiptUri: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toExpenseEntity(): ExpenseEntity {
        return ExpenseEntity(
            id = 0, // Auto-generated locally on insertion
            title = title,
            amount = amount,
            date = date,
            category = category,
            paymentMethod = paymentMethod,
            notes = notes,
            tag = tag,
            receiptUri = receiptUri,
            createdAt = date,
            syncStatus = "SYNCED",
            bankName = bankName,
            creditCardName = creditCardName
        )
    }

    companion object {
        fun fromExpenseEntity(entity: ExpenseEntity, userId: String, docId: String = ""): FirestoreExpense {
            return FirestoreExpense(
                id = docId,
                userId = userId,
                localId = entity.id,
                title = entity.title,
                amount = entity.amount,
                date = entity.date,
                category = entity.category,
                paymentMethod = entity.paymentMethod,
                notes = entity.notes,
                tag = entity.tag,
                bankName = entity.bankName,
                creditCardName = entity.creditCardName,
                receiptUri = entity.receiptUri
            )
        }
    }
}

@IgnoreExtraProperties
data class FirestoreUserProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    @ServerTimestamp val lastSyncedAt: Timestamp? = null
)

data class SyncSummary(
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
    val totalCloudCount: Int = 0,
    val message: String = ""
)
