package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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
fun UserDepositScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val gateways by viewModel.paymentGateways.collectAsState()
    val selectedGw by viewModel.selectedDepositGateway.collectAsState()

    val u = user ?: return
    val currencyGateways = gateways.filter { it.currency.equals(u.currency, true) }.ifEmpty { gateways }

    var amountText by remember { mutableStateOf("") }
    var senderAccount by remember { mutableStateOf(u.mobileNumber) }
    var referenceText by remember { mutableStateOf("") }
    var screenshotUri by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            screenshotUri = uri.toString()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UserTopBar(user = u, onLogout = { viewModel.logout() })
        },
        bottomBar = {
            UserBottomNavBar(
                currentScreen = ScreenType.USER_DEPOSIT,
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
            // Currency badge bar
            Card(
                colors = CardDefaults.cardColors(containerColor = BPGreenLight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = BPGreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Deposit Currency: ${u.currency} (${currencyName(u.currency)})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = BPGreenDark
                        )
                    }
                    StatusBadge(status = "LOCKED")
                }
            }

            val gw = selectedGw
            if (gw == null) {
                // STEP 1 OF 2: SELECT GATEWAY
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Select ${u.currency} Gateway",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "Step 1 of 2",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BPGreenDark
                    )
                }

                Text(
                    text = "Choose your preferred payment gateway for ${currencyName(u.currency)}:",
                    fontSize = 13.sp,
                    color = Slate500
                )

                currencyGateways.forEach { gw ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectDepositGateway(gw) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
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
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(BPGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = gw.name,
                                        tint = BPGreenDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = gw.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = Slate900
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        StatusBadge(status = gw.currency)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Title: ${gw.title}",
                                        fontSize = 12.sp,
                                        color = Slate700
                                    )
                                    Text(
                                        text = "Min Deposit: Rs ${gw.minDeposit.toInt()}",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.selectDepositGateway(gw) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BPGreenPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "Select",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // STEP 2 OF 2: SUBMIT AMOUNT AND REFERENCE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Enter Deposit Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "Step 2 of 2",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BPGreenDark
                    )
                }

                // Selected Gateway Card with Copy Button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BPGreenLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected Gateway: ${gw.name}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = BPGreenDark
                            )
                            TextButton(onClick = { viewModel.selectDepositGateway(null) }) {
                                Text("Change", color = Slate700, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Account Title: ${gw.title}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Account Number: ${gw.accountNumber}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            IconButton(onClick = {
                                viewModel.showSnack("Copied Account Number: ${gw.accountNumber}")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Slate700
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Deposit Amount (${u.currency})") },
                    placeholder = { Text("Min ${gw.minDeposit.toInt()} e.g. 1500") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BPGreenPrimary,
                        unfocusedBorderColor = Slate500
                    )
                )

                OutlinedTextField(
                    value = senderAccount,
                    onValueChange = { senderAccount = it },
                    label = { Text("Your Sender Account / Mobile Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BPGreenPrimary,
                        unfocusedBorderColor = Slate500
                    )
                )

                OutlinedTextField(
                    value = referenceText,
                    onValueChange = { referenceText = it },
                    label = { Text("Transaction Reference / Screenshot ID") },
                    placeholder = { Text("e.g. EPX-9988776655") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BPGreenPrimary,
                        unfocusedBorderColor = Slate500
                    )
                )

                // Screenshot Image Attachment Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (screenshotUri.isNotBlank()) BPGreenLight else Slate100
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (screenshotUri.isNotBlank()) BPGreenPrimary else Slate300
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (screenshotUri.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.AddPhotoAlternate,
                                contentDescription = "Screenshot",
                                tint = if (screenshotUri.isNotBlank()) BPGreenPrimary else Slate700
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (screenshotUri.isNotBlank()) "Screenshot Attached" else "Upload Payment Screenshot (Optional)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = if (screenshotUri.isNotBlank()) "Tap to replace screenshot proof" else "Tap to select image from gallery",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }
                        if (screenshotUri.isNotBlank()) {
                            IconButton(onClick = { screenshotUri = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        viewModel.submitDepositRequest(
                            amount = amt,
                            reference = referenceText.ifBlank { "TRX-${System.currentTimeMillis()}" },
                            screenshotUri = screenshotUri
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BPGreenPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "SUBMIT DEPOSIT REQUEST",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // WhatsApp Helpline Button
            WhatsAppHelplineButton(
                onClick = {
                    viewModel.showSnack("Connecting to Official BP Wallet WhatsApp Support @bptraders_pkr...")
                }
            )
        }
    }
}

@Composable
fun UserWithdrawScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val gateways by viewModel.paymentGateways.collectAsState()
    val u = user ?: return

    var selectedGatewayName by remember { mutableStateOf("EasyPaisa (PKR)") }
    var accountTitle by remember { mutableStateOf(u.fullName) }
    var accountNumber by remember { mutableStateOf(u.mobileNumber) }
    var amountText by remember { mutableStateOf("1000") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UserTopBar(user = u, onLogout = { viewModel.logout() })
        },
        bottomBar = {
            UserBottomNavBar(
                currentScreen = ScreenType.USER_WITHDRAW,
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
            // Payout Currency header
            Card(
                colors = CardDefaults.cardColors(containerColor = BPGreenLight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Payout",
                            tint = BPGreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Payout Currency: ${u.currency} (${currencyName(u.currency)})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = BPGreenDark
                        )
                    }
                    StatusBadge(status = "LOCKED")
                }
            }

            // Request Payout Header + min badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.USER_HOME) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Request ${u.currency} Payout",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFE5E5))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Min Rs 1000",
                        color = Color(0xFFD32F2F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Gateway selector pill row
            Column {
                Text(
                    text = "Select Payout Gateway",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val gwOptions = listOf("EasyPaisa", "JazzCash", "Meezan Bank")
                    gwOptions.forEach { opt ->
                        val selected = selectedGatewayName.contains(opt)
                        FilterChip(
                            selected = selected,
                            onClick = { selectedGatewayName = "$opt (${u.currency})" },
                            label = { Text(opt, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BPGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = accountTitle,
                onValueChange = { accountTitle = it },
                label = { Text("Account Title (Receiver Name)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BPGreenPrimary,
                    unfocusedBorderColor = Slate500
                )
            )

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text("Account Number / IBAN") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BPGreenPrimary,
                    unfocusedBorderColor = Slate500
                )
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Withdrawal Amount (${u.currency})") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BPGreenPrimary,
                    unfocusedBorderColor = Slate500
                )
            )

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    viewModel.submitWithdrawalRequest(amt, selectedGatewayName, accountTitle, accountNumber)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Slate900,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Submit", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SUBMIT WITHDRAWAL IN ${u.currency}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // WhatsApp Helpline Button
            WhatsAppHelplineButton(
                onClick = {
                    viewModel.showSnack("Connecting to Official BP Wallet WhatsApp Support @bptraders_pkr...")
                }
            )
        }
    }
}

@Composable
fun UserHistoryScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val allTxs by viewModel.allTransactions.collectAsState()
    val u = user ?: return
    val userTxs = allTxs.filter { it.userId == u.id }

    var selectedTab by remember { mutableStateOf("ALL") } // "ALL", "DEPOSIT", "WITHDRAW"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UserTopBar(user = u, onLogout = { viewModel.logout() })
        },
        bottomBar = {
            UserBottomNavBar(
                currentScreen = ScreenType.USER_HISTORY,
                onNavigate = { viewModel.setScreen(it) },
                onBpIdClick = { viewModel.setBetProExchangeModalVisible(true) }
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
            Text(
                text = "Transaction History",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900
            )

            // Tabs: All | Deposits | Withdrawals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL" to "All", "DEPOSIT" to "Deposits", "WITHDRAW" to "Withdrawals").forEach { (key, label) ->
                    val sel = selectedTab == key
                    FilterChip(
                        selected = sel,
                        onClick = { selectedTab = key },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BPGreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            val filteredTxs = userTxs.filter {
                if (selectedTab == "ALL") true else it.type == selectedTab
            }

            if (filteredTxs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HistoryToggleOff,
                            contentDescription = "Empty",
                            tint = Slate500,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No transactions recorded yet.",
                            color = Slate500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTxs) { tx ->
                        TransactionItemCard(tx = tx)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(tx: TransactionRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (tx.type == "DEPOSIT") BPGreenLight else Color(0xFFFFF3E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tx.type == "DEPOSIT") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = tx.type,
                        tint = if (tx.type == "DEPOSIT") BPGreenDark else BPGoldDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${tx.type}: ${tx.gatewayName}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Slate900
                    )
                    Text(
                        text = "Ref: ${tx.referenceNumber}",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                    Text(
                        text = formatTime(tx.timestamp),
                        fontSize = 10.sp,
                        color = Slate500
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (tx.type == "DEPOSIT") "+" else "-"} Rs ${tx.amount.toInt()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = if (tx.type == "DEPOSIT") BPGreenDark else Slate900
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = tx.status)
            }
        }
    }
}

@Composable
fun UserProfileScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val u = user ?: return

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UserTopBar(user = u, onLogout = { viewModel.logout() })
        },
        bottomBar = {
            UserBottomNavBar(
                currentScreen = ScreenType.USER_PROFILE,
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BPGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = u.fullName.take(1).uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BPGreenDark
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = u.fullName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = u.email,
                        fontSize = 13.sp,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusBadge(status = "${u.currency} VERIFIED WALLET")
                }
            }

            // Info rows
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProfileRowItem(label = "Mobile Number", valText = u.mobileNumber)
                    HorizontalDivider(color = Slate100)
                    ProfileRowItem(label = "Account Currency", valText = "${u.currency} (${currencyName(u.currency)})")
                    HorizontalDivider(color = Slate100)
                    ProfileRowItem(label = "Assigned Master Agent", valText = u.masterAgentName)
                    HorizontalDivider(color = Slate100)
                    ProfileRowItem(label = "BetPro ID Status", valText = u.betproIdStatus)
                }
            }

            // Change Password Card
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
                            contentDescription = "Change Password",
                            tint = BPGreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Change Account Password",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Slate900
                            )
                            Text(
                                text = "Update your wallet login password securely",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            if (newPassword != confirmPassword) {
                                viewModel.showSnack("Passwords do not match!")
                            } else {
                                viewModel.updateUserPassword(newPassword)
                                newPassword = ""
                                confirmPassword = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
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

            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = "Logout")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Log Out of BP Wallet", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileRowItem(label: String, valText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Slate500,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = valText,
            fontSize = 13.sp,
            color = Slate900,
            fontWeight = FontWeight.Bold
        )
    }
}

fun currencyName(curr: String): String = when (curr) {
    "PKR" -> "Pakistani Rupee"
    "AED" -> "UAE Dirham"
    "SAR" -> "Saudi Riyal"
    else -> curr
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
