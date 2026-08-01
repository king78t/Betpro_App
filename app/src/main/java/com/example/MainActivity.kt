package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BPWalletViewModel
import com.example.ui.viewmodel.ScreenType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Safe fallback when Firebase is not configured
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BPWalletApp()
            }
        }
    }
}

@Composable
fun BPWalletApp(
    viewModel: BPWalletViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val snackMessage by viewModel.snackMessage.collectAsState()
    val broadcast by viewModel.recentBroadcast.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMessage) {
        snackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackMessage()
        }
    }

    LaunchedEffect(broadcast) {
        broadcast?.let { (title, msg) ->
            snackbarHostState.showSnackbar("📢 $title: $msg")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ScreenType.LOGIN -> LoginScreen(viewModel = viewModel)
                ScreenType.REGISTER -> RegisterScreen(viewModel = viewModel)
                ScreenType.USER_HOME -> UserHomeScreen(viewModel = viewModel)
                ScreenType.USER_DEPOSIT -> UserDepositScreen(viewModel = viewModel)
                ScreenType.USER_WITHDRAW -> UserWithdrawScreen(viewModel = viewModel)
                ScreenType.USER_HISTORY -> UserHistoryScreen(viewModel = viewModel)
                ScreenType.USER_PROFILE -> UserProfileScreen(viewModel = viewModel)
                ScreenType.ADMIN_DASHBOARD -> AdminDashboardScreen(viewModel = viewModel)
                ScreenType.ADMIN_USERS_CRM -> AdminUsersCrmScreen(viewModel = viewModel)
                ScreenType.ADMIN_MASTER_AGENTS -> AdminMasterAgentsScreen(viewModel = viewModel)
                ScreenType.ADMIN_TRANSACTIONS -> AdminTransactionsScreen(viewModel = viewModel)
            }
        }
    }
}
