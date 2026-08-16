package com.bp.wallet.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bp.wallet.model.*
import com.bp.wallet.ui.components.*
import com.bp.wallet.ui.theme.*
import com.bp.wallet.ui.viewmodel.BPWalletViewModel
import com.bp.wallet.ui.viewmodel.ScreenType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(viewModel: BPWalletViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val context = LocalContext.current

    val initialLetter = remember(currentUser) {
        val name = currentUser?.fullName?.ifBlank { currentUser?.username } ?: "B"
        name.take(1).uppercase()
    }

    val usernameHandle = remember(currentUser) {
        val u = currentUser?.username?.ifBlank { "bptraders_pkr" } ?: "bptraders_pkr"
        if (u.startsWith("@")) u else "@$u"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Avatar circle with initial letter
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initialLetter,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentUser?.fullName?.ifBlank { "User" } ?: "User",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                                // PKR FIXED badge
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFE8F5E9),
                                    border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                                ) {
                                    Text(
                                        text = "${currentUser?.currency ?: "PKR"} FIXED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = usernameHandle,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showSnack("No new notifications") }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.Black
                        )
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            UserBottomNav(
                currentScreen = ScreenType.USER_HOME,
                onNavigate = { viewModel.setScreen(it) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Side by side → Green DEPOSIT (PKR) (+) and Dark WITHDRAW (PKR) (−)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.setScreen(ScreenType.USER_DEPOSIT) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DEPOSIT (${currentUser?.currency ?: "PKR"})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                Button(
                    onClick = { viewModel.setScreen(ScreenType.USER_WITHDRAW) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WITHDRAW (${currentUser?.currency ?: "PKR"})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }

            // Exchange ID Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "BetPro Account ID",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, Color(0xFF81C784))
                        ) {
                            Text(
                                text = currentUser?.betproIdStatus?.uppercase() ?: "ACTIVE ID",
                                color = Color(0xFF2E7D32),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Credentials Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("BetPro Username", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentUser?.betproUsername?.ifBlank { "Pending Assignment" } ?: "Pending Assignment",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black,
                                    fontSize = 15.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("BetPro Password", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentUser?.betproPassword?.ifBlank { "••••••••" } ?: "••••••••",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    // Yellow Info Tip Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Tip: Use the above BetPro credentials to log in on the BetPro Exchange portal.",
                                fontSize = 12.sp,
                                color = Color(0xFF92400E),
                                lineHeight = 16.sp
                            )
                        }
                    }


                }
            }

            // Instructions Section: Urdu text box + English translation below
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📋 Instructions / ہدایات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )

                    // Urdu text box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "براہ کرم ڈیپوزٹ اور ودڈرا سے پہلے اپنی بیٹ پرو آئی ڈی اور پاسورڈ کی تصدیق کر لیں۔ کسی بھی مسئلے کی صورت میں ہیلپ لائن سے رابطہ کریں۔",
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Right,
                            lineHeight = 20.sp
                        )
                    }

                    // English translation below
                    Text(
                        text = "Please confirm your BetPro ID and password before making any deposit or withdrawal request. In case of any issue, contact support.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 17.sp
                    )
                }
            }

            // Broadcast announcement banner if available
            if (appSettings.announcementMessage.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFF2563EB))
                        Text(
                            text = appSettings.announcementMessage,
                            fontSize = 12.sp,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDepositScreen(viewModel: BPWalletViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val gateways by viewModel.paymentGateways.collectAsState()
    val selectedGateway by viewModel.selectedDepositGateway.collectAsState()
    val context = LocalContext.current

    var amountText by remember { mutableStateOf("") }
    var referenceText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val userGateways = remember(gateways, currentUser) {
        gateways.filter { it.isEnabled && (it.country == currentUser?.country || it.country == "Global" || it.currency == currentUser?.currency) }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deposit Funds", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.USER_HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            UserBottomNav(
                currentScreen = ScreenType.USER_DEPOSIT,
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
                Text(
                    text = "1. Select Payment Method",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Slate900
                )
            }

            if (userGateways.isEmpty()) {
                item {
                    Text("No payment methods available for your region.", color = Slate500)
                }
            } else {
                items(userGateways) { gw ->
                    val isSelected = selectedGateway?.id == gw.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectDepositGateway(gw) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) BPGreenPrimary.copy(alpha = 0.08f) else Color.White
                        ),
                        border = BorderStroke(1.5.dp, if (isSelected) BPGreenPrimary else Slate200)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(gw.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate900)
                                if (gw.bankName.isNotBlank()) Text("Bank: ${gw.bankName}", fontSize = 13.sp, color = Slate600)
                                if (gw.title.isNotBlank()) Text("Title: ${gw.title}", fontSize = 13.sp, color = Slate600)
                                if (gw.accountNumber.isNotBlank()) Text("A/C: ${gw.accountNumber}", fontSize = 13.sp, color = Slate800, fontWeight = FontWeight.Medium)
                                if (gw.iban.isNotBlank()) Text("IBAN: ${gw.iban}", fontSize = 12.sp, color = Slate500)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = BPGreenPrimary)
                            }
                        }
                    }
                }
            }

            selectedGateway?.let { gw ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate100)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Payment Instructions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = gw.instructions.ifBlank { "Transfer the exact amount to the account details above and upload your receipt screenshot below." },
                                fontSize = 13.sp,
                                color = Slate700
                            )
                            if (gw.accountNumber.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Account Number", gw.accountNumber))
                                        viewModel.showSnack("Account Number Copied!")
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Account Number", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "2. Enter Deposit Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Slate900
                    )
                }

                item {
                    GlassTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = "Deposit Amount (${currentUser?.currency ?: "PKR"})",
                        placeholder = "Min: ${gw.minDeposit}",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                item {
                    GlassTextField(
                        value = referenceText,
                        onValueChange = { referenceText = it },
                        label = "Transaction ID / Reference / Sender Name",
                        placeholder = "e.g. TRX12345678"
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedImageUri != null) "Screenshot Attached ✓" else "Upload Payment Screenshot")
                    }
                }

                item {
                    GlassButton(
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt == null || amt <= 0) {
                                viewModel.showSnack("Please enter a valid amount")
                                return@GlassButton
                            }
                            if (amt < gw.minDeposit) {
                                viewModel.showSnack("Minimum deposit is ${gw.minDeposit} ${currentUser?.currency}")
                                return@GlassButton
                            }
                            viewModel.submitDepositRequest(
                                amount = amt,
                                reference = referenceText.ifBlank { "PROOF_UPLOADED" },
                                screenshotUri = selectedImageUri?.toString() ?: ""
                            )
                        },
                        text = "Submit Deposit Request",
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserWithdrawScreen(viewModel: BPWalletViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val gateways by viewModel.paymentGateways.collectAsState()

    var amountText by remember { mutableStateOf("") }
    var accountTitle by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var selectedMethodName by remember { mutableStateOf("") }

    val userGateways = remember(gateways, currentUser) {
        gateways.filter { it.isEnabled && (it.country == currentUser?.country || it.country == "Global" || it.currency == currentUser?.currency) }
    }

    LaunchedEffect(userGateways) {
        if (userGateways.isNotEmpty() && selectedMethodName.isBlank()) {
            selectedMethodName = userGateways.first().name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Withdraw Funds", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.USER_HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            UserBottomNav(
                currentScreen = ScreenType.USER_WITHDRAW,
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
            // Balance card info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Available for Withdrawal", color = Slate400, fontSize = 13.sp)
                            Text(
                                text = "${CountryUtils.getCurrencySymbol(currentUser?.currency ?: "PKR")} ${String.format("%.2f", currentUser?.walletBalance ?: 0.0)}",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Text("Select Withdrawal Bank / Gateway", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            items(userGateways) { gw ->
                val isSelected = selectedMethodName == gw.name
                Surface(
                    onClick = { selectedMethodName = gw.name },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) BPGreenPrimary.copy(alpha = 0.08f) else Color.White,
                    border = BorderStroke(1.5.dp, if (isSelected) BPGreenPrimary else Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(gw.name, fontWeight = FontWeight.SemiBold, color = Slate900)
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BPGreenPrimary)
                        }
                    }
                }
            }

            item {
                Text("Enter Withdrawal Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            item {
                GlassTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Withdrawal Amount (${currentUser?.currency ?: "PKR"})",
                    placeholder = "Min: 1000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            item {
                GlassTextField(
                    value = accountTitle,
                    onValueChange = { accountTitle = it },
                    label = "Account Title / Beneficiary Name",
                    placeholder = "e.g. John Doe"
                )
            }

            item {
                GlassTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = "Account Number / IBAN",
                    placeholder = "Enter account number or IBAN"
                )
            }

            item {
                GlassButton(
                    onClick = {
                        val amt = amountText.toDoubleOrNull()
                        if (amt == null || amt <= 0) {
                            viewModel.showSnack("Please enter a valid amount")
                            return@GlassButton
                        }
                        if (amt > (currentUser?.walletBalance ?: 0.0)) {
                            viewModel.showSnack("Insufficient wallet balance")
                            return@GlassButton
                        }
                        if (accountTitle.isBlank() || accountNumber.isBlank()) {
                            viewModel.showSnack("Please enter account title and number")
                            return@GlassButton
                        }
                        viewModel.submitWithdrawalRequest(
                            amount = amt,
                            gatewayName = selectedMethodName.ifBlank { "Bank Transfer" },
                            accountTitle = accountTitle,
                            accountNumberOrIban = accountNumber
                        )
                        amountText = ""
                        accountTitle = ""
                        accountNumber = ""
                    },
                    text = "Submit Withdrawal Request",
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHistoryScreen(viewModel: BPWalletViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    var filterType by remember { mutableStateOf("ALL") }

    val userTxs = remember(transactions, currentUser, filterType) {
        transactions.filter { it.userId == currentUser?.id }
            .filter { if (filterType == "ALL") true else it.type.equals(filterType, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.USER_HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            UserBottomNav(
                currentScreen = ScreenType.USER_HISTORY,
                onNavigate = { viewModel.setScreen(it) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            BPTabSwitcher(
                tabs = listOf("All", "Deposits", "Withdrawals"),
                selectedIndex = when (filterType) {
                    "DEPOSIT" -> 1
                    "WITHDRAW" -> 2
                    else -> 0
                },
                onTabSelected = { idx ->
                    filterType = when (idx) {
                        1 -> "DEPOSIT"
                        2 -> "WITHDRAW"
                        else -> "ALL"
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (userTxs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions found", color = Slate500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(userTxs) { tx ->
                        TransactionItemCard(tx = tx)
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(viewModel: BPWalletViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    var editName by remember { mutableStateOf(currentUser?.fullName ?: "") }
    var editPhone by remember { mutableStateOf(currentUser?.mobileNumber ?: "") }
    var newPassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile & Security", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.USER_HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            UserBottomNav(
                currentScreen = ScreenType.USER_PROFILE,
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(BPGreenPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = BPGreenPrimary, modifier = Modifier.size(40.dp))
                        }
                        Text(currentUser?.fullName ?: "User", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Slate900)
                        Text(currentUser?.email ?: "", fontSize = 13.sp, color = Slate500)
                    }
                }
            }

            item {
                Text("Edit Personal Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            item {
                GlassTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = "Full Name"
                )
            }

            item {
                GlassTextField(
                    value = editPhone,
                    onValueChange = { editPhone = it },
                    label = "Mobile Number"
                )
            }

            item {
                Button(
                    onClick = { viewModel.updateUserProfile(editName, editPhone) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
                ) {
                    Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                }
            }

            item {
                Text("Change Password", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            item {
                GlassPasswordTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "New Password"
                )
            }

            item {
                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            viewModel.showSnack("Password must be at least 6 characters")
                            return@Button
                        }
                        viewModel.updateUserPassword(newPassword)
                        newPassword = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800)
                ) {
                    Text("Update Password", fontWeight = FontWeight.Bold)
                }
            }

            item {
                OutlinedButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500),
                    border = BorderStroke(1.dp, Red500)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(tx: TransactionRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (tx.type == "DEPOSIT") BPGreenPrimary.copy(alpha = 0.12f)
                            else Blue500.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tx.type == "DEPOSIT") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (tx.type == "DEPOSIT") BPGreenDark else Blue500,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = if (tx.type == "DEPOSIT") "Deposit via ${tx.gatewayName}" else "Withdrawal to ${tx.accountTitle.ifBlank { "Bank" }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate900
                    )
                    Text(
                        text = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(tx.timestamp)),
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (tx.type == "DEPOSIT") "+" else "-"} ${tx.currency} ${String.format("%.2f", tx.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (tx.type == "DEPOSIT") BPGreenDark else Slate900
                )
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
    }
}

@Composable
fun UserBottomNav(
    currentScreen: ScreenType,
    onNavigate: (ScreenType) -> Unit
) {
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color(0xFF00C853),
        unselectedIconColor = Color(0xFF666666),
        selectedTextColor = Color(0xFF00C853),
        unselectedTextColor = Color(0xFF666666),
        indicatorColor = Color(0xFF00C853).copy(alpha = 0.12f)
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 10.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == ScreenType.USER_HOME,
            onClick = { onNavigate(ScreenType.USER_HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = navItemColors
        )
        NavigationBarItem(
            selected = currentScreen == ScreenType.USER_DEPOSIT,
            onClick = { onNavigate(ScreenType.USER_DEPOSIT) },
            icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = "Deposit") },
            label = { Text("Deposit", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = navItemColors
        )

        // BetPro icon: Globe inside SOLID GREEN circular button (elevated/center)
        NavigationBarItem(
            selected = currentScreen == ScreenType.USER_BETPRO,
            onClick = { onNavigate(ScreenType.USER_BETPRO) },
            icon = {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(4.dp, CircleShape, spotColor = Color(0xFF00C853))
                        .clip(CircleShape)
                        .background(Color(0xFF00C853)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "BetPro",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            label = {
                Text(
                    text = "BetPro",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF00C853)
                )
            },
            colors = navItemColors
        )

        NavigationBarItem(
            selected = currentScreen == ScreenType.USER_WITHDRAW,
            onClick = { onNavigate(ScreenType.USER_WITHDRAW) },
            icon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Withdraw") },
            label = { Text("Withdraw", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = navItemColors
        )
        NavigationBarItem(
            selected = currentScreen == ScreenType.USER_HISTORY,
            onClick = { onNavigate(ScreenType.USER_HISTORY) },
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = navItemColors
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserBetProScreen(viewModel: BPWalletViewModel) {
    val appSettings by viewModel.appSettings.collectAsState()
    val exchangeUrl = remember(appSettings) {
        appSettings.exchangeWebsiteUrl.ifBlank { "https://betproexch.com" }
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoadingWeb by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = BPGreenDark
                        )
                        Text(
                            text = "BetPro Exchange",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate900
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen(ScreenType.USER_HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            UserBottomNav(
                currentScreen = ScreenType.USER_BETPRO,
                onNavigate = { viewModel.setScreen(it) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Enable cookies and session persistence
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            allowFileAccess = true
                            allowContentAccess = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            javaScriptCanOpenWindowsAutomatically = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoadingWeb = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoadingWeb = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                // Keep all navigation within the in-app WebView, never launch external browser
                                return false
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                // Gracefully handle renderer process crashes in headless/virtual environments
                                return true
                            }
                        }

                        webChromeClient = WebChromeClient()
                        loadUrl(exchangeUrl)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoadingWeb) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = BPGreenPrimary
                )
            }
        }
    }
}
