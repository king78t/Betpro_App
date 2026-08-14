package com.bp.wallet

import io.github.jan.supabase.auth.auth
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
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
import androidx.compose.material.icons.filled.Lock
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
import androidx.lifecycle.lifecycleScope
import com.bp.wallet.data.BPWalletRepository
import com.bp.wallet.data.SupabaseCloudManager
import com.bp.wallet.ui.components.*
import com.bp.wallet.ui.screens.*
import com.bp.wallet.ui.theme.*
import com.bp.wallet.ui.components.BPLottieLoadingView
import com.bp.wallet.ui.components.BPLoader
import com.bp.wallet.ui.viewmodel.BPWalletViewModel
import com.bp.wallet.ui.viewmodel.ScreenType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val viewModel: BPWalletViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[BPWalletViewModel::class.java]
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.recordUserActivity()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: android.content.Intent?) {
        if (intent?.data != null) {
            lifecycleScope.launch {
                val verified = SupabaseCloudManager.handleDeepLink(intent)
                if (verified) {
                    val authId = SupabaseCloudManager.getCurrentAuthUserId()
                    var user = if (authId != null) SupabaseCloudManager.loadUserById(authId) else null
                    if (user == null) {
                        val authEmail = SupabaseCloudManager.client?.auth?.currentUserOrNull()?.email
                        if (!authEmail.isNullOrBlank()) {
                            user = SupabaseCloudManager.loadUserByEmail(authEmail)
                        }
                    }
                    if (user != null) {
                        val verifiedUser = user.copy(isVerified = true, betproIdStatus = "Active")
                        BPWalletRepository.setCurrentUser(verifiedUser)
                        viewModel.setScreen(if (verifiedUser.isAdminRole) ScreenType.ADMIN_DASHBOARD else ScreenType.USER_HOME)
                        viewModel.showSnack("Email verification complete! Welcome ${verifiedUser.fullName}")
                    } else {
                        viewModel.showSnack("Email verification complete! Please log into your account.")
                        viewModel.setScreen(ScreenType.LOGIN)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            BPWalletRepository.initContext(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error in initContext", e)
        }
        
        handleDeepLinkIntent(intent)
        
        try {
            // One-time Purge of Local Data for Migration/Clean Audit
            val auditPrefs = getSharedPreferences("audit_reset", android.content.Context.MODE_PRIVATE)
            val isResetDone = auditPrefs.getBoolean("v2_reset_done", false)
            if (!isResetDone) {
                // Clear standard session prefs
                getSharedPreferences("bp_wallet_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                getSharedPreferences("bp_secure_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                
                auditPrefs.edit().putBoolean("v2_reset_done", true).apply()
                android.util.Log.i("MainActivity", "Completed One-Time Factory Reset for Audit")
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error in reset check", e)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsState()

                // Session Timeout Check (25 Minutes)
                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        while (true) {
                            kotlinx.coroutines.delay(30000) // Check every 30 seconds
                            val elapsed = System.currentTimeMillis() - viewModel.lastActivityTime.value
                            if (elapsed >= 25 * 60 * 1000) { // 25 Minutes
                                viewModel.logout()
                            }
                        }
                    }
                }

                BPWalletApp(viewModel)
            }
        }
    }
}

@Composable
fun BPWalletApp(
    viewModel: BPWalletViewModel
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val snackMessage by viewModel.snackMessage.collectAsState()
    val broadcast by viewModel.recentBroadcast.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val celebrationEvent by viewModel.celebrationEvent.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event: com.bp.wallet.util.UiEvent ->
            when (event) {
                is com.bp.wallet.util.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = event.duration
                    )
                }
                is com.bp.wallet.util.UiEvent.ShowToast -> {
                    android.widget.Toast.makeText(
                        context,
                        event.message,
                        if (event.isLong) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

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
                ScreenType.FORGOT_PASSWORD -> ForgotPasswordScreen(viewModel = viewModel)
                ScreenType.RESET_PASSWORD -> ResetPasswordScreen(viewModel = viewModel)
                ScreenType.VERIFY_EMAIL -> VerifyEmailScreen(viewModel = viewModel)
                ScreenType.USER_HOME -> UserHomeScreen(viewModel = viewModel)
                ScreenType.USER_DEPOSIT -> UserDepositScreen(viewModel = viewModel)
                ScreenType.USER_WITHDRAW -> UserWithdrawScreen(viewModel = viewModel)
                ScreenType.USER_HISTORY -> UserHistoryScreen(viewModel = viewModel)
                ScreenType.USER_PROFILE -> UserProfileScreen(viewModel = viewModel)
                ScreenType.ADMIN_DASHBOARD, ScreenType.ADMIN_LIVE_CONTROL -> AdminDashboardScreen(viewModel = viewModel)
                ScreenType.ADMIN_USERS_CRM -> AdminUserManagementScreen(viewModel = viewModel)
                ScreenType.ADMIN_DEPOSITS -> AdminDepositsScreen(viewModel = viewModel)
                ScreenType.ADMIN_WITHDRAWALS -> AdminWithdrawalsScreen(viewModel = viewModel)
                ScreenType.ADMIN_BANK_MANAGEMENT -> AdminBankManagementScreen(viewModel = viewModel)
                ScreenType.ADMIN_NOTIFICATIONS -> AdminNotificationsScreen(viewModel = viewModel)
                ScreenType.ADMIN_AUDIT_LOGS -> AdminAuditLogsScreen(viewModel = viewModel)
                ScreenType.ADMIN_TRANSACTIONS -> AdminDepositsScreen(viewModel = viewModel) // Unified or split? I split them.
                ScreenType.ADMIN_SETTINGS -> AdminSettingsScreen(viewModel = viewModel)
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    BPLottieLoadingView(size = 180)
                }
            }

            celebrationEvent?.let { event ->
                TransactionCelebrationDialog(
                    event = event,
                    onDismiss = { viewModel.clearCelebration() }
                )
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
