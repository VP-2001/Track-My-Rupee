package com.example

import android.app.Application
import android.util.Log
import com.example.notification.BudgetNotificationService
import com.google.firebase.FirebaseApp

class TrackMyRupeeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Guarantee Firebase initialization before any component queries FirebaseAuth or Firestore
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.e("TrackMyRupeeApp", "Failed to initialize FirebaseApp in Application.onCreate: ${e.message}", e)
        }

        try {
            // Register notification channels early
            BudgetNotificationService.initNotificationChannel(this)
        } catch (e: Exception) {
            Log.e("TrackMyRupeeApp", "Failed to initialize notification channels: ${e.message}", e)
        }
    }
}
