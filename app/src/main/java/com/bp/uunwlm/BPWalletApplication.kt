package com.bp.uunwlm

import android.app.Application
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.bp.uunwlm.data.BPWalletRepository
import com.bp.uunwlm.worker.SupabaseSyncWorker

class BPWalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            val availability = GoogleApiAvailability.getInstance()
            val code = availability.isGooglePlayServicesAvailable(this)
            android.util.Log.i("BPWalletApp", "Google Play Services check result: $code")
            
            // Initialize Firebase regardless of GMS code, as it might still work or provide local fallback
            try {
                FirebaseApp.initializeApp(this)
                android.util.Log.d("BPWalletApp", "FirebaseApp.initializeApp() called")
            } catch (e: Exception) {
                android.util.Log.e("BPWalletApp", "FirebaseApp init error: ${e.message}")
            }
            
            if (code == ConnectionResult.SUCCESS) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                            .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build())
                        .build()
                    db.firestoreSettings = settings
                    android.util.Log.d("BPWalletApp", "Firestore settings initialized with persistence")
                } catch (se: SecurityException) {
                    android.util.Log.e("BPWalletApp", "SecurityException during Firestore init: ${se.message}")
                } catch (e: Exception) {
                    android.util.Log.e("BPWalletApp", "Exception during Firestore init: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("BPWalletApp", "Initialization error: ${e.message}", e)
        }

        // Initialize repository context
        BPWalletRepository.initContext(this)

        // Schedule periodic sync
        SupabaseSyncWorker.schedulePeriodicSync(this)
    }
}
