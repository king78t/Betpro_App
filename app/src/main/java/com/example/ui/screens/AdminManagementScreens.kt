package com.example.ui.screens

import androidx.compose.foundation.background
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
fun AdminScopeHeaderCard(currentUser: UserAccount?) {
    val isSuperAdmin = currentUser?.isSuperAdmin == true || currentUser == null
    val roleTitle = currentUser?.role ?: "Super Admin"
    val scopeText = if (isSuperAdmin) {
        "Unrestricted access to all countries, modules, and final withdrawal approvals."
    } else {
        "Assigned Country: ${currentUser?.country ?: "Pakistan"} (${currentUser?.currency ?: "PKR"}) • Can verify & approve deposits."
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSuperAdmin) BPGreenLight else Color(0xFFFFF3E0)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ACTIVE ROLE: ${roleTitle.uppercase()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = if (isSuperAdmin) BPGreenDark else Color(0xFFE65100)
                )
                Text(
                    text = scopeText,
                    fontSize = 11.sp,
                    color = Slate700,
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(status = if (isSuperAdmin) "Global" else "${currentUser?.country ?: "PK"}")
        }
    }
}

@Composable
fun AdminUsersCrmScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val search by viewModel.crmSearchQuery.collectAsState()
    val currFilter by viewModel.crmCurrencyFilter.collectAsState()
    val statusFilter by viewModel.crmStatusFilter.collectAsState()
    val selectedUserForCreds by viewModel.selectedUserForCreds.collectAsState()

    val normalUsers = users.filter { !it.isSuperAdmin && !it.isCountrySuperMaster && !it.isSupportStaff && !it.isReadOnlyUser && it.role != "admin" }
    val countryScopedUsers = if (currentUser?.isSuperAdmin == true || currentUser == null) {
        normalUsers
    } else {
        normalUsers.filter { u -> u.country.equals(currentUser?.country, true) || u.currency.equals(currentUser?.currency, true) }
    }
    val filteredUsers = countryScopedUsers.filter { u ->
        val matchesSearch = search.isEmpty() || u.fullName.contains(search, true) ||
                u.email.contains(search, true) || u.mobileNumber.contains(search, true) ||
                u.betproUsername.contains(search, true)
        val matchesCurr = currFilter == "All Curr" || u.currency.equals(currFilter, true)
        val matchesStatus = statusFilter == "All Status" ||
                (statusFilter == "Active" && u.betproIdStatus == "Active") ||
                (statusFilter == "Pending" && u.betproIdStatus == "Pending") ||
                (statusFilter == "Blocked" && u.betproIdStatus == "Blocked")
        matchesSearch && matchesCurr && matchesStatus
    }

    var showDrawer by remember { mutableStateOf(false) }

    if (selectedUserForCreds != null) {
        SetBetProCredsDialog(
            user = selectedUserForCreds!!,
            onDismiss = { viewModel.closeBetProCredsModal() },
            onSave = { username, pass, status ->
                viewModel.saveBetProCredentials(selectedUserForCreds!!.id, username, pass, status)
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AdminTopBar(
                    title = "ADMIN / USERS",
                    subtitle = "User Management CRM",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AdminScopeHeaderCard(currentUser = currentUser)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "User Management CRM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "Manage user accounts, assign BetPro credentials, toggle access",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
                StatusBadge(status = "${countryScopedUsers.size} Users")
            }

            // Search input
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.setCrmSearchQuery(it) },
                placeholder = { Text("Search user, phone, BP ID, master...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Slate500) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BPGreenPrimary,
                    unfocusedBorderColor = Slate500
                )
            )

            // Currency Pill Filter: All Curr | PKR | AED | SAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All Curr", "PKR", "AED", "SAR").forEach { c ->
                    val sel = currFilter == c
                    FilterChip(
                        selected = sel,
                        onClick = { viewModel.setCrmCurrencyFilter(c) },
                        label = { Text(c, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BPGreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Status Pill Filter: All Status | Active | Pending | Blocked
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All Status", "Active", "Pending", "Blocked").forEach { st ->
                    val sel = statusFilter == st
                    FilterChip(
                        selected = sel,
                        onClick = { viewModel.setCrmStatusFilter(st) },
                        label = { Text(st, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate700,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Users list
            if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No users found matching filters", color = Slate500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredUsers) { u ->
                        CrmUserCard(
                            user = u,
                            onAssignCreds = { viewModel.openBetProCredsModal(u) }
                        )
                    }
                }
            }
        }
        }

        AdminEnterpriseDrawer(
            showDrawer = showDrawer,
            onDismiss = { showDrawer = false },
            currentScreen = ScreenType.ADMIN_USERS_CRM,
            onNavigate = { viewModel.setScreen(it) },
            currentUser = currentUser,
            onLogout = { viewModel.logout() }
        )
    }
}

@Composable
fun CrmUserCard(
    user: UserAccount,
    onAssignCreds: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top row: Avatar + Name + Email + Currency badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BPGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.fullName.take(1).uppercase(),
                            color = BPGreenDark,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = user.fullName, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Slate900)
                        Text(text = user.email, fontSize = 11.sp, color = Slate500)
                    }
                }
                StatusBadge(status = user.currency)
            }

            HorizontalDivider(color = Slate100)

            // Middle row: Assigned Master & BetPro Credentials badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "MASTER AGENT", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                    Text(text = user.masterAgentName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { onAssignCreds() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = "ID", tint = BPGreenDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = user.betproUsername, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                            Text(text = "Pass: ${user.betproPassword}", fontSize = 10.sp, color = Slate500)
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate100)

            // Bottom Row: Wallet Balance, Status pill, Actions (Assign Creds button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "WALLET BALANCE", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${user.currency} ${user.walletBalance.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = BPGreenDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        status = if (user.betproIdStatus == "Active") "Active ID" else "Pending ID"
                    )

                    Button(
                        onClick = onAssignCreds,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BPGreenPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = "Set", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Assign ID", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

    }
}

