package com.bp.wallet

import android.app.Application
import android.util.Log
import com.bp.wallet.data.BPWalletRepository

class BPWalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            BPWalletRepository.initContext(this)
            Log.d("BPWalletApplication", "BP Wallet Application Initialized successfully")
        } catch (e: Throwable) {
            Log.e("BPWalletApplication", "Error during app initialization", e)
        }
    }
}
