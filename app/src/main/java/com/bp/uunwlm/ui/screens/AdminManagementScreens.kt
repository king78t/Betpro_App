package com.bp.uunwlm.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.bp.uunwlm.model.*
import com.bp.uunwlm.ui.theme.*
import com.bp.uunwlm.ui.components.StatusBadge
import com.bp.uunwlm.ui.viewmodel.BPWalletViewModel
import com.bp.uunwlm.ui.viewmodel.ScreenType
import com.bp.uunwlm.util.DateTimeUtils

@Composable
fun AdminManagementLayout(
    title: String,
    viewModel: BPWalletViewModel,
    content: @Composable (PaddingValues) -> Unit
) {
    var showDrawer by remember { mutableStateOf(false) }
    val currentUser by viewModel.currentUser.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                AdminTopBarRedesigned(
                    title = title,
                    subtitle = "Enterprise Management",
                    onOpenDrawer = { showDrawer = true },
                    onLogout = { viewModel.logout() }
                )
            }
        ) { innerPadding ->
            content(innerPadding)
        }

        AdminEnterpriseDrawer(
            showDrawer = showDrawer,
            onDismiss = { showDrawer = false },
            currentScreen = ScreenType.ADMIN_USERS_CRM, // Dynamic later
            onNavigate = { viewModel.setScreen(it) },
            currentUser = currentUser,
            onLogout = { viewModel.logout() }
        )
    }
}

// 1. REDESIGNED USER MANAGEMENT CRM
@Composable
fun AdminUserManagementScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.allUsers.collectAsState()
    val search by viewModel.crmSearchQuery.collectAsState()
    val currFilter by viewModel.crmCurrencyFilter.collectAsState()
    val statusFilter by viewModel.crmStatusFilter.collectAsState()
    val selectedUserForCreds by viewModel.selectedUserForCreds.collectAsState()

    val normalUsers = users.filter { !it.isSuperAdmin && it.role != "admin" }
    
    val filteredUsers = normalUsers.filter { u ->
        val matchesSearch = search.isBlank() ||
                u.fullName.contains(search, true) ||
                u.email.contains(search, true) ||
                u.mobileNumber.contains(search, true) ||
                u.betproUsername.contains(search, true)
        val matchesCurr = currFilter == "All Curr" || u.currency.equals(currFilter, true)
        val matchesStatus = statusFilter == "All Status" || u.betproIdStatus == statusFilter
        matchesSearch && matchesCurr && matchesStatus
    }

    if (selectedUserForCreds != null) {
        SetBetProCredsDialog(
            user = selectedUserForCreds!!,
            onDismiss = { viewModel.closeBetProCredsModal() },
            onConfirm = { username, pass, status ->
                viewModel.saveBetProCredentials(selectedUserForCreds!!.id, username, pass, status)
            }
        )
    }

    AdminManagementLayout(title = "User Management", viewModel = viewModel) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search & Filter Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { viewModel.setCrmSearchQuery(it) },
                        placeholder = { Text("Search users...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = BPGreenPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BPGreenPrimary)
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterPill("All Curr", currFilter == "All Curr") { viewModel.setCrmCurrencyFilter("All Curr") }
                        FilterPill("PKR", currFilter == "PKR") { viewModel.setCrmCurrencyFilter("PKR") }
                        FilterPill("AED", currFilter == "AED") { viewModel.setCrmCurrencyFilter("AED") }
                    }
                }
            }

            // User List
            if (filteredUsers.isEmpty()) {
                EmptyState(message = "No users found matching filters")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filteredUsers, key = { it.id }) { user ->
                        RedesignedUserCard(
                            user = user,
                            onEdit = { viewModel.openBetProCredsModal(user) },
                            onDelete = { viewModel.deleteUser(user.id) },
                            onToggleStatus = { 
                                val newStatus = if (user.betproIdStatus == "Active") "Suspended" else "Active"
                                viewModel.updateUserStatus(user.id, newStatus)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RedesignedUserCard(
    user: UserAccount,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BPGreenPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.fullName.take(1).uppercase(), fontWeight = FontWeight.Black, color = BPGreenPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.fullName, fontWeight = FontWeight.Bold, color = Slate900)
                    Text(user.email, fontSize = 12.sp, color = Slate500)
                }
                StatusBadge(status = user.betproIdStatus)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Balance", fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.Bold)
                    Text("${user.currency} ${user.walletBalance.toInt()}", fontWeight = FontWeight.Black, color = BPGreenPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Joined", fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.Bold)
                    Text(DateTimeUtils.formatDate(user.createdAt), fontSize = 12.sp, color = Slate700)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate700)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manage ID", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onToggleStatus) {
                    Icon(
                        imageVector = if (user.betproIdStatus == "Active") Icons.Default.Block else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (user.betproIdStatus == "Active") Color.Red else BPGreenPrimary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Slate400)
                }
            }
        }
    }
}

