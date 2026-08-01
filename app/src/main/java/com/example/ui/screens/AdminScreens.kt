package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MasterAgent
import com.example.model.PaymentGateway
import com.example.model.TransactionRequest
import com.example.model.UserAccount
import com.example.ui.components.ShimmerDashboardSkeleton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import com.example.data.SupabaseCloudManager
import com.example.ui.viewmodel.BPWalletViewModel
import com.example.ui.viewmodel.ScreenType

@Composable
fun AdminDashboardScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.allUsers.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val masters by viewModel.masterAgents.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val normalUsers = users.filter { !it.isSuperAdmin && !it.isCountrySuperMaster && !it.isSupportStaff && !it.isReadOnlyUser && it.role != "admin" }
    val countryUsers = if (currentUser?.isSuperAdmin == true || currentUser == null) {
        normalUsers
    } else {
        normalUsers.filter { u -> u.country.equals(currentUser?.country, true) || u.currency.equals(currentUser?.currency, true) }
    }
    val countryTxs = if (currentUser?.isSuperAdmin == true || currentUser == null) {
        transactions
    } else {
        transactions.filter { tx -> tx.country.equals(currentUser?.country, true) || tx.currency.equals(currentUser?.currency, true) }
    }
    val pendingDeposits = countryTxs.count { it.type == "DEPOSIT" && it.status == "Pending" }
    val pendingPayouts = countryTxs.count { it.type == "WITHDRAW" && it.status == "Pending" }
    val totalBalanceSum = countryUsers.sumOf { it.walletBalance }

    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }

    var showDrawer by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AdminTopBar(
                    title = "ADMIN / DASHBOARD",
                    subtitle = "Executive Overview",
                    currentUser = currentUser,
                    onOpenDrawer = { showDrawer = true },
                    onLogout = { viewModel.logout() }
                )
            }
        ) { innerPadding ->
            if (isLoading) {
                ShimmerDashboardSkeleton(modifier = Modifier.padding(innerPadding))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Supabase Cloud Database Permanent Storage Card
                SupabaseCloudStorageCard(viewModel = viewModel)

                // 1. USERS (Full width card)
                AdminStatCard(
                    title = "USERS",
                    value = "${countryUsers.size}",
                    subtitle = "↗ +12.4% vs last week",
                    icon = Icons.Default.Group,
                    iconTint = Color(0xFF2563EB),
                    iconBg = Color(0xFFDBEAFE),
                    subtitleColor = BPGreenDark
                )

                // 2. USER CIRCULATION (Full width card)
                AdminStatCard(
                    title = "USER CIRCULATION",
                    value = "PKR ${totalBalanceSum.toInt()}",
                    subtitle = "Across ${countryUsers.size} Active Wallets",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = BPGreenPrimary,
                    iconBg = BPGreenLight,
                    subtitleColor = Slate500
                )

                // 3. NET PLATFORM P&L (Full width card)
                AdminStatCard(
                    title = "NET PLATFORM P&L",
                    value = "PKR ${totalBalanceSum.toInt()}",
                    subtitle = "↗ Net Positive Reserve",
                    icon = Icons.Default.AttachMoney,
                    iconTint = BPGreenPrimary,
                    iconBg = BPGreenLight,
                    subtitleColor = BPGreenDark
                )

                // 4. MASTER AGENTS (Full width card)
                AdminStatCard(
                    title = "MASTER AGENTS",
                    value = "${masters.size} Masters",
                    subtitle = "PKR • AED • SAR Regions",
                    icon = Icons.Default.Bolt,
                    iconTint = Color(0xFFF97316),
                    iconBg = Color(0xFFFFEDD5),
                    subtitleColor = Slate500
                )

                // 5. PENDING DEPOSITS (Full width card)
                AdminStatCard(
                    title = "PENDING DEPOSITS",
                    value = "$pendingDeposits",
                    subtitle = "Awaiting Verification",
                    icon = Icons.Default.ArrowDownward,
                    iconTint = BPGreenPrimary,
                    iconBg = BPGreenLight,
                    subtitleColor = Slate500
                )

                // 6. PENDING PAYOUTS (Full width card)
                AdminStatCard(
                    title = "PENDING PAYOUTS",
                    value = "$pendingPayouts",
                    subtitle = "Payout Approvals",
                    icon = Icons.Default.ArrowUpward,
                    iconTint = Color(0xFFEF4444),
                    iconBg = Color(0xFFFEE2E2),
                    subtitleColor = Slate500
                )

                // 7. USER ACQUISITION CHART CARD
                UserAcquisitionChartCard(
                    usersCount = countryUsers.size,
                    activeTradersCount = 1
                )

                // 8. REAL-TIME PUSH BROADCAST TOOL
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
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = "Broadcast",
                                    tint = BPGreenDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Real-Time Push Broadcast",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Slate900
                                )
                            }
                            StatusBadge(status = "WebSocket Engine")
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = { broadcastTitle = it },
                            label = { Text("Announcement Title") },
                            placeholder = { Text("e.g. 🚀 BetPro Exchange PSL Match Live!") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BPGreenPrimary,
                                unfocusedBorderColor = Slate500
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = broadcastMessage,
                            onValueChange = { broadcastMessage = it },
                            label = { Text("Notification Message") },
                            placeholder = { Text("Message will trigger an instant push alert on all user devices...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BPGreenPrimary,
                                unfocusedBorderColor = Slate500
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.broadcastPushAlert(broadcastTitle, broadcastMessage)
                                broadcastTitle = ""
                                broadcastMessage = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BPGreenPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BROADCAST PUSH ALERT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // 9. REAL-TIME PLATFORM ACTIVITY STREAM CARD
                RealTimeActivityStreamCard()

                // 10. RECENT FINANCIAL LEDGER CARD
                RecentFinancialLedgerCard(
                    transactions = countryTxs,
                    onViewAll = { viewModel.setScreen(ScreenType.ADMIN_TRANSACTIONS) }
                )
            }
            }
        }

        AdminEnterpriseDrawer(
            showDrawer = showDrawer,
            onDismiss = { showDrawer = false },
            currentScreen = ScreenType.ADMIN_DASHBOARD,
            onNavigate = { viewModel.setScreen(it) },
            currentUser = currentUser,
            onLogout = { viewModel.logout() }
        )
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = BPGreenPrimary,
    iconBg: Color = BPGreenLight,
    subtitleColor: Color = BPGreenDark
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Slate900
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = subtitleColor
            )
        }
    }
}

