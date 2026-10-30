package com.example.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.db.ExpenseDatabase
import com.example.data.firestore.FirestoreSyncRepository
import com.example.data.model.ExpenseEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class FirestoreSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private lateinit var syncRepository: FirestoreSyncRepository
    private lateinit var roomDatabase: ExpenseDatabase
    private lateinit var roomRepository: ExpenseRepository

    @Before
    override fun setUpFirebase() {
        super.setUpFirebase()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        syncRepository = FirestoreSyncRepository(context, firestore, auth)

        roomDatabase = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        roomRepository = ExpenseRepository(
            roomDatabase.expenseDao(),
            roomDatabase.budgetDao(),
            roomDatabase.syncLogDao(),
            roomDatabase.recurringExpenseDao()
        )
    }

    @After
    override fun tearDownFirebase() {
        super.tearDownFirebase()
        if (::roomDatabase.isInitialized) {
            roomDatabase.close()
        }
    }

    @Test
    fun unauthenticated_cannot_upload_or_fetch_expenses() = runBlocking {
        auth.signOut()
        assertFalse("User should not be signed in", syncRepository.isUserSignedIn())

        val testExpense = ExpenseEntity(
            id = 1,
            title = "Coffee",
            amount = 150.0,
            date = System.currentTimeMillis(),
            category = "Food & Dining",
            paymentMethod = "UPI"
        )

        val uploadResult = syncRepository.uploadExpense(testExpense)
        assertTrue("Upload must fail when unauthenticated", uploadResult.isFailure)

        val fetchResult = syncRepository.fetchCloudExpenses()
        assertTrue("Fetch must fail when unauthenticated", fetchResult.isFailure)
    }

    @Test
    fun authenticated_user_can_upload_and_fetch_expenses() = runBlocking {
        val email = "alice_${System.currentTimeMillis()}@example.com"
        val uid = signInTestUser(email)
        assertTrue(syncRepository.isUserSignedIn())
        assertEquals(uid, syncRepository.requireUserId())

        val testExpense = ExpenseEntity(
            id = 42,
            title = "Groceries",
            amount = 1250.0,
            date = System.currentTimeMillis(),
            category = "Food & Dining",
            paymentMethod = "UPI",
            notes = "Weekly veggies",
            tag = "Essential"
        )

        val uploadResult = syncRepository.uploadExpense(testExpense)
        assertTrue("Upload should succeed for authenticated user", uploadResult.isSuccess)
        val docId = uploadResult.getOrNull()
        assertNotNull(docId)

        val fetchResult = syncRepository.fetchCloudExpenses()
        assertTrue("Fetch should succeed for authenticated user", fetchResult.isSuccess)
        val cloudExpenses = fetchResult.getOrNull().orEmpty()
        assertTrue("Should contain uploaded expense", cloudExpenses.any { it.title == "Groceries" && it.amount == 1250.0 })
    }

    @Test
    fun cross_user_access_is_forbidden() = runBlocking {
        val aliceEmail = "alice_${System.currentTimeMillis()}@example.com"
        val aliceUid = signInTestUser(aliceEmail)

        val expense = ExpenseEntity(
            id = 99,
            title = "Secret Vault Item",
            amount = 5000.0,
            date = System.currentTimeMillis(),
            category = "Personal",
            paymentMethod = "Cash"
        )
        val uploadResult = syncRepository.uploadExpense(expense)
        assertTrue(uploadResult.isSuccess)

        // Now sign in as Bob
        val bobEmail = "bob_${System.currentTimeMillis()}@example.com"
        val bobUid = signInTestUser(bobEmail)
        assertTrue(bobUid != aliceUid)

        // Bob tries to directly read Alice's expense document
        try {
            firestore.collection("users").document(aliceUid).collection("expenses").document("exp_local_99").get().addOnCompleteListener { }
            // Querying another user's collection should fail
            val snapshot = firestore.collection("users").document(aliceUid).collection("expenses").get()
            snapshot.await()
            fail("Bob should not be able to read Alice's expenses")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertTrue(
                "Must fail with PERMISSION_DENIED or security exception: ${e.message}",
                firestoreException?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    e.message?.contains("PERMISSION_DENIED") == true ||
                    e is FirebaseFirestoreException
            )
        }
    }

    @Test
    fun syncWithRoom_synchronizes_local_records_to_cloud_and_back() = runBlocking {
        val userEmail = "sync_user_${System.currentTimeMillis()}@example.com"
        signInTestUser(userEmail)

        // Insert local records in Room DB
        val local1 = ExpenseEntity(
            title = "Book Store",
            amount = 450.0,
            date = System.currentTimeMillis(),
            category = "Education",
            paymentMethod = "UPI"
        )
        val localId = roomRepository.insertExpense(local1)
        assertTrue(localId > 0)

        // Perform synchronization
        val syncResult = syncRepository.syncWithRoom(roomRepository)
        assertTrue("Sync should succeed", syncResult.isSuccess)
        val summary = syncResult.getOrNull()
        assertNotNull(summary)
        assertTrue("Uploaded count should be at least 1", summary!!.uploadedCount >= 1)

        // Verify cloud contains the record
        val cloudExpenses = syncRepository.fetchCloudExpenses().getOrNull().orEmpty()
        assertTrue("Cloud must contain synced expense", cloudExpenses.any { it.title == "Book Store" })
    }
}