// 2. PROFESSIONAL DEPOSIT SCREEN
@Composable
fun AdminDepositsScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val txs by viewModel.allTransactions.collectAsState()
    val deposits = txs.filter { it.type == "DEPOSIT" }.sortedByDescending { it.timestamp }
    val proofTx by viewModel.txForProofVerification.collectAsState()

    if (proofTx != null) {
        ProofVerificationDialog(
            transaction = proofTx!!,
            onDismiss = { viewModel.closeProofVerification() },
            onApprove = { viewModel.approveTransaction(proofTx!!.id); viewModel.closeProofVerification() },
            onReject = { viewModel.rejectTransaction(proofTx!!.id); viewModel.closeProofVerification() }
        )
    }

    AdminManagementLayout(title = "Deposit Requests", viewModel = viewModel) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(deposits, key = { it.id }) { tx ->
                AdminTransactionItem(
                    tx = tx,
                    onApprove = { viewModel.openProofVerification(tx) },
                    onReject = { viewModel.rejectTransaction(tx.id) }
                )
            }
        }
    }
}

// 3. PROFESSIONAL WITHDRAWAL SCREEN
@Composable
fun AdminWithdrawalsScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val txs by viewModel.allTransactions.collectAsState()
    val withdrawals = txs.filter { it.type == "WITHDRAW" }.sortedByDescending { it.timestamp }

    AdminManagementLayout(title = "Withdrawal Requests", viewModel = viewModel) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(withdrawals, key = { it.id }) { tx ->
                AdminTransactionItem(
                    tx = tx,
                    onApprove = { viewModel.approveTransaction(tx.id) },
                    onReject = { viewModel.rejectTransaction(tx.id) }
                )
            }
        }
    }
}

@Composable
fun AdminTransactionItem(
    tx: TransactionRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(tx.userName, fontWeight = FontWeight.Bold, color = Slate900)
                    Text(DateTimeUtils.formatDate(tx.timestamp), fontSize = 11.sp, color = Slate500)
                }
                StatusBadge(status = tx.status)
            }
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${tx.currency} ${tx.amount.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = BPGreenPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("via ${tx.gatewayName}", fontSize = 12.sp, color = Slate500)
            }

            if (tx.status == "Pending") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (tx.type == "DEPOSIT") "Verify Proof" else "Approve", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(0.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reject", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 4. BANK ACCOUNT MANAGEMENT
@Composable
fun AdminBankManagementScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val gateways by viewModel.paymentGateways.collectAsState()

    AdminManagementLayout(title = "Bank Accounts", viewModel = viewModel) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(gateways, key = { it.id }) { gw ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(BPGreenPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalance, null, tint = BPGreenPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(gw.name, fontWeight = FontWeight.Bold, color = Slate900)
                            Text(gw.accountNumber, fontSize = 12.sp, color = Slate500)
                        }
                        Switch(
                            checked = gw.isEnabled,
                            onCheckedChange = { viewModel.togglePaymentGatewayStatus(gw.id) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BPGreenPrimary)
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { /* Add New Gateway Modal */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Bank Account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 5. AUDIT LOGS SCREEN
@Composable
fun AdminAuditLogsScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.auditLogs.collectAsState()

    AdminManagementLayout(title = "Security Audit Logs", viewModel = viewModel) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs) { log ->
                ActivityItem(log) // Reusing the one from AdminScreens.kt
            }
        }
    }
}

