package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.data.model.ExpenseEntity
import com.example.data.repository.ExpenseRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val FALLBACK_DATABASE_ID = "ai-studio-android-trackmyr-367a8e33-93d8-4bdd-b42b-02cc6c261dff"

private fun getFirestoreInstance(context: Context): FirebaseFirestore {
    val appContext = context.applicationContext
    val app = if (FirebaseApp.getApps(appContext).isEmpty()) {
        FirebaseApp.initializeApp(appContext) ?: FirebaseApp.getInstance()
    } else {
        FirebaseApp.getInstance()
    }
    val dbId = try {
        val resId = appContext.resources.getIdentifier("firestore_database_id", "string", appContext.packageName)
        if (resId != 0) {
            val v = appContext.getString(resId)
            if (v.isNotBlank()) v else FALLBACK_DATABASE_ID
        } else {
            FALLBACK_DATABASE_ID
        }
    } catch (e: Exception) {
        FALLBACK_DATABASE_ID
    }
    return try {
        FirebaseFirestore.getInstance(app, dbId)
    } catch (e: Exception) {
        FirebaseFirestore.getInstance(app)
    }
}

class FirestoreSyncRepository(
    private val context: Context,
    val firestore: FirebaseFirestore,
    val auth: FirebaseAuth
) {
    // Primary production constructor passing the custom database ID from resources
    constructor(context: Context) : this(
        context.applicationContext,
        getFirestoreInstance(context),
        FirebaseAuth.getInstance()
    )

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    private fun userExpensesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("expenses")

    private fun userDocument(userId: String) =
        firestore.collection("users").document(userId)

    fun observeCloudExpenses(): Flow<List<FirestoreExpense>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/expenses"
        userExpensesCollection(uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreExpense::class.java)
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun fetchCloudExpenses(): Result<List<FirestoreExpense>> = withContext(Dispatchers.IO) {
        try {
            val uid = requireUserId()
            val path = "users/$uid/expenses"
            val snapshot = userExpensesCollection(uid).get().await()
            val list = snapshot.toObjects(FirestoreExpense::class.java)
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/${auth.currentUser?.uid}/expenses")
            Result.failure(e)
        }
    }

    suspend fun uploadExpense(expense: ExpenseEntity): Result<String> = withContext(Dispatchers.IO) {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        val collection = userExpensesCollection(uid)
        // Deterministic document ID to ensure idempotency across recurring sync runs
        val docId = if (expense.id > 0) "exp_local_${expense.id}" else collection.document().id
        val docRef = collection.document(docId)

        try {
            val firestoreModel = FirestoreExpense.fromExpenseEntity(expense, uid, docId)
            val safeTitle = expense.title.trim().ifBlank { "Expense" }
            val safeCategory = expense.category.trim().ifBlank { "Miscellaneous" }
            val safePaymentMethod = expense.paymentMethod.trim().ifBlank { "UPI" }

            val dataMap = hashMapOf<String, Any?>(
                "userId" to uid,
                "localId" to expense.id,
                "title" to safeTitle,
                "amount" to expense.amount,
                "date" to expense.date,
                "category" to safeCategory,
                "paymentMethod" to safePaymentMethod,
                "notes" to expense.notes,
                "tag" to expense.tag,
                "bankName" to expense.bankName,
                "creditCardName" to expense.creditCardName,
                "receiptUri" to expense.receiptUri,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(dataMap).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudExpense(docId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        val docRef = userExpensesCollection(uid).document(docId)
        try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncWithRoom(roomRepository: ExpenseRepository): Result<SyncSummary> = withContext(Dispatchers.IO) {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        try {
            // 1. Fetch current cloud expenses for the authenticated user
            val cloudSnapshot = userExpensesCollection(uid).get().await()
            val cloudList = cloudSnapshot.toObjects(FirestoreExpense::class.java)

            // Index cloud transactions for quick lookup by title, amount, and date
            val cloudLookup = cloudList.associateBy {
                "${it.title.trim().lowercase()}_${it.amount}_${it.date}"
            }

            // 2. Fetch local Room DB expenses
            val localList = roomRepository.getExpensesList()
            val localLookup = localList.associateBy {
                "${it.title.trim().lowercase()}_${it.amount}_${it.date}"
            }

            var uploadedCount = 0
            var downloadedCount = 0

            // 3. Upload local records that are not in Firestore
            for (local in localList) {
                val key = "${local.title.trim().lowercase()}_${local.amount}_${local.date}"
                if (!cloudLookup.containsKey(key)) {
                    val docId = if (local.id > 0) "exp_local_${local.id}" else userExpensesCollection(uid).document().id
                    val safeTitle = local.title.trim().ifBlank { "Expense" }
                    val safeCategory = local.category.trim().ifBlank { "Miscellaneous" }
                    val safePaymentMethod = local.paymentMethod.trim().ifBlank { "UPI" }

                    val dataMap = hashMapOf<String, Any?>(
                        "userId" to uid,
                        "localId" to local.id,
                        "title" to safeTitle,
                        "amount" to local.amount,
                        "date" to local.date,
                        "category" to safeCategory,
                        "paymentMethod" to safePaymentMethod,
                        "notes" to local.notes,
                        "tag" to local.tag,
                        "bankName" to local.bankName,
                        "creditCardName" to local.creditCardName,
                        "receiptUri" to local.receiptUri,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    userExpensesCollection(uid).document(docId).set(dataMap).await()
                    uploadedCount++
                }
            }

            // 4. Download cloud records that are not in Room DB
            val newToInsertLocally = mutableListOf<ExpenseEntity>()
            for (cloud in cloudList) {
                val key = "${cloud.title.trim().lowercase()}_${cloud.amount}_${cloud.date}"
                if (!localLookup.containsKey(key)) {
                    newToInsertLocally.add(cloud.toExpenseEntity())
                    downloadedCount++
                }
            }

            if (newToInsertLocally.isNotEmpty()) {
                roomRepository.insertExpenses(newToInsertLocally)
            }

            // 5. Update user profile document with lastSyncedAt
            val userEmail = auth.currentUser?.email?.takeIf { it.isNotBlank() } ?: "user_${uid.take(8)}@account.google.com"
            val userProfileMap = hashMapOf<String, Any?>(
                "userId" to uid,
                "email" to userEmail,
                "displayName" to (auth.currentUser?.displayName ?: "Google User"),
                "lastSyncedAt" to FieldValue.serverTimestamp()
            )
            userDocument(uid).set(userProfileMap).await()

            // 6. Record sync log
            val totalCloud = cloudList.size + uploadedCount
            val summaryMsg = "Synced: $uploadedCount uploaded, $downloadedCount downloaded. Total cloud: $totalCloud"
            roomRepository.addSyncLog(
                action = "CLOUD_FIRESTORE_SYNC",
                count = uploadedCount + downloadedCount,
                status = "SUCCESS",
                email = auth.currentUser?.email ?: "Google Account",
                details = summaryMsg
            )

            Result.success(
                SyncSummary(
                    uploadedCount = uploadedCount,
                    downloadedCount = downloadedCount,
                    totalCloudCount = totalCloud,
                    message = summaryMsg
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }
}
