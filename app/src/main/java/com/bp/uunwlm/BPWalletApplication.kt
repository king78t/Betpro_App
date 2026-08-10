package com.bp.uunwlm

import android.app.Application
import com.bp.uunwlm.data.BPWalletRepository
import com.bp.uunwlm.worker.SupabaseSyncWorker

class BPWalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize repository context
        BPWalletRepository.initContext(this)

        // Schedule periodic sync with Supabase
        SupabaseSyncWorker.schedulePeriodicSync(this)
    }
}
