package com.bp.uunwlm

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
import com.bp.uunwlm.ui.components.*
import com.bp.uunwlm.ui.screens.*
import com.bp.uunwlm.ui.theme.*
import com.bp.uunwlm.ui.viewmodel.BPWalletViewModel
import com.bp.uunwlm.ui.viewmodel.ScreenType
import kotlinx.coroutines.flow.*

class MainActivity : FragmentActivity() {
    private val viewModel: BPWalletViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[BPWalletViewModel::class.java]
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.recordUserActivity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // One-time Purge of Local Data for Migration/Clean Audit
        val auditPrefs = getSharedPreferences("audit_reset", android.content.Context.MODE_PRIVATE)
        val isResetDone = auditPrefs.getBoolean("v2_reset_done", false)
        if (!isResetDone) {
            // Clear standard session prefs
            getSharedPreferences("bp_wallet_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            // Clear secure prefs if any (using same name as in Repo if possible, or just targeting common names)
            getSharedPreferences("bp_secure_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            
            // Attempt to clear Firestore cache
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().clearPersistence()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Firestore cache clear failed", e)
            }
            
            auditPrefs.edit().putBoolean("v2_reset_done", true).apply()
            android.util.Log.i("MainActivity", "Completed One-Time Factory Reset for Audit")
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
                val isBiometricAuthenticated by viewModel.isBiometricAuthenticated.collectAsState()
                val currentUser by viewModel.currentUser.collectAsState()
                val lastActivityTime by viewModel.lastActivityTime.collectAsState()

                val biometricAuthenticator = remember { com.bp.uunwlm.util.BiometricAuthenticator(this) }
                
                // Session Timeout Check (25 Minutes)
                LaunchedEffect(lastActivityTime, isBiometricAuthenticated, currentUser, isBiometricEnabled) {
                    if (currentUser != null) {
                        while (true) {
                            val elapsed = System.currentTimeMillis() - lastActivityTime
                            if (elapsed >= 25 * 60 * 1000) { // 25 Minutes
                                if (isBiometricEnabled) {
                                    viewModel.setBiometricAuthenticated(false)
                                } else {
                                    viewModel.logout()
                                }
                                break
                            }
                            kotlinx.coroutines.delay(30000) // Check every 30 seconds
                        }
                    }
                }

                LaunchedEffect(currentUser) {
                    if (currentUser != null && isBiometricEnabled && !isBiometricAuthenticated) {
                        if (biometricAuthenticator.isBiometricAvailable()) {
                            biometricAuthenticator.promptBiometricAuth(
                                activity = this@MainActivity,
                                onSuccess = {
                                    viewModel.setBiometricAuthenticated(true)
                                },
                                onError = { _, _ ->
                                    // Handle error (maybe logout if required or just show snackbar)
                                },
                                onFailed = {
                                    // Handle failure
                                }
                            )
                        } else {
                            // Biometric was enabled but hardware is gone or removed? 
                            // Just allow access or warn
                            viewModel.setBiometricAuthenticated(true)
                        }
                    } else if (currentUser == null) {
                        viewModel.setBiometricAuthenticated(false)
                    }
                }

                if (currentUser != null && isBiometricEnabled && !isBiometricAuthenticated) {
                    // Show a blurred or lock screen if desired, or just wait for biometric
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Wallet Locked",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please authenticate to access your wallet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = {
                                biometricAuthenticator.promptBiometricAuth(
                                    activity = this@MainActivity,
                                    onSuccess = { viewModel.setBiometricAuthenticated(true) },
                                    onError = { _, _ -> },
                                    onFailed = { }
                                )
                            }) {
                                Text("Unlock with Biometrics")
                            }
                        }
                    }
                } else {
                    BPWalletApp(viewModel)
                }
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
        viewModel.uiEvent.collect { event: com.bp.uunwlm.util.UiEvent ->
            when (event) {
                is com.bp.uunwlm.util.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = event.duration
                    )
                }
                is com.bp.uunwlm.util.UiEvent.ShowToast -> {
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
                ScreenType.USER_HOME -> UserHomeScreen(viewModel = viewModel)
                ScreenType.USER_DEPOSIT -> UserDepositScreen(viewModel = viewModel)
                ScreenType.USER_WITHDRAW -> UserWithdrawScreen(viewModel = viewModel)
                ScreenType.USER_HISTORY -> UserHistoryScreen(viewModel = viewModel)
                ScreenType.USER_PROFILE -> UserProfileScreen(viewModel = viewModel)
                ScreenType.ADMIN_DASHBOARD, ScreenType.ADMIN_LIVE_CONTROL -> AdminDashboardScreen(viewModel = viewModel)
                ScreenType.ADMIN_USERS_CRM, ScreenType.ADMIN_MASTER_AGENTS -> AdminUsersCrmScreen(viewModel = viewModel)
                ScreenType.ADMIN_TRANSACTIONS -> AdminTransactionsScreen(viewModel = viewModel)
                ScreenType.ADMIN_SETTINGS -> AdminSettingsScreen(viewModel = viewModel)
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BPGreenPrimary)
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
