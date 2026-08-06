package com.bp.uunwlm

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bp.uunwlm.ui.screens.SplashScreen
import com.bp.uunwlm.ui.theme.MyApplicationTheme
import com.bp.uunwlm.ui.viewmodel.BPWalletViewModel

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: BPWalletViewModel = viewModel()
                viewModel.setScreen(com.bp.uunwlm.ui.viewmodel.ScreenType.SPLASH)
                Box(modifier = Modifier.fillMaxSize()) {
                    SplashScreen(
                        viewModel = viewModel,
                        onNavigate = {
                            val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
