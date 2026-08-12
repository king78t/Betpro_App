package com.bp.uunwlm.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bp.uunwlm.R
import com.bp.uunwlm.model.PaymentGateway
import com.bp.uunwlm.model.TransactionRequest
import com.bp.uunwlm.model.UserAccount
import com.bp.uunwlm.ui.components.BPLogoIcon
import com.bp.uunwlm.ui.components.ShimmerDashboardSkeleton
import com.bp.uunwlm.ui.components.StatusBadge
import com.bp.uunwlm.ui.theme.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import com.bp.uunwlm.data.SupabaseCloudManager
import com.bp.uunwlm.ui.viewmodel.BPWalletViewModel
import com.bp.uunwlm.ui.viewmodel.ScreenType

@Composable
fun AdminDashboardScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.allUsers.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val normalUsers = users.filter { !it.isSuperAdmin && it.role != "admin" }
    val totalDeposits = transactions.filter { it.type == "DEPOSIT" && it.status == "Approved" }.sumOf { it.amount }
    val totalWithdrawals = transactions.filter { it.type == "WITHDRAW" && it.status == "Approved" }.sumOf { it.amount }
    val totalWalletBalance = normalUsers.sumOf { it.walletBalance }

    var showDrawer by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                AdminTopBarRedesigned(
                    title = "Dashboard",
                    subtitle = "Financial Overview",
                    onOpenDrawer = { showDrawer = true },
                    onLogout = { viewModel.logout() }
                )
            }
        ) { innerPadding ->
            if (isLoading) {
                ShimmerDashboardSkeleton(modifier = Modifier.padding(innerPadding))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. STATS GRID (5 Cards)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AdminStatCardModern(
                                    title = "Total Users",
                                    value = "${normalUsers.size}",
                                    icon = Icons.Default.Group,
                                    color = Color(0xFF3B82F6),
                                    modifier = Modifier.weight(1f)
                                )
                                AdminStatCardModern(
                                    title = "Active Users",
                                    value = "${normalUsers.count { it.betproIdStatus == "Active" }}",
                                    icon = Icons.Default.VerifiedUser,
                                    color = BPGreenPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AdminStatCardModern(
                                    title = "Total Deposits",
                                    value = "${currentUser?.displayCurrencySymbol ?: "PKR"} ${totalDeposits.toInt()}",
                                    icon = Icons.Default.ArrowDownward,
                                    color = BPGreenPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                AdminStatCardModern(
                                    title = "Total Withdrawals",
                                    value = "${currentUser?.displayCurrencySymbol ?: "PKR"} ${totalWithdrawals.toInt()}",
                                    icon = Icons.Default.ArrowUpward,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AdminStatCardModern(
                                    title = "Platform Revenue",
                                    value = "${currentUser?.displayCurrencySymbol ?: "PKR"} ${(totalDeposits * 0.02).toInt()}",
                                    icon = Icons.Default.MonetizationOn,
                                    color = BPGold,
                                    modifier = Modifier.weight(1f)
                                )
                                AdminStatCardModern(
                                    title = "User Wallet Total",
                                    value = "${currentUser?.displayCurrencySymbol ?: "PKR"} ${totalWalletBalance.toInt()}",
                                    icon = Icons.Default.AccountBalanceWallet,
                                    color = Color(0xFF8B5CF6),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 2. ANALYTICS CHART
                    item {
                        AdminAnalyticsChartCard(transactions = transactions)
                    }

                    // 2. QUICK ACTIONS
                    item {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionItem(
                                label = "Add User",
                                icon = Icons.Default.PersonAdd,
                                color = Color(0xFF3B82F6),
                                onClick = { viewModel.setScreen(ScreenType.ADMIN_USERS_CRM) }
                            )
                            QuickActionItem(
                                label = "Deposits",
                                icon = Icons.Default.FileUpload,
                                color = BPGreenPrimary,
                                onClick = { viewModel.setScreen(ScreenType.ADMIN_DEPOSITS) }
                            )
                            QuickActionItem(
                                label = "Withdraws",
                                icon = Icons.Default.FileDownload,
                                color = Color(0xFFEF4444),
                                onClick = { viewModel.setScreen(ScreenType.ADMIN_WITHDRAWALS) }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionItem(
                                label = "Bank Accounts",
                                icon = Icons.Default.AccountBalance,
                                color = Color(0xFF8B5CF6),
                                onClick = { viewModel.setScreen(ScreenType.ADMIN_BANK_MANAGEMENT) }
                            )
                            QuickActionItem(
                                label = "Notifications",
                                icon = Icons.Default.Campaign,
                                color = Color(0xFFF59E0B),
                                onClick = { viewModel.setScreen(ScreenType.ADMIN_NOTIFICATIONS) }
                            )
                            QuickActionItem(
                                label = "Settings",
                                icon = Icons.Default.Settings,
                                color = Slate700,
                                onClick = { viewModel.setScreen(ScreenType.ADMIN_SETTINGS) }
                            )
                        }
                    }

                    // 3. RECENT ACTIVITY
                    item {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (auditLogs.isEmpty()) {
                                    Text(
                                        text = "No recent activity found.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate500,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                                    )
                                } else {
                                    auditLogs.take(5).forEach { log ->
                                        ActivityItem(log)
                                    }
                                    TextButton(
                                        onClick = { viewModel.setScreen(ScreenType.ADMIN_AUDIT_LOGS) },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("View All Activity", color = BPGreenPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
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
fun AdminStatCardModern(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = BPGreenPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Slate900,
                    maxLines = 1
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate500,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AdminAnalyticsChartCard(
    transactions: List<com.bp.uunwlm.model.TransactionRequest>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
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
                        text = "Transaction Volume Analytics",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "Deposits vs Withdrawals Overview",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BPGreenLight
                ) {
                    Text(
                        text = "LIVE DATA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BPGreenDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas Chart Rendering
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val w = size.width
                val h = size.height
                val barWidth = w / 15f
                val sampleData = listOf(
                    0.4f to 0.2f,
                    0.65f to 0.3f,
                    0.8f to 0.45f,
                    0.5f to 0.35f,
                    0.9f to 0.6f,
                    0.75f to 0.5f,
                    0.95f to 0.7f
                )

                sampleData.forEachIndexed { i, (depRatio, withRatio) ->
                    val x = (i * 2 + 0.5f) * barWidth
                    
                    // Deposit bar (Green gradient)
                    val depHeight = h * depRatio
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF22C55E), Color(0xFF16A34A))
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(x, h - depHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth * 0.7f, depHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Withdrawal bar (Gold gradient)
                    val withHeight = h * withRatio
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(x + barWidth * 0.8f, h - withHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth * 0.7f, withHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BPGreenPrimary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Deposits", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                }
                Spacer(modifier = Modifier.width(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BPGold))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Withdrawals", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                }
            }
        }
    }
}

@Composable
fun RowScope.QuickActionItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .weight(1f)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.1f))
                .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Slate700,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ActivityItem(log: com.bp.uunwlm.model.AuditLog) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val icon = when (log.action) {
            "LOGIN" -> Icons.Default.Login
            "DEPOSIT_APPROVAL" -> Icons.Default.CheckCircle
            "WITHDRAW_APPROVAL" -> Icons.Default.CheckCircle
            "USER_CREATION" -> Icons.Default.PersonAdd
            "UPDATE_CREDENTIALS" -> Icons.Default.VpnKey
            else -> Icons.Default.Info
        }
        val iconColor = when (log.action) {
            "DEPOSIT_APPROVAL", "WITHDRAW_APPROVAL", "USER_CREATION" -> BPGreenPrimary
            "LOGIN" -> Color(0xFF3B82F6)
            else -> Slate500
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.details,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Slate900
            )
            Text(
                text = com.bp.uunwlm.util.DateTimeUtils.getRelativeTime(log.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )
        }
    }
}

@Composable
fun AdminTopBarRedesigned(
    title: String,
    subtitle: String,
    onOpenDrawer: () -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.8f),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = Slate900)
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Slate900)
                    Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Slate500)
                }
            }
            IconButton(onClick = onLogout) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BPGreenPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = "Logout", tint = BPGreenPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
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
                            BPLogoIcon(sizeDp = 38)
                            Spacer(modifier = Modifier.width(12.dp))
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
                        label = "User Management",
                        icon = Icons.Default.People,
                        selected = currentScreen == ScreenType.ADMIN_USERS_CRM,
                        onClick = {
                            onNavigate(ScreenType.ADMIN_USERS_CRM)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdminDrawerItem(
                        label = "Deposits",
                        icon = Icons.Default.ArrowDownward,
                        selected = currentScreen == ScreenType.ADMIN_DEPOSITS,
                        onClick = {
                            onNavigate(ScreenType.ADMIN_DEPOSITS)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdminDrawerItem(
                        label = "Withdrawals",
                        icon = Icons.Default.ArrowUpward,
                        selected = currentScreen == ScreenType.ADMIN_WITHDRAWALS,
                        onClick = {
                            onNavigate(ScreenType.ADMIN_WITHDRAWALS)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdminDrawerItem(
                        label = "Bank Accounts",
                        icon = Icons.Default.AccountBalance,
                        selected = currentScreen == ScreenType.ADMIN_BANK_MANAGEMENT,
                        onClick = {
                            onNavigate(ScreenType.ADMIN_BANK_MANAGEMENT)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdminDrawerItem(
                        label = "Notification Center",
                        icon = Icons.Default.Campaign,
                        selected = currentScreen == ScreenType.ADMIN_NOTIFICATIONS,
                        onClick = {
                            onNavigate(ScreenType.ADMIN_NOTIFICATIONS)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdminDrawerItem(
                        label = "Audit Logs",
                        icon = Icons.Default.History,
                        selected = currentScreen == ScreenType.ADMIN_AUDIT_LOGS,
                        onClick = {
                            onNavigate(ScreenType.ADMIN_AUDIT_LOGS)
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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
    onNotificationClick: () -> Unit = {},
    unreadNotificationsCount: Int = 3,
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

                // Bell notification button with badge
                Box {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Slate700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (unreadNotificationsCount > 0) {
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
                                text = "$unreadNotificationsCount",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
fun AdminNotificationRightDrawer(
    showDrawer: Boolean,
    onDismiss: () -> Unit,
    pendingTxCount: Int = 1,
    onNavigateToTx: () -> Unit = {},
    onNavigateToCrm: () -> Unit = {}
) {
    if (showDrawer) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.68f)
                    .clickable(enabled = false) {}, // absorb clicks inside
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BPGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Notifications",
                                    tint = BPGreenDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Alerts Center",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "Real-time updates",
                                    fontSize = 11.sp,
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
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Slate100)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Notifications List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            NotificationCardItem(
                                title = "Pending Transactions",
                                desc = "$pendingTxCount deposit/payout request(s) awaiting approval",
                                time = "Just now",
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                iconColor = Color(0xFFE65100),
                                iconBg = Color(0xFFFFF3E0),
                                actionLabel = "Review Txs",
                                onClick = {
                                    onDismiss()
                                    onNavigateToTx()
                                }
                            )
                        }

                        item {
                            NotificationCardItem(
                                title = "BetPro Credentials",
                                desc = "Users requesting BetPro ID credential setup",
                                time = "10m ago",
                                icon = Icons.Default.VpnKey,
                                iconColor = BPGreenPrimary,
                                iconBg = BPGreenLight,
                                actionLabel = "Open CRM",
                                onClick = {
                                    onDismiss()
                                    onNavigateToCrm()
                                }
                            )
                        }

                        item {
                            NotificationCardItem(
                                title = "New Account Alert",
                                desc = "New user registered in PKR & AED regions",
                                time = "1h ago",
                                icon = Icons.Default.PersonAdd,
                                iconColor = Color(0xFF2563EB),
                                iconBg = Color(0xFFDBEAFE),
                                actionLabel = "View CRM",
                                onClick = {
                                    onDismiss()
                                    onNavigateToCrm()
                                }
                            )
                        }

                        item {
                            NotificationCardItem(
                                title = "System Audit Passed",
                                desc = "Enterprise ledger state verified healthy",
                                time = "Today",
                                icon = Icons.Default.Shield,
                                iconColor = BPGreenDark,
                                iconBg = BPGreenLight,
                                actionLabel = "Dismiss",
                                onClick = { onDismiss() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
                    ) {
                        Text("CLOSE PANEL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCardItem(
    title: String,
    desc: String,
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBg: Color,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Slate900
                        )
                        Text(
                            text = time,
                            fontSize = 10.sp,
                            color = Slate500
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = desc,
                fontSize = 11.sp,
                color = Slate700,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.clickable { onClick() },
                shape = RoundedCornerShape(8.dp),
                color = iconBg
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = iconColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
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
                label = "Transactions",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
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
                            text = "All BetPro payment gateways and deposit processors are fully operational.",
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
            OutlinedButton(
                onClick = { showSqlDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BPGreenDark)
            ) {
                Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SQL Setup Script", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
