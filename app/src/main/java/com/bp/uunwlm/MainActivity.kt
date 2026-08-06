package com.bp.uunwlm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bp.uunwlm.ui.screens.*
import com.bp.uunwlm.ui.theme.*
import com.bp.uunwlm.ui.viewmodel.BPWalletViewModel
import com.bp.uunwlm.ui.viewmodel.ScreenType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val context = LocalContext.current

    var showPermissionDialog by remember { mutableStateOf(false) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                showPermissionDialog = false
                if (!isGranted) {
                    viewModel.showSnack("Notification permission recommended for deposit/withdrawal alerts.")
                } else {
                    viewModel.showSnack("Notifications enabled! You will receive real-time alerts.")
                }
            }
        )

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                showPermissionDialog = true
            }
        }

        if (showPermissionDialog) {
            NotificationPermissionDialog(
                onEnableClick = {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onDismissClick = {
                    showPermissionDialog = false
                }
            )
        }
    }

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
                ScreenType.SPLASH -> Unit // Handled exclusively by SplashActivity
                ScreenType.LOGIN -> LoginScreen(viewModel = viewModel)
                ScreenType.REGISTER -> RegisterScreen(viewModel = viewModel)
                ScreenType.USER_HOME -> UserHomeScreen(viewModel = viewModel)
                ScreenType.USER_DEPOSIT -> UserDepositScreen(viewModel = viewModel)
                ScreenType.USER_WITHDRAW -> UserWithdrawScreen(viewModel = viewModel)
                ScreenType.USER_HISTORY -> UserHistoryScreen(viewModel = viewModel)
                ScreenType.USER_PROFILE -> UserProfileScreen(viewModel = viewModel)
                ScreenType.ADMIN_DASHBOARD -> AdminDashboardScreen(viewModel = viewModel)
                ScreenType.ADMIN_LIVE_CONTROL -> AdminLiveControlScreen(viewModel = viewModel)
                ScreenType.ADMIN_USERS_CRM -> AdminUsersCrmScreen(viewModel = viewModel)
                ScreenType.ADMIN_MASTER_AGENTS -> AdminMasterAgentsScreen(viewModel = viewModel)
                ScreenType.ADMIN_TRANSACTIONS -> AdminTransactionsScreen(viewModel = viewModel)
                ScreenType.ADMIN_SETTINGS -> AdminSettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NotificationPermissionDialog(
    onEnableClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissClick,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BPGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Notifications",
                    tint = BPGreenDark,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Stay Updated in Real-Time 🔔",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Enable notifications to receive instant status alerts for your deposit and withdrawal requests.",
                    fontSize = 13.sp,
                    color = Slate600,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Feature items
                NotificationFeatureItem(
                    icon = Icons.Default.CheckCircle,
                    text = "Instant Deposit Approval Alerts"
                )
                NotificationFeatureItem(
                    icon = Icons.Default.CheckCircle,
                    text = "Fast Withdrawal Status Updates"
                )
                NotificationFeatureItem(
                    icon = Icons.Default.CheckCircle,
                    text = "Important BetPro Security Announcements"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onEnableClick,
                colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Enable Notifications",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Maybe Later",
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )
            }
        }
    )
}

@Composable
private fun NotificationFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BPGreenPrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate800
        )
    }
}
