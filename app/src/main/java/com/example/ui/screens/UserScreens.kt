package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.model.PaymentGateway
import com.example.model.TransactionRequest
import com.example.model.UserAccount
import com.example.ui.components.StatusBadge
import com.example.ui.components.WhatsAppHelplineButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.BPWalletViewModel
import com.example.ui.viewmodel.ScreenType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserHomeScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val allTxs by viewModel.allTransactions.collectAsState()
    val showBetProModal by viewModel.showBetProExchangeModal.collectAsState()

    val u = user ?: return
    val userTxs = allTxs.filter { it.userId == u.id }
    val approvedDepositsCount = userTxs.count { it.type == "DEPOSIT" && it.status == "Approved" }
    val approvedWithdrawalsCount = userTxs.count { it.type == "WITHDRAW" && it.status == "Approved" }
    val totalWithdrawalSum = userTxs.filter { it.type == "WITHDRAW" && it.status == "Approved" }.sumOf { it.amount }

    if (showBetProModal) {
        BetProExchangeModal(
            user = u,
            onDismiss = { viewModel.setBetProExchangeModalVisible(false) },
            onCopy = { txt -> viewModel.showSnack("Copied: $txt") }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UserTopBar(user = u, onLogout = { viewModel.logout() })
        },
        bottomBar = {
            UserBottomNavBar(
                currentScreen = ScreenType.USER_HOME,
                onNavigate = { viewModel.setScreen(it) },
                onBpIdClick = { viewModel.setBetProExchangeModalVisible(true) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cards Row: Total Deposit & Total Withdrawal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // TOTAL DEPOSIT CARD
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL DEPOSIT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500
                            )
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Deposit",
                                tint = BPGreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${u.displayCurrencySymbol}${u.walletBalance.toInt()}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$approvedDepositsCount Approved Requests",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.setScreen(ScreenType.USER_DEPOSIT) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BPGreenPrimary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "+ DEPOSIT (${u.currency})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // TOTAL WITHDRAWAL CARD
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL WITHDRAWAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500
                            )
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = "Withdrawal",
                                tint = BPGoldDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${u.displayCurrencySymbol}${totalWithdrawalSum.toInt()}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$approvedWithdrawalsCount Approved Requests",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.setScreen(ScreenType.USER_WITHDRAW) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Slate700,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "WITHDRAW (${u.currency})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // EXCHANGE ID CREDENTIALS CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Exchange",
                                tint = BPGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EXCHANGE ID CREDENTIALS",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Slate900
                            )
                        }
                        StatusBadge(
                            status = if (u.betproIdStatus == "Active") "Active ID" else "Pending ID"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = Slate100)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Username Row
                    Text(
                        text = "BETPRO USERNAME",
                        fontSize = 11.sp,
                        color = Slate500,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = u.betproUsername,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Row
                    Text(
                        text = "BETPRO PASSWORD",
                        fontSize = 11.sp,
                        color = Slate500,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = u.betproPassword,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Yellow tip box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BPGoldSoft)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Tip",
                            tint = BPGoldDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Use these credentials on the official exchange website to log in directly.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate900
                        )
                    }
                }
            }

            // OFFICIAL INSTRUCTIONS CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = BPGreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OFFICIAL INSTRUCTIONS",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Slate900
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Urdu Text
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BPGreenLight)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "براہ کرم اپنا بی پی ایکسچینج آئی ڈی حاصل کرنے کے لیے کم از کم PKR 500 کا ڈپازٹ کریں۔ ڈپازٹ کی تصدیق کے بعد ایڈمن آپ کا آئی ڈی ایکٹیو کر دے گا۔",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // English Text
                    Text(
                        text = "Please deposit a minimum of ${u.currency} 500 to get your official BP Exchange ID credentials. Once approved, admin will activate your BP username and password.",
                        fontSize = 13.sp,
                        color = Slate700,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // WhatsApp Helpline Button
            WhatsAppHelplineButton(
                onClick = {
                    viewModel.showSnack("Connecting to Official BP Wallet WhatsApp Support @bptraders_pkr...")
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun UserTopBar(
    user: UserAccount,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BPGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.fullName.take(1).uppercase(),
                        color = BPGreenDark,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = user.fullName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Slate900
                    )
                    Text(
                        text = "@bptraders_${user.currency.lowercase()}",
                        fontSize = 12.sp,
                        color = BPGreenDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Currency FIXED Badge
                StatusBadge(status = "${user.currency} FIXED")

                // Notification Bell
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Slate700
                    )
                }

                // Power / Logout button
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Logout",
                        tint = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@Composable
fun UserBottomNavBar(
    currentScreen: ScreenType,
    onNavigate: (ScreenType) -> Unit,
    onBpIdClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            NavBarItem(
                label = "Home",
                icon = Icons.Default.Home,
                selected = currentScreen == ScreenType.USER_HOME,
                onClick = { onNavigate(ScreenType.USER_HOME) }
            )

            // Deposit
            NavBarItem(
                label = "Deposit",
                icon = Icons.Default.AddCircleOutline,
                selected = currentScreen == ScreenType.USER_DEPOSIT,
                onClick = { onNavigate(ScreenType.USER_DEPOSIT) }
            )

            // BP ID Center Gold Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .clickable { onBpIdClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(BPGold)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "BP ID",
                        tint = Slate900,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = "BP ID",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
            }

            // Withdraw
            NavBarItem(
                label = "Withdraw",
                icon = Icons.Default.RemoveCircleOutline,
                selected = currentScreen == ScreenType.USER_WITHDRAW,
                onClick = { onNavigate(ScreenType.USER_WITHDRAW) }
            )

            // History
            NavBarItem(
                label = "History",
                icon = Icons.Default.History,
                selected = currentScreen == ScreenType.USER_HISTORY,
                onClick = { onNavigate(ScreenType.USER_HISTORY) }
            )

            // Profile
            NavBarItem(
                label = "Profile",
                icon = Icons.Default.Person,
                selected = currentScreen == ScreenType.USER_PROFILE,
                onClick = { onNavigate(ScreenType.USER_PROFILE) }
            )
        }
    }
}

@Composable
fun NavBarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) BPGreenPrimary else Slate500,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) BPGreenPrimary else Slate500
        )
    }
}

@Composable
fun BetProExchangeModal(
    user: UserAccount,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "BetPro Exchange",
                    tint = BPGreenPrimary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BetPro Exchange",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Slate900
                )
                Text(
                    text = "https://bpexch.live",
                    fontSize = 12.sp,
                    color = BPGreenDark,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bpexch.live"))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback if browser unavailable
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BPGreenPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Open BetPro Exchange",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open URL",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Official website will open in a secure browser. Login with your device saved passwords or use your assigned credentials below:",
                    fontSize = 12.sp,
                    color = Slate500,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "ID: ${user.betproUsername}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Pass: ${user.betproPassword}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            IconButton(onClick = { onCopy("${user.betproUsername} / ${user.betproPassword}") }) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Slate700)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = Slate700)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}