@Composable
fun UserAcquisitionChartCard(usersCount: Int, activeTradersCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "User Acquisition",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Slate900
                    )
                    Text(
                        text = "Growth in accounts vs active traders",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Users",
                        tint = Slate700,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(0.55f)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(Color(0xFF3B82F6))
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(0.55f)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(BPGreenPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Users",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
            }
        }
    }
}

@Composable
fun RealTimeActivityStreamCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Activity",
                        tint = Slate700,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Real-Time Platform Activity Stream",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Slate900
                    )
                }
                Text(
                    text = "Auto-syncs",
                    fontSize = 11.sp,
                    color = Slate500,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No platform activity recorded yet. Real-time updates will appear here as users deposit or request withdrawals.",
                fontSize = 13.sp,
                color = Slate500,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun RecentFinancialLedgerCard(
    transactions: List<TransactionRequest>,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewAll() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column {
                    Text(
                        text = "Recent Financial Ledger",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Slate900
                    )
                    Text(
                        text = "Latest deposits and withdrawal requests",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View All Transactions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BPGreenDark
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = BPGreenDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (transactions.isEmpty()) {
                Text(
                    text = "No transactions recorded yet. New user deposit/withdrawal requests will appear here in real-time.",
                    fontSize = 13.sp,
                    color = Slate500,
                    lineHeight = 18.sp
                )
            } else {
                transactions.take(3).forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${tx.type}: ${tx.userName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${tx.currency} ${tx.amount.toInt()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (tx.type == "DEPOSIT") BPGreenDark else Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminEnterpriseDrawer(
    showDrawer: Boolean,
    onDismiss: () -> Unit,
    currentScreen: ScreenType,
    onNavigate: (ScreenType) -> Unit,
    currentUser: UserAccount?,
    onLogout: () -> Unit
) {
    if (!showDrawer) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .clickable(enabled = false) {},
            color = Color.White,
            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BPGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "BP",
                                    tint = BPGreenDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "BP ENTERPRISE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "ADMIN CONTROL",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Slate700,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    AdminDrawerItem(
                        label = "Dashboard",
                        icon = Icons.Default.Dashboard,
                        selected = currentScreen == ScreenType.ADMIN_DASHBOARD,
                        onClick = {
                            onNavigate(ScreenType.ADMIN_DASHBOARD)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdminDrawerItem(
                        label = "Live Control",
                        icon = Icons.Default.FlashOn,
                        selected = currentScreen == ScreenType.ADMIN_LIVE_CONTROL,
                        onClick = {
                            onNavigate(ScreenType.ADMIN_LIVE_CONTROL)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdminDrawerItem(
                        label = "Users CRM",
                        icon = Icons.Default.People,
                        selected = currentScreen == ScreenType.ADMIN_USERS_CRM,
                        onClick = {
                            onNavigate(ScreenType.ADMIN_USERS_CRM)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdminDrawerItem(
                        label = "Master Agents",
                        icon = Icons.Default.Security,
                        selected = currentScreen == ScreenType.ADMIN_MASTER_AGENTS,
                        onClick = {
                            onNavigate(ScreenType.ADMIN_MASTER_AGENTS)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdminDrawerItem(
                        label = "Transactions",
                        icon = Icons.Default.ReceiptLong,
                        selected = currentScreen == ScreenType.ADMIN_TRANSACTIONS,
                        onClick = {
                            onNavigate(ScreenType.ADMIN_TRANSACTIONS)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdminDrawerItem(
                        label = "Settings",
                        icon = Icons.Default.Settings,
                        selected = currentScreen == ScreenType.ADMIN_SETTINGS,
                        onClick = {
                            onNavigate(ScreenType.ADMIN_SETTINGS)
                            onDismiss()
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BPGreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "UA",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentUser?.fullName ?: "Umar Admin",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Slate900
                            )
                            Text(
                                text = currentUser?.role?.uppercase() ?: "SUPER ADMIN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = BPGreenDark
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            onDismiss()
                            onLogout()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Logout",
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) BPGreenPrimary else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.White else Slate500,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Slate800
        )
    }
}

@Composable
fun AdminTopBar(
    title: String,
    subtitle: String,
    currentUser: UserAccount? = null,
    onOpenDrawer: () -> Unit = {},
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
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Slate700,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Text(
                        text = subtitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Supabase Cloud Active Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFE8F5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Supabase Cloud DB",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Supabase",
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // User Switch Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.clickable { onLogout() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "User View",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "User",
                            color = Slate800,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Bell notification button with red 1 badge
                Box {
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Slate700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // UA Circle Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BPGreenPrimary)
                        .clickable { onLogout() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "UA",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AdminBottomNavBar(
    currentScreen: ScreenType,
    onNavigate: (ScreenType) -> Unit
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
            AdminNavItem(
                label = "Dashboard",
                icon = Icons.Default.Dashboard,
                selected = currentScreen == ScreenType.ADMIN_DASHBOARD,
                onClick = { onNavigate(ScreenType.ADMIN_DASHBOARD) }
            )
            AdminNavItem(
                label = "Users CRM",
                icon = Icons.Default.People,
                selected = currentScreen == ScreenType.ADMIN_USERS_CRM,
                onClick = { onNavigate(ScreenType.ADMIN_USERS_CRM) }
            )
            AdminNavItem(
                label = "Master Agents",
                icon = Icons.Default.Security,
                selected = currentScreen == ScreenType.ADMIN_MASTER_AGENTS,
                onClick = { onNavigate(ScreenType.ADMIN_MASTER_AGENTS) }
            )
            AdminNavItem(
                label = "Transactions",
                icon = Icons.Default.ReceiptLong,
                selected = currentScreen == ScreenType.ADMIN_TRANSACTIONS,
                onClick = { onNavigate(ScreenType.ADMIN_TRANSACTIONS) }
            )
        }
    }
}

@Composable
fun AdminNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) BPGreenDark else Slate500,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) BPGreenDark else Slate500
        )
    }
}

@Composable
fun AdminLiveControlScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var showDrawer by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AdminTopBar(
                    title = "ADMIN / LIVE CONTROL",
                    subtitle = "Real-time Platform Streams & Activity",
                    currentUser = currentUser,
                    onOpenDrawer = { showDrawer = true },
                    onLogout = { viewModel.logout() }
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
                RealTimeActivityStreamCard()

                // Active Traders & System Stats Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "System Status",
                                tint = BPGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "System Load & Operational Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Slate900
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "All BetPro payment gateways, deposit processors, and master agent settlement channels are operational.",
                            fontSize = 13.sp,
                            color = Slate500,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        AdminEnterpriseDrawer(
            showDrawer = showDrawer,
            onDismiss = { showDrawer = false },
            currentScreen = ScreenType.ADMIN_LIVE_CONTROL,
            onNavigate = { viewModel.setScreen(it) },
            currentUser = currentUser,
            onLogout = { viewModel.logout() }
        )
    }
}

@Composable
fun SupabaseCloudStorageCard(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val connectionStatus by SupabaseCloudManager.connectionStatus.collectAsState()
    val isOnline by SupabaseCloudManager.isOnline.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val gateways by viewModel.paymentGateways.collectAsState()

    var showSqlDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    if (showSqlDialog) {
        AlertDialog(
            onDismissRequest = { showSqlDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "SQL",
                        tint = BPGreenDark
                    )
                    Text(
                        text = "Supabase SQL Setup Script",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Slate900
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Copy this SQL code and execute it in your Supabase SQL Editor (vmglozamlzwjbigareie.supabase.co) to initialize permanent database tables:",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = SupabaseCloudManager.SQL_SCHEMA_SCRIPT,
                            fontSize = 10.sp,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(SupabaseCloudManager.SQL_SCHEMA_SCRIPT))
                        Toast.makeText(context, "SQL script copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BPGreenDark)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy SQL Script", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSqlDialog = false }) {
                    Text("Close", color = Slate600, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Supabase",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Supabase Cloud Database",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Slate900
                        )
                        Text(
                            text = "Permanent PostgreSQL Storage",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isOnline) Color(0xFFA5D6A7) else Color(0xFFFFCC80))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF2E7D32) else Color(0xFFEF6C00))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOnline) "Connected" else "Connecting...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) Color(0xFF1B5E20) else Color(0xFFE65100)
                        )
                    }
                }
            }

            // URL Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Slate50
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SUPABASE PROJECT URL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate500
                        )
                        Text(
                            text = SupabaseCloudManager.SUPABASE_URL,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                    }
                    Text(
                        text = "PostgREST v1",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BPGreenDark
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "${allUsers.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                        Text(text = "Synced Users", fontSize = 10.sp, color = Slate600)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "${allTransactions.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                        Text(text = "Synced Txs", fontSize = 10.sp, color = Slate600)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "${gateways.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                        Text(text = "Gateways", fontSize = 10.sp, color = Slate600)
                    }
                }
            }

            Text(
                text = "Status: $connectionStatus",
                fontSize = 11.sp,
                color = Slate600,
                fontWeight = FontWeight.Medium
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showSqlDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BPGreenDark)
                ) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SQL Setup Script", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Synchronizing with Supabase Cloud...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BPGreenDark)
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Cloud Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminSettingsScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val paymentGateways by viewModel.paymentGateways.collectAsState()
    val whatsappHelplineNumber by viewModel.whatsappHelplineNumber.collectAsState()
    var editedHelplineNumber by remember(whatsappHelplineNumber) { mutableStateOf(whatsappHelplineNumber) }
    val exchangeWebsiteUrl by viewModel.exchangeWebsiteUrl.collectAsState()
    var editedExchangeUrl by remember(exchangeWebsiteUrl) { mutableStateOf(exchangeWebsiteUrl) }
    var showDrawer by remember { mutableStateOf(false) }

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var passwordSuccess by remember { mutableStateOf(false) }

    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }

    var gwName by remember { mutableStateOf("") }
    var gwCurrency by remember { mutableStateOf("PKR") }
    var gwCountry by remember { mutableStateOf("Pakistan") }
    var gwTitle by remember { mutableStateOf("") }
    var gwAccount by remember { mutableStateOf("") }
    var gwMinDeposit by remember { mutableStateOf("500") }
    var showAddGateway by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AdminTopBar(
                    title = "ADMIN / SETTINGS",
                    subtitle = "System & Security Configuration",
                    currentUser = currentUser,
                    onOpenDrawer = { showDrawer = true },
                    onLogout = { viewModel.logout() }
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
                // Supabase Cloud Database Permanent Storage Card
                SupabaseCloudStorageCard(viewModel = viewModel)

                // SuperAdmin / Admin Change Password Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Security",
                                tint = BPGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Change SuperAdmin / Admin Password",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "Update your login credentials securely",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }

                        if (passwordError.isNotEmpty()) {
                            Text(
                                text = passwordError,
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (passwordSuccess) {
                            Text(
                                text = "✓ Password updated successfully!",
                                color = BPGreenDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                passwordError = ""
                                passwordSuccess = false
                            },
                            label = { Text("New Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                passwordError = ""
                                passwordSuccess = false
                            },
                            label = { Text("Confirm New Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                if (newPassword.isBlank() || newPassword.length < 4) {
                                    passwordError = "Password must be at least 4 characters."
                                    return@Button
                                }
                                if (newPassword != confirmPassword) {
                                    passwordError = "Passwords do not match."
                                    return@Button
                                }
                                viewModel.updateAdminPassword(newPassword)
                                newPassword = ""
                                confirmPassword = ""
                                passwordSuccess = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Update Password",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // WhatsApp Helpline Configuration Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "WhatsApp Helpline",
                                tint = BPGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "WhatsApp Helpline Number",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "Set official support number for users & agents",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }

                        OutlinedTextField(
                            value = editedHelplineNumber,
                            onValueChange = { editedHelplineNumber = it },
                            label = { Text("WhatsApp Number (e.g. +923001234567)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.updateWhatsAppHelpline(editedHelplineNumber)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Helpline Number",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Exchange Website URL Card (BP ID Link)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Exchange Website URL",
                                tint = BPGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "BP ID Exchange Link",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "Configure dynamic URL loaded inside user WebView",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }

                        OutlinedTextField(
                            value = editedExchangeUrl,
                            onValueChange = { editedExchangeUrl = it },
                            label = { Text("Exchange Website URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.updateExchangeWebsiteUrl(editedExchangeUrl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Exchange URL",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Push Alert Announcement Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Broadcast",
                                tint = BPGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Push Notification Broadcast",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "Send alert announcements to all users",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }

                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = { broadcastTitle = it },
                            label = { Text("Announcement Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = broadcastMessage,
                            onValueChange = { broadcastMessage = it },
                            label = { Text("Announcement Message") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.broadcastPushAlert(broadcastTitle, broadcastMessage)
                                broadcastTitle = ""
                                broadcastMessage = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Broadcast Alert",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Payment Gateways Management Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Gateways",
                                    tint = BPGreenDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Payment Gateways & Methods",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "Add or remove deposit methods for users",
                                        fontSize = 12.sp,
                                        color = Slate500
                                    )
                                }
                            }

                            TextButton(onClick = { showAddGateway = !showAddGateway }) {
                                Text(
                                    text = if (showAddGateway) "Cancel" else "+ Add",
                                    fontWeight = FontWeight.Bold,
                                    color = BPGreenDark
                                )
                            }
                        }

                        if (showAddGateway) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Slate100)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("New Payment Gateway", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = gwName,
                                    onValueChange = { gwName = it },
                                    label = { Text("Gateway Name (e.g. UPI, bKash, USDT)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = gwCurrency,
                                        onValueChange = { gwCurrency = it },
                                        label = { Text("Currency") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = gwCountry,
                                        onValueChange = { gwCountry = it },
                                        label = { Text("Country") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = gwTitle,
                                    onValueChange = { gwTitle = it },
                                    label = { Text("Account Title / Receiver Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = gwAccount,
                                    onValueChange = { gwAccount = it },
                                    label = { Text("Account Number / IBAN / Wallet Address") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = gwMinDeposit,
                                    onValueChange = { gwMinDeposit = it },
                                    label = { Text("Min Deposit Amount") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        if (gwName.isNotBlank() && gwAccount.isNotBlank()) {
                                            viewModel.addPaymentGateway(
                                                name = gwName,
                                                currency = gwCurrency,
                                                country = gwCountry,
                                                title = gwTitle,
                                                accountNumber = gwAccount,
                                                minDeposit = gwMinDeposit.toDoubleOrNull() ?: 500.0
                                            )
                                            gwName = ""
                                            gwTitle = ""
                                            gwAccount = ""
                                            showAddGateway = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Payment Gateway", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        paymentGateways.forEach { gw ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Slate100)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${gw.name} (${gw.currency} - ${gw.country})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "Acc: ${gw.accountNumber} | Title: ${gw.title}",
                                        fontSize = 11.sp,
                                        color = Slate700
                                    )
                                    Text(
                                        text = "Min Deposit: ${gw.minDeposit.toInt()} ${gw.currency}",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deletePaymentGateway(gw.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Gateway",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // System Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Platform Details & Version",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate900
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("System Environment", fontSize = 13.sp, color = Slate500)
                            Text("BetPro Multi-Currency Live", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("App Version", fontSize = 13.sp, color = Slate500)
                            Text("2.5.0 Enterprise", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                    }
                }
            }
        }

        AdminEnterpriseDrawer(
            showDrawer = showDrawer,
            onDismiss = { showDrawer = false },
            currentScreen = ScreenType.ADMIN_SETTINGS,
            onNavigate = { viewModel.setScreen(it) },
            currentUser = currentUser,
            onLogout = { viewModel.logout() }
        )
    }
}
