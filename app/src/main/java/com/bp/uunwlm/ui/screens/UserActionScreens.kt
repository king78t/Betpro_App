package com.bp.uunwlm.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.*
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
import com.bp.uunwlm.model.PaymentGateway
import com.bp.uunwlm.model.TransactionRequest
import com.bp.uunwlm.model.UserAccount
import com.bp.uunwlm.ui.components.ShimmerDepositSkeleton
import com.bp.uunwlm.ui.components.ShimmerWithdrawSkeleton
import com.bp.uunwlm.ui.components.StatusBadge
import com.bp.uunwlm.ui.components.WhatsAppHelplineButton
import com.bp.uunwlm.ui.theme.*
import com.bp.uunwlm.ui.viewmodel.BPWalletViewModel
import com.bp.uunwlm.ui.viewmodel.ScreenType
import com.airbnb.lottie.compose.*
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
    val isLoading by viewModel.isLoading.collectAsState()

    val u = user ?: return
    val currencyGateways = gateways.filter { it.currency.equals(u.currency, true) }.ifEmpty { gateways }

    var amountText by remember { mutableStateOf("") }
    var senderAccount by remember { mutableStateOf(u.mobileNumber) }
    var referenceText by remember { mutableStateOf("") }
    var screenshotUri by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            screenshotUri = uri.toString()
        }
    }

    BetProWebViewDialogHandler(viewModel = viewModel)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UserTopBar(
                user = u,
                onLogout = { viewModel.logout() },
                onProfileClick = { viewModel.setScreen(ScreenType.USER_PROFILE) }
            )
        },
        bottomBar = {
            UserBottomNavBar(
                currentScreen = ScreenType.USER_DEPOSIT,
                onNavigate = { viewModel.setScreen(it) },
                onBpIdClick = { viewModel.setBetProExchangeModalVisible(true) }
            )
        }
    ) { innerPadding ->
        if (isLoading && gateways.isEmpty()) {
            ShimmerDepositSkeleton(modifier = Modifier.padding(innerPadding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLoading || isSubmitting) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = BPGreenPrimary,
                        trackColor = BPGreenLight
                    )
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
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(BPGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = gw.name,
                                        tint = BPGreenDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = gw.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Slate900,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        StatusBadge(status = gw.currency)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Title: ${gw.title}",
                                        fontSize = 12.sp,
                                        color = Slate700,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Min Deposit: ${u.currency} ${gw.minDeposit.toInt()}",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.selectDepositGateway(gw) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
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
                        text = "Deposit via ${gw.name}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    IconButton(onClick = { viewModel.selectDepositGateway(null) }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate900)
                    }
                }

                // Selected Gateway Card (Pay To)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDFF9EC)) // Light green matching image
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PAY TO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BPGreenDark,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = gw.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = gw.accountNumber,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            Button(
                                onClick = {
                                    viewModel.showSnack("Copied: ${gw.accountNumber}")
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = BPGreenDark
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BPGreenPrimary.copy(alpha = 0.3f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Amount (${u.currency})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = { Text("Min ${gw.minDeposit.toInt()}") },
                        prefix = { Text("Rs ", fontWeight = FontWeight.Bold, color = Slate900) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFDFF9EC).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFFDFF9EC).copy(alpha = 0.5f),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    // Amount Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("+1k" to 1000, "+5k" to 5000, "+10k" to 10000, "+25k" to 25000, "+50k" to 50000).forEach { (label, value) ->
                            OutlinedButton(
                                onClick = {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    amountText = (current + value).toInt().toString()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BPGreenDark),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                            ) {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sender Name (Optional)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    OutlinedTextField(
                        value = senderAccount,
                        onValueChange = { senderAccount = it },
                        leadingIcon = { Icon(imageVector = Icons.Default.PersonOutline, contentDescription = null, tint = Slate500) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFDFF9EC).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFFDFF9EC).copy(alpha = 0.5f),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Transaction Reference (Optional)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    OutlinedTextField(
                        value = referenceText,
                        onValueChange = { referenceText = it },
                        leadingIcon = { Icon(imageVector = Icons.Default.Tag, contentDescription = null, tint = Slate500) },
                        placeholder = { Text("e.g. JC-882301 or Bank TID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFDFF9EC).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFFDFF9EC).copy(alpha = 0.5f),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }

                // Screenshot Image Attachment (Mandatory)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (screenshotUri.isNotBlank()) BPGreenLight else Color(0xFFFEECEB) // Light red if missing? Or just grey.
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (screenshotUri.isNotBlank()) BPGreenPrimary else Color(0xFFFFCDD2)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (screenshotUri.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = screenshotUri,
                                    contentDescription = "Preview",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Screenshot",
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (screenshotUri.isNotBlank()) "Screenshot Attached" else "Attach Payment Screenshot (Required)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (screenshotUri.isNotBlank()) Slate900 else Color(0xFFD32F2F)
                                )
                                Text(
                                    text = if (screenshotUri.isNotBlank()) "Tap to replace proof" else "Screenshot is mandatory for approval",
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
                        if (amt < gw.minDeposit) {
                            viewModel.showSnack("Minimum deposit is ${u.currency} ${gw.minDeposit.toInt()}")
                            return@Button
                        }
                        if (screenshotUri.isBlank()) {
                            viewModel.showSnack("Please attach payment screenshot proof!")
                            return@Button
                        }

                        isSubmitting = true
                        viewModel.submitDepositRequest(
                            amount = amt,
                            reference = referenceText.ifBlank { "TRX-${System.currentTimeMillis()}" },
                            screenshotUri = screenshotUri
                        )
                        isSubmitting = false
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00B16A), // Emerald green matching image
                        contentColor = Color.White
                    )
                ) {
                    if (isSubmitting) {
                        val loadingComp by rememberLottieComposition(LottieCompositionSpec.Url("https://assets9.lottiefiles.com/packages/lf20_p8bfn5to.json"))
                        LottieAnimation(
                            composition = loadingComp,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit Deposit in ${u.currency}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
}

@Composable
fun UserWithdrawScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val gateways by viewModel.paymentGateways.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val u = user ?: return

    var selectedGatewayName by remember { mutableStateOf("") }
    var accountTitle by remember { mutableStateOf(u.fullName) }
    var accountNumber by remember { mutableStateOf(u.mobileNumber) }
    var amountText by remember { mutableStateOf("1000") }
    var isSubmitting by remember { mutableStateOf(false) }

    BetProWebViewDialogHandler(viewModel = viewModel)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UserTopBar(
                user = u,
                onLogout = { viewModel.logout() },
                onProfileClick = { viewModel.setScreen(ScreenType.USER_PROFILE) }
            )
        },
        bottomBar = {
            UserBottomNavBar(
                currentScreen = ScreenType.USER_WITHDRAW,
                onNavigate = { viewModel.setScreen(it) },
                onBpIdClick = { viewModel.setBetProExchangeModalVisible(true) }
            )
        }
    ) { innerPadding ->
        if (isLoading && gateways.isEmpty()) {
            ShimmerWithdrawSkeleton(modifier = Modifier.padding(innerPadding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLoading || isSubmitting) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = BPGreenPrimary,
                        trackColor = BPGreenLight
                    )
                }

                // Request Payout Header + min badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.USER_HOME) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

            // Custom Bank / Wallet Name input field
            OutlinedTextField(
                value = selectedGatewayName,
                onValueChange = { selectedGatewayName = it },
                label = { Text("Bank / Wallet Name (e.g., Bank Name, Wallet Provider)") },
                placeholder = { Text("Enter account or wallet provider name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BPGreenPrimary,
                    unfocusedBorderColor = Slate500
                )
            )

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
                label = { Text("Account Number / IBAN / Phone Number") },
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
                    isSubmitting = true
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    val gwName = if (selectedGatewayName.isNotBlank()) selectedGatewayName else "Bank/Wallet Transfer"
                    viewModel.submitWithdrawalRequest(amt, gwName, accountTitle, accountNumber)
                    isSubmitting = false
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Slate900,
                    contentColor = Color.White
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Submit", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isSubmitting) "PROCESSING WITHDRAWAL..." else "SUBMIT WITHDRAWAL IN ${u.currency}",
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

    BetProWebViewDialogHandler(viewModel = viewModel)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UserTopBar(
                user = u,
                onLogout = { viewModel.logout() },
                onProfileClick = { viewModel.setScreen(ScreenType.USER_PROFILE) }
            )
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
                    items(filteredTxs, key = { it.id }) { tx ->
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

    var editableFullName by remember(u.fullName) { mutableStateOf(u.fullName) }
    var editableMobileNumber by remember(u.mobileNumber) { mutableStateOf(u.mobileNumber) }
    var editableCountry by remember(u.country) { mutableStateOf(u.country) }
    var newPassword by remember { mutableStateOf("") }
    val whatsappNumber by viewModel.whatsappHelplineNumber.collectAsState()
    val biometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    BetProWebViewDialogHandler(viewModel = viewModel)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setScreen(ScreenType.USER_HOME) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Slate50)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate900,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
            }
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
            // User Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BPGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Column {
                        Text(
                            text = u.fullName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = u.email,
                            fontSize = 13.sp,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(BPGreenLight)
                                .border(1.dp, BPGreenPrimary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = BPGreenDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${u.id} · ${u.currency}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BPGreenDark
                            )
                        }
                    }
                }
            }

            // Personal Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Personal details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    ProfileInputField(label = "Full name", value = editableFullName, onValueChange = { editableFullName = it })
                    ProfileInputField(label = "Mobile", value = editableMobileNumber, onValueChange = { editableMobileNumber = it })
                    ProfileInputField(label = "Country", value = editableCountry, onValueChange = { editableCountry = it })
                    ProfileInputField(label = "Currency (locked)", value = u.currency, onValueChange = {}, readOnly = true)

                    Button(
                        onClick = {
                            viewModel.updateUserProfile(editableFullName, editableMobileNumber)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
                    ) {
                        Text(text = "Save changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            // Change Password Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = BPGreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Change password",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }

                    ProfileInputField(label = "", value = newPassword, onValueChange = { newPassword = it }, placeholder = "New password")

                    OutlinedButton(
                        onClick = {
                            if (newPassword.isNotBlank()) {
                                viewModel.updateUserPassword(newPassword)
                                newPassword = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate900)
                    ) {
                        Text(text = "Update password", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
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
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = BPGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Biometric Lock",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Fingerprint or Face Unlock",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BPGreenPrimary,
                                uncheckedThumbColor = Slate200,
                                uncheckedTrackColor = Slate50
                            )
                        )
                    }
                }
            }

            // Support Card
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Support",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "24/7 Support · Member since ${formatFullDate(u.createdAt)}",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.openWhatsAppSupport(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Chat with support on WhatsApp", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            // Logout Button
            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE5E5)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Logout", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ProfileInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    readOnly: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate700
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { if (placeholder.isNotBlank()) Text(placeholder) },
            readOnly = readOnly,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Slate100,
                focusedBorderColor = BPGreenPrimary,
                unfocusedContainerColor = Slate50,
                focusedContainerColor = Slate50,
                unfocusedTextColor = if (readOnly) Slate500 else Slate900,
                focusedTextColor = Slate900
            )
        )
    }
}

fun formatFullDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
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
