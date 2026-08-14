package com.bp.wallet.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bp.wallet.model.*
import com.bp.wallet.ui.components.*
import com.bp.wallet.ui.theme.*
import com.bp.wallet.ui.viewmodel.BPWalletViewModel
import com.bp.wallet.ui.viewmodel.ScreenType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: BPWalletViewModel) {
    val users by viewModel.usersList.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    val pendingDeposits = remember(transactions) {
        transactions.filter { it.type == "DEPOSIT" && it.status == "Pending" }
    }
    val pendingWithdrawals = remember(transactions) {
        transactions.filter { it.type == "WITHDRAW" && it.status == "Pending" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BPWalletLogo(size = 32.dp)
                        Text("Admin Control Hub", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Red500)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            AdminBottomNav(
                currentScreen = ScreenType.ADMIN_DASHBOARD,
                onNavigate = { viewModel.setScreen(it) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminMetricCard(
                        title = "Pending Deposits",
                        value = "${pendingDeposits.size}",
                        icon = Icons.Default.ArrowDownward,
                        color = BPGreenPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setScreen(ScreenType.ADMIN_DEPOSITS) }
                    )
                    AdminMetricCard(
                        title = "Pending Withdrawals",
                        value = "${pendingWithdrawals.size}",
                        icon = Icons.Default.ArrowUpward,
                        color = Amber500,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setScreen(ScreenType.ADMIN_WITHDRAWALS) }
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminMetricCard(
                        title = "Total Users",
                        value = "${users.size}",
                        icon = Icons.Default.People,
                        color = Blue500,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setScreen(ScreenType.ADMIN_USERS_CRM) }
                    )
                    AdminMetricCard(
                        title = "Payment Gateways",
                        value = "Manage",
                        icon = Icons.Default.AccountBalance,
                        color = Purple500,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setScreen(ScreenType.ADMIN_BANK_MANAGEMENT) }
                    )
                }
            }

            item {
                Text("Quick Admin Operations", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate900)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        AdminNavRow("User Management & BetPro CRM", Icons.Default.Badge) { viewModel.setScreen(ScreenType.ADMIN_USERS_CRM) }
                        HorizontalDivider(color = Slate100)
                        AdminNavRow("Deposit Approvals", Icons.Default.ArrowDownward) { viewModel.setScreen(ScreenType.ADMIN_DEPOSITS) }
                        HorizontalDivider(color = Slate100)
                        AdminNavRow("Withdrawal Approvals", Icons.Default.ArrowUpward) { viewModel.setScreen(ScreenType.ADMIN_WITHDRAWALS) }
                        HorizontalDivider(color = Slate100)
                        AdminNavRow("Bank Accounts & Gateways", Icons.Default.AccountBalance) { viewModel.setScreen(ScreenType.ADMIN_BANK_MANAGEMENT) }
                        HorizontalDivider(color = Slate100)
                        AdminNavRow("Send Push Broadcasts", Icons.Default.Notifications) { viewModel.setScreen(ScreenType.ADMIN_NOTIFICATIONS) }
                        HorizontalDivider(color = Slate100)
                        AdminNavRow("Audit Security Logs", Icons.Default.Security) { viewModel.setScreen(ScreenType.ADMIN_AUDIT_LOGS) }
                        HorizontalDivider(color = Slate100)
                        AdminNavRow("System Configuration", Icons.Default.Settings) { viewModel.setScreen(ScreenType.ADMIN_SETTINGS) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(viewModel: BPWalletViewModel) {
    val users by viewModel.usersList.collectAsState()
    val searchQuery by viewModel.crmSearchQuery.collectAsState()
    val selectedUserForCreds by viewModel.selectedUserForCreds.collectAsState()

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter { it.fullName.contains(searchQuery, true) || it.email.contains(searchQuery, true) || it.username.contains(searchQuery, true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User CRM & BetPro IDs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.ADMIN_DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            AdminBottomNav(
                currentScreen = ScreenType.ADMIN_USERS_CRM,
                onNavigate = { viewModel.setScreen(it) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassTextField(
                value = searchQuery,
                onValueChange = { viewModel.setCrmSearchQuery(it) },
                placeholder = "Search user by name, email...",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                                    Text(user.email, fontSize = 12.sp, color = Slate500)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (user.betproIdStatus == "Active") BPGreenPrimary.copy(alpha = 0.15f) else Amber500.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = user.betproIdStatus,
                                        color = if (user.betproIdStatus == "Active") BPGreenDark else Amber500,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bal: ${user.currency} ${String.format("%.2f", user.walletBalance)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Button(
                                    onClick = { viewModel.openBetProCredsModal(user) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                                ) {
                                    Text("Assign BetPro ID", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedUserForCreds?.let { user ->
        BetProCredentialsDialog(
            user = user,
            onDismiss = { viewModel.closeBetProCredsModal() },
            onSave = { u, p, s ->
                viewModel.saveBetProCredentials(user.id, u, p, s)
            }
        )
    }
}

@Composable
fun BetProCredentialsDialog(
    user: UserAccount,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var username by remember { mutableStateOf(if (user.betproUsername == "Available Soon") "" else user.betproUsername) }
    var password by remember { mutableStateOf(if (user.betproPassword == "Wait for Admin") "" else user.betproPassword) }
    var status by remember { mutableStateOf(user.betproIdStatus) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Assign BetPro Credentials", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("User: ${user.fullName}", fontSize = 13.sp, color = Slate600)

                GlassTextField(value = username, onValueChange = { username = it }, label = "BetPro Username")
                GlassTextField(value = password, onValueChange = { password = it }, label = "BetPro Password")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { status = "Active" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (status == "Active") BPGreenPrimary else Slate200),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Active", color = if (status == "Active") Color.White else Slate700)
                    }
                    Button(
                        onClick = { status = "Pending" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (status == "Pending") Amber500 else Slate200),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pending", color = if (status == "Pending") Color.White else Slate700)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(username, password, status) },
                        colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDepositsScreen(viewModel: BPWalletViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val deposits = remember(transactions) { transactions.filter { it.type == "DEPOSIT" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deposit Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.ADMIN_DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            AdminBottomNav(
                currentScreen = ScreenType.ADMIN_DEPOSITS,
                onNavigate = { viewModel.setScreen(it) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (deposits.isEmpty()) {
                item {
                    Text("No deposit requests found", color = Slate500)
                }
            } else {
                items(deposits) { tx ->
                    AdminTxCard(
                        tx = tx,
                        onApprove = { viewModel.approveTransaction(tx.id) },
                        onReject = { viewModel.rejectTransaction(tx.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWithdrawalsScreen(viewModel: BPWalletViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val withdrawals = remember(transactions) { transactions.filter { it.type == "WITHDRAW" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Withdrawal Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.ADMIN_DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            AdminBottomNav(
                currentScreen = ScreenType.ADMIN_WITHDRAWALS,
                onNavigate = { viewModel.setScreen(it) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (withdrawals.isEmpty()) {
                item {
                    Text("No withdrawal requests found", color = Slate500)
                }
            } else {
                items(withdrawals) { tx ->
                    AdminTxCard(
                        tx = tx,
                        onApprove = { viewModel.approveTransaction(tx.id) },
                        onReject = { viewModel.rejectTransaction(tx.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminTxCard(
    tx: TransactionRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(tx.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                    Text("${tx.type} • ${tx.gatewayName}", fontSize = 12.sp, color = Slate500)
                    if (tx.referenceNumber.isNotBlank()) Text("Ref: ${tx.referenceNumber}", fontSize = 11.sp, color = Slate600)
                    if (tx.accountNumber.isNotBlank()) Text("A/C: ${tx.accountNumber} (${tx.accountTitle})", fontSize = 11.sp, color = Slate600)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${tx.currency} ${String.format("%.2f", tx.amount)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (tx.status) {
                            "Approved" -> BPGreenPrimary.copy(alpha = 0.15f)
                            "Rejected" -> Red500.copy(alpha = 0.15f)
                            else -> Amber500.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = tx.status,
                            color = when (tx.status) {
                                "Approved" -> BPGreenDark
                                "Rejected" -> Red500
                                else -> Amber500
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (tx.status == "Pending") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Approve", color = Color.White)
                    }
                    Button(
                        onClick = onReject,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Red500),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reject", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBankManagementScreen(viewModel: BPWalletViewModel) {
    val gateways by viewModel.paymentGateways.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Gateways & Banks", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.ADMIN_DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = BPGreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            AdminBottomNav(
                currentScreen = ScreenType.ADMIN_BANK_MANAGEMENT,
                onNavigate = { viewModel.setScreen(it) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(gateways) { gw ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(gw.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate900)
                                Text("${gw.bankName} • ${gw.country}", fontSize = 12.sp, color = Slate500)
                                if (gw.accountNumber.isNotBlank()) Text("A/C: ${gw.accountNumber}", fontSize = 12.sp, color = Slate700)
                            }
                            Switch(
                                checked = gw.isEnabled,
                                onCheckedChange = { viewModel.togglePaymentGatewayStatus(gw.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPaymentGatewayDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, bank, title, acc, iban, cur, country ->
                viewModel.addPaymentGateway(
                    name = name,
                    bankName = bank,
                    title = title,
                    accountNumber = acc,
                    iban = iban,
                    currency = cur,
                    country = country
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddPaymentGatewayDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("PKR") }
    var country by remember { mutableStateOf("Pakistan") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Payment Method", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                GlassTextField(value = name, onValueChange = { name = it }, label = "Method Name (e.g. EasyPaisa, Bank Transfer)")
                GlassTextField(value = bankName, onValueChange = { bankName = it }, label = "Bank Name")
                GlassTextField(value = title, onValueChange = { title = it }, label = "Account Title")
                GlassTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = "Account Number")
                GlassTextField(value = iban, onValueChange = { iban = it }, label = "IBAN (Optional)")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onAdd(name, bankName, title, accountNumber, iban, currency, country) },
                        colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationsScreen(viewModel: BPWalletViewModel) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Push Alert Broadcast", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.ADMIN_DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            AdminBottomNav(
                currentScreen = ScreenType.ADMIN_NOTIFICATIONS,
                onNavigate = { viewModel.setScreen(it) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Broadcast Message to All Users", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            GlassTextField(value = title, onValueChange = { title = it }, label = "Alert Title")
            GlassTextField(value = message, onValueChange = { message = it }, label = "Alert Message", singleLine = false)

            GlassButton(
                onClick = {
                    if (title.isNotBlank() && message.isNotBlank()) {
                        viewModel.broadcastPushAlert(title, message)
                        viewModel.sendAdminNotification(title, message, "PROMO")
                        title = ""
                        message = ""
                    } else {
                        viewModel.showSnack("Please fill title and message")
                    }
                },
                text = "Send Broadcast Now",
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditLogsScreen(viewModel: BPWalletViewModel) {
    val auditLogs by viewModel.auditLogs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Audit Logs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.ADMIN_DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            AdminBottomNav(
                currentScreen = ScreenType.ADMIN_AUDIT_LOGS,
                onNavigate = { viewModel.setScreen(it) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (auditLogs.isEmpty()) {
                item { Text("No audit logs yet", color = Slate500) }
            } else {
                items(auditLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(log.action, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BPGreenDark)
                                Text(
                                    java.text.SimpleDateFormat("dd MMM, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp)),
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            }
                            Text(log.details, fontSize = 12.sp, color = Slate700)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(viewModel: BPWalletViewModel) {
    val settings by viewModel.appSettings.collectAsState()

    var whatsapp by remember { mutableStateOf(settings.whatsappHelpline) }
    var exchangeUrl by remember { mutableStateOf(settings.exchangeWebsiteUrl) }
    var announcement by remember { mutableStateOf(settings.announcementMessage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.ADMIN_DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            AdminBottomNav(
                currentScreen = ScreenType.ADMIN_SETTINGS,
                onNavigate = { viewModel.setScreen(it) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("App Configuration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            item {
                GlassTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = "WhatsApp Helpline Number")
            }

            item {
                GlassTextField(value = exchangeUrl, onValueChange = { exchangeUrl = it }, label = "BetPro Exchange Website URL")
            }

            item {
                GlassTextField(value = announcement, onValueChange = { announcement = it }, label = "Broadcast Announcement Banner", singleLine = false)
            }

            item {
                Button(
                    onClick = {
                        viewModel.saveAppSettings(
                            settings.copy(
                                whatsappHelpline = whatsapp,
                                exchangeWebsiteUrl = exchangeUrl,
                                announcementMessage = announcement
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
                ) {
                    Text("Save Settings", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Slate900)
            Text(title, fontSize = 12.sp, color = Slate500)
        }
    }
}

@Composable
fun AdminNavRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = Slate600)
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Slate800)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
    }
}

@Composable
fun AdminBottomNav(
    currentScreen: ScreenType,
    onNavigate: (ScreenType) -> Unit
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        NavigationBarItem(
            selected = currentScreen == ScreenType.ADMIN_DASHBOARD,
            onClick = { onNavigate(ScreenType.ADMIN_DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard") }
        )
        NavigationBarItem(
            selected = currentScreen == ScreenType.ADMIN_USERS_CRM,
            onClick = { onNavigate(ScreenType.ADMIN_USERS_CRM) },
            icon = { Icon(Icons.Default.People, contentDescription = "Users") },
            label = { Text("Users") }
        )
        NavigationBarItem(
            selected = currentScreen == ScreenType.ADMIN_DEPOSITS,
            onClick = { onNavigate(ScreenType.ADMIN_DEPOSITS) },
            icon = { Icon(Icons.Default.ArrowDownward, contentDescription = "Deposits") },
            label = { Text("Deposits") }
        )
        NavigationBarItem(
            selected = currentScreen == ScreenType.ADMIN_WITHDRAWALS,
            onClick = { onNavigate(ScreenType.ADMIN_WITHDRAWALS) },
            icon = { Icon(Icons.Default.ArrowUpward, contentDescription = "Withdrawals") },
            label = { Text("Withdrawals") }
        )
        NavigationBarItem(
            selected = currentScreen == ScreenType.ADMIN_SETTINGS,
            onClick = { onNavigate(ScreenType.ADMIN_SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}
