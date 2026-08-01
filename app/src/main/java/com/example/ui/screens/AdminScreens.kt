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
import com.example.model.TransactionRequest
import com.example.model.UserAccount
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AdminTopBar(
                title = "ADMIN • DASHBOARD",
                subtitle = "Executive Overview",
                currentUser = currentUser,
                onLogout = { viewModel.logout() }
            )
        },
        bottomBar = {
            AdminBottomNavBar(
                currentScreen = ScreenType.ADMIN_DASHBOARD,
                onNavigate = { viewModel.setScreen(it) }
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
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (currentUser?.isSuperAdmin == true || currentUser == null) BPGreenLight else Color(0xFFFFF3E0)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ACTIVE ROLE: ${currentUser?.role?.uppercase() ?: "SUPER ADMIN"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (currentUser?.isSuperAdmin == true || currentUser == null) BPGreenDark else Color(0xFFE65100)
                        )
                        Text(
                            text = if (currentUser?.isSuperAdmin == true || currentUser == null) {
                                "Unrestricted access to all countries, modules & final withdrawal approvals"
                            } else {
                                "Country Scope: ${currentUser?.country} (${currentUser?.currency}) • Can verify/approve deposits"
                            },
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    }
                    StatusBadge(
                        status = if (currentUser?.isSuperAdmin == true || currentUser == null) "All Countries" else "${currentUser?.country}"
                    )
                }
            }

            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminStatCard(
                    title = "USERS",
                    value = "${countryUsers.size}",
                    subtitle = "↗ +12.4% vs last week",
                    icon = Icons.Default.Group,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "USER CIRCULATION",
                    value = "PKR ${totalBalanceSum.toInt()}",
                    subtitle = "Across ${countryUsers.size} Active Wallets",
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminStatCard(
                    title = "NET PLATFORM P&L",
                    value = "PKR ${totalBalanceSum.toInt()}",
                    subtitle = "✔ Net Positive Reserve",
                    icon = Icons.Default.AttachMoney,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "MASTER AGENTS",
                    value = "${masters.size} Masters",
                    subtitle = "PKR • AED • SAR Regions",
                    icon = Icons.Default.Bolt,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminStatCard(
                    title = "PENDING DEPOSITS",
                    value = "$pendingDeposits",
                    subtitle = "Awaiting Verification",
                    icon = Icons.Default.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "PENDING PAYOUTS",
                    value = "$pendingPayouts",
                    subtitle = "Payout Approvals",
                    icon = Icons.Default.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
            }

            // Real-Time Push Broadcast Tool
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

            // Recent Financial Ledger link
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setScreen(ScreenType.ADMIN_TRANSACTIONS) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = BPGreenDark)
                    }
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BPGreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Slate900
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = BPGreenDark
            )
        }
    }
}

@Composable
fun AdminTopBar(
    title: String,
    subtitle: String,
    currentUser: UserAccount? = null,
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (currentUser?.isSuperAdmin == true || currentUser == null) BPGreenLight else Color(0xFFFFF3E0))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val badgeCode = when {
                            currentUser?.isSuperAdmin == true || currentUser == null -> "ALL"
                            else -> currentUser.currency
                        }
                        val badgeRole = when {
                            currentUser?.isSuperAdmin == true || currentUser == null -> "SUPER ADMIN"
                            currentUser.isCountrySuperMaster -> "SUPER MASTER"
                            currentUser.isSupportStaff -> "SUPPORT"
                            else -> "READ ONLY"
                        }
                        Text(
                            text = badgeCode,
                            color = if (currentUser?.isSuperAdmin == true || currentUser == null) BPGreenDark else Color(0xFFE65100),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeRole,
                            color = Slate700,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

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
