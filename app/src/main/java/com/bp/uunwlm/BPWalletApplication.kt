package com.bp.uunwlm

import android.app.Application
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
            FirebaseApp.initializeApp(this)
            // Optional Firestore persistence settings
            try {
                val db = FirebaseFirestore.getInstance()
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build())
                    .build()
                db.firestoreSettings = settings
            } catch (e: Exception) {
                android.util.Log.w("BPWalletApp", "Firestore settings init note: ${e.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("BPWalletApp", "Firebase init error: ${e.message}")
        }

        // Initialize repository context
        BPWalletRepository.initContext(this)

        // Schedule periodic sync
        SupabaseSyncWorker.schedulePeriodicSync(this)
    }
}