@Composable
fun SetBetProCredsDialog(
    user: UserAccount,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var username by remember { mutableStateOf(if (user.betproUsername == "Available Soon") "bpexch_${user.fullName.take(3).lowercase()}101" else user.betproUsername) }
    var password by remember { mutableStateOf(if (user.betproPassword == "Wait for Admin") "pass4078" else user.betproPassword) }
    var status by remember { mutableStateOf("Active") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set BetPro Exchange Credentials",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Assign official BetPro credentials for ${user.fullName}",
                    fontSize = 13.sp,
                    color = Slate500
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("BetPro Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("BetPro Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Text(text = "ID Status", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Active", "Pending", "Blocked").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(username, password, status) },
                colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
            ) {
                Text("Save Credentials & Notify User", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun AdminMasterAgentsScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val masters by viewModel.masterAgents.collectAsState()
    val showModal by viewModel.showCreateMasterModal.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val countryScopedMasters = if (currentUser?.isSuperAdmin == true || currentUser == null) {
        masters
    } else {
        masters.filter { m -> m.country.equals(currentUser?.country, true) || m.currency.equals(currentUser?.currency, true) }
    }

    var showDrawer by remember { mutableStateOf(false) }

    if (showModal) {
        CreateMasterDialog(
            currentUser = currentUser,
            onDismiss = { viewModel.closeCreateMasterModal() },
            onCreate = { name, pass, curr, role, limit, share ->
                viewModel.createMasterAgent(name, pass, curr, role, limit, share)
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AdminTopBar(
                    title = "ADMIN / MASTERS",
                    subtitle = "Master Agents & Partners",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AdminScopeHeaderCard(currentUser = currentUser)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Master Agents & Network Hierarchy",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "Click to view complete user lists, balance details",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }

                Button(
                    onClick = { viewModel.openCreateMasterModal() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
                ) {
                    Text("+ Create Master Agent", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Stats badge
            Card(
                colors = CardDefaults.cardColors(containerColor = BPGreenLight),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "ACTIVE MASTER ACCOUNTS", fontWeight = FontWeight.Bold, color = BPGreenDark, fontSize = 12.sp)
                    Text(text = "${countryScopedMasters.size}", fontWeight = FontWeight.Black, color = Slate900, fontSize = 18.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(countryScopedMasters) { agent ->
                    MasterAgentCard(agent = agent)
                }
            }
        }
        }

        AdminEnterpriseDrawer(
            showDrawer = showDrawer,
            onDismiss = { showDrawer = false },
            currentScreen = ScreenType.ADMIN_MASTER_AGENTS,
            onNavigate = { viewModel.setScreen(it) },
            currentUser = currentUser,
            onLogout = { viewModel.logout() }
        )
    }
}

@Composable
fun MasterAgentCard(agent: MasterAgent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BPGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = agent.role,
                        tint = BPGreenDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = agent.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Slate900)
                    Text(text = "Role: ${agent.role}", fontSize = 12.sp, color = Slate500)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(status = agent.currency)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Share: ${agent.marginShare.toInt()}%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BPGreenDark)
            }
        }
    }
}

@Composable
fun CreateMasterDialog(
    currentUser: UserAccount? = null,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("master123") }
    var curr by remember { mutableStateOf(if (currentUser?.isSuperAdmin == false) currentUser.currency else "PKR") }
    var role by remember { mutableStateOf("Master Agent") }
    var creditLimit by remember { mutableStateOf("200000") }
    var share by remember { mutableStateOf("80") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Master Agent", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Agent / Agency Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Assign Master Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currencies = if (currentUser?.isSuperAdmin == false) listOf(currentUser.currency) else listOf("PKR", "AED", "SAR")
                    currencies.forEach { c ->
                        FilterChip(
                            selected = curr == c,
                            onClick = { curr = c },
                            label = { Text(c, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Super Master", "Master Agent", "Sub Agent").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limitNum = creditLimit.toDoubleOrNull() ?: 200000.0
                    val shareNum = share.toDoubleOrNull() ?: 80.0
                    onCreate(name, password, curr, role, limitNum, shareNum)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
            ) {
                Text("Create Master ($curr)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color.White
    )
}

@Composable
fun AdminTransactionsScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val allTxs by viewModel.allTransactions.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val countryFilteredTxs = if (currentUser?.isSuperAdmin == true || currentUser == null) {
        allTxs
    } else {
        allTxs.filter { tx ->
            tx.country.equals(currentUser?.country, true) || tx.currency.equals(currentUser?.currency, true)
        }
    }

    var showDrawer by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AdminTopBar(
                    title = "ADMIN / TRANSACTIONS",
                    subtitle = "Pending Deposits & Withdrawals",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AdminScopeHeaderCard(currentUser = currentUser)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Financial Ledger",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900
                )
                val pendingCnt = countryFilteredTxs.count { it.status == "Pending" }
                StatusBadge(status = "$pendingCnt Pending")
            }

            if (countryFilteredTxs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions recorded yet.", color = Slate500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(countryFilteredTxs) { tx ->
                        AdminTxCard(
                            tx = tx,
                            currentUser = currentUser,
                            onApprove = { viewModel.approveTransaction(tx.id) },
                            onReject = { viewModel.rejectTransaction(tx.id) }
                        )
                    }
                }
            }
        }
        }

        AdminEnterpriseDrawer(
            showDrawer = showDrawer,
            onDismiss = { showDrawer = false },
            currentScreen = ScreenType.ADMIN_TRANSACTIONS,
            onNavigate = { viewModel.setScreen(it) },
            currentUser = currentUser,
            onLogout = { viewModel.logout() }
        )
    }
}