@Composable
fun AdminNotificationsScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Alert") }
    val notifications by viewModel.adminNotifications.collectAsState()

    AdminManagementLayout(title = "Notification Center", viewModel = viewModel) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Broadcast New Notification", fontWeight = FontWeight.Black, color = Slate900)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3
                    )
                    
                    Text("Type", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate600)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterPill("Alert", type == "Alert") { type = "Alert" }
                        FilterPill("Update", type == "Update") { type = "Update" }
                        FilterPill("Promotion", type == "Promotion") { type = "Promotion" }
                    }

                    Button(
                        onClick = { 
                            viewModel.sendAdminNotification(title, message, type)
                            title = ""; message = ""
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
                    ) {
                        Text("Send Notification", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("Sent History", fontWeight = FontWeight.Bold, color = Slate900)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(notifications) { n ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(n.title, fontWeight = FontWeight.Bold, color = Slate900)
                                StatusBadge(status = n.type)
                            }
                            Text(n.message, fontSize = 12.sp, color = Slate600)
                            Text(DateTimeUtils.formatDate(n.timestamp), fontSize = 10.sp, color = Slate400, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        }
                    }
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
    val settings by viewModel.appSettings.collectAsState()
    var appName by remember { mutableStateOf(settings.appName) }
    var whatsapp by remember { mutableStateOf(settings.supportWhatsapp) }
    var email by remember { mutableStateOf(settings.supportEmail) }
    val context = LocalContext.current

    LaunchedEffect(settings) {
        appName = settings.appName
        whatsapp = settings.supportWhatsapp
        email = settings.supportEmail
    }

    AdminManagementLayout(title = "App Settings", viewModel = viewModel) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("General Configuration", fontWeight = FontWeight.Black, color = Slate900)
                    
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("Application Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("Support WhatsApp Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Support Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { 
                            viewModel.saveAppSettings(settings.copy(appName = appName, supportWhatsapp = whatsapp, supportEmail = email))
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
                    ) {
                        Text("Save Configurations", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Support Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Need Help?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Slate900
                    )
                    Text(
                        text = "If you encounter any issues or have questions about the admin panel, please contact our technical support team.",
                        fontSize = 13.sp,
                        color = Slate600
                    )
                    
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/${whatsapp.replace("+", "").replace(" ", "")}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Contact Technical Support", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) BPGreenPrimary else Color.Transparent,
        border = if (selected) null else BorderStroke(1.dp, Slate200)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else Slate600
        )
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Slate400, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SetBetProCredsDialog(
    user: UserAccount,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var betproUsername by remember { mutableStateOf(user.betproUsername) }
    var betproPassword by remember { mutableStateOf(user.betproPassword) }
    var status by remember { mutableStateOf(user.betproIdStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set BetPro Credentials") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = betproUsername,
                    onValueChange = { betproUsername = it },
                    label = { Text("BetPro Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = betproPassword,
                    onValueChange = { betproPassword = it },
                    label = { Text("BetPro Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Account Status", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill("Pending", status == "Pending") { status = "Pending" }
                    FilterPill("Active", status == "Active") { status = "Active" }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(betproUsername, betproPassword, status) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ProofVerificationDialog(
    transaction: TransactionRequest,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Verify Proof of Payment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Slate900
                )
                
                if (transaction.screenshotUri.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Slate100, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No Proof Image Provided", color = Slate500)
                    }
                } else {
                    AsyncImage(
                        model = transaction.screenshotUri,
                        contentDescription = "Payment Proof",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Transaction Details:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Amount: ${transaction.amount} ${transaction.currency}", fontSize = 13.sp)
                    Text("Method: ${transaction.gatewayName}", fontSize = 13.sp)
                    Text("User ID: ${transaction.userId}", fontSize = 13.sp)
                    Text("Status: ${transaction.status}", fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reject")
                    }
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Approve")
                    }
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close", color = Slate500)
                }
            }
        }
    }
}