@Composable
fun AdminTxCard(
    tx: TransactionRequest,
    currentUser: UserAccount? = null,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val canApprove = if (tx.type == "WITHDRAW") {
        currentUser?.isSuperAdmin == true || currentUser == null
    } else {
        currentUser?.isSuperAdmin == true || currentUser?.isCountrySuperMaster == true || currentUser == null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = tx.type)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rs ${tx.amount.toInt()} (${tx.currency})",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Slate900
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "User: ${tx.userName} (${tx.userEmail})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                Text(text = "Gateway: ${tx.gatewayName} | Acc: ${tx.accountNumber}", fontSize = 11.sp, color = Slate500)
                Text(text = "Ref/Screenshot: ${tx.referenceNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BPGreenDark)
                if (tx.screenshotUri.isNotBlank()) {
                    Text(
                        text = "📎 Screenshot Proof Attached",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BPGreenPrimary
                    )
                }
            }

            if (tx.status == "Pending") {
                if (canApprove) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onApprove,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BPGreenPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Approve", tint = Color.White)
                        }
                        IconButton(
                            onClick = onReject,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD32F2F))
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Reject", tint = Color.White)
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Awaiting Super Admin",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color(0xFFE65100),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                StatusBadge(status = tx.status)
            }
        }
    }
}
