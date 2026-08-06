package com.bp.uunwlm.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bp.uunwlm.model.PaymentGateway
import com.bp.uunwlm.model.TransactionRequest
import com.bp.uunwlm.model.UserAccount
import com.bp.uunwlm.ui.components.ShimmerDashboardSkeleton
import com.bp.uunwlm.ui.components.StatusBadge
import com.bp.uunwlm.ui.components.WhatsAppHelplineButton
import com.bp.uunwlm.ui.theme.*
import com.bp.uunwlm.ui.viewmodel.BPWalletViewModel
import com.bp.uunwlm.ui.viewmodel.ScreenType
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
    val whatsappNumber by viewModel.whatsappHelplineNumber.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val u = user ?: return
    val userTxs = allTxs.filter { it.userId == u.id }
    val approvedDepositsCount = userTxs.count { it.type == "DEPOSIT" && it.status == "Approved" }
    val approvedWithdrawalsCount = userTxs.count { it.type == "WITHDRAW" && it.status == "Approved" }
    val totalWithdrawalSum = userTxs.filter { it.type == "WITHDRAW" && it.status == "Approved" }.sumOf { it.amount }

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
                currentScreen = ScreenType.USER_HOME,
                onNavigate = { viewModel.setScreen(it) },
                onBpIdClick = { viewModel.setBetProExchangeModalVisible(true) }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            ShimmerDashboardSkeleton(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Action Buttons: Deposit & Withdrawal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // DEPOSIT BUTTON
                Button(
                    onClick = { viewModel.setScreen(ScreenType.USER_DEPOSIT) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BPGreenPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Deposit",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ DEPOSIT (${u.currency})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // WITHDRAWAL BUTTON
                Button(
                    onClick = { viewModel.setScreen(ScreenType.USER_WITHDRAW) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate900,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircle,
                        contentDescription = "Withdraw",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WITHDRAW (${u.currency})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Centered Open BetPro Button on Dashboard Card
                    Button(
                        onClick = { viewModel.setBetProExchangeModalVisible(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BPGreenPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "BetPro Exchange",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OPEN BETPRO EXCHANGE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
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
                    viewModel.showSnack("Connecting to Official BP Wallet WhatsApp Support at $whatsappNumber...")
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun UserTopBar(
    user: UserAccount,
    onLogout: () -> Unit,
    onProfileClick: (() -> Unit)? = null
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onProfileClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onProfileClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                } else {
                    Modifier
                }
            ) {
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

                // Profile button
                if (onProfileClick != null) {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = BPGreenDark
                        )
                    }
                }

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
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home (Left 1)
            NavBarItem(
                label = "Home",
                icon = Icons.Default.Home,
                selected = currentScreen == ScreenType.USER_HOME,
                onClick = { onNavigate(ScreenType.USER_HOME) },
                modifier = Modifier.weight(1f)
            )

            // Deposit (Left 2)
            NavBarItem(
                label = "Deposit",
                icon = Icons.Default.AddCircleOutline,
                selected = currentScreen == ScreenType.USER_DEPOSIT,
                onClick = { onNavigate(ScreenType.USER_DEPOSIT) },
                modifier = Modifier.weight(1f)
            )

            // BetPro Center Button (EXACT CENTER - 3rd out of 5 equal weight items)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .offset(y = (-10).dp)
                    .clickable { onBpIdClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BPGreenPrimary)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "BetPro",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "BetPro",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
            }

            // Withdraw (Right 1)
            NavBarItem(
                label = "Withdraw",
                icon = Icons.Default.RemoveCircleOutline,
                selected = currentScreen == ScreenType.USER_WITHDRAW,
                onClick = { onNavigate(ScreenType.USER_WITHDRAW) },
                modifier = Modifier.weight(1f)
            )

            // History (Right 2)
            NavBarItem(
                label = "History",
                icon = Icons.Default.History,
                selected = currentScreen == ScreenType.USER_HISTORY,
                onClick = { onNavigate(ScreenType.USER_HISTORY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun NavBarItem(
 label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 4.dp)
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
fun BetProWebViewDialogHandler(viewModel: BPWalletViewModel) {
    val showBetProModal by viewModel.showBetProExchangeModal.collectAsState()
    val exchangeUrl by viewModel.exchangeWebsiteUrl.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val u = user ?: return
    if (showBetProModal) {
        BetProExchangeModal(
            user = u,
            url = exchangeUrl,
            onDismiss = { viewModel.setBetProExchangeModalVisible(false) },
            onCopy = { txt -> viewModel.showSnack("Copied: $txt") }
        )
    }
}


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BetProExchangeModal(
    user: UserAccount,
    url: String = "https://bpexch.live",
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit = {}
) {
    val targetUrl = remember(url) {
        val clean = url.trim()
        if (clean.startsWith("http://") || clean.startsWith("https://")) clean else "https://$clean"
    }
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
        }
    }

    Dialog(
        onDismissRequest = {
            if (webViewRef?.canGoBack() == true) {
                webViewRef?.goBack()
            } else {
                CookieManager.getInstance().flush()
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Slate900
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top Header Bar (Video-like experience: title + close/back button)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Slate900,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    if (webViewRef?.canGoBack() == true) {
                                        webViewRef?.goBack()
                                    } else {
                                        CookieManager.getInstance().flush()
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.testTag("webview_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "BetPro Official",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = targetUrl.removePrefix("https://").removePrefix("http://"),
                                    fontSize = 11.sp,
                                    color = BPGreenLight,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    isLoading = true
                                    webViewRef?.reload()
                                },
                                modifier = Modifier.testTag("webview_refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Compact Credentials Bar with Auto-Fill button inside WebView
                Surface(
                    color = Slate800,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "ID",
                                tint = BPGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ID: ${user.betproUsername} | Pass: ${user.betproPassword}",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = {
                                val script = """
                                    (function() {
                                        var u = "${user.betproUsername}";
                                        var p = "${user.betproPassword}";
                                        function setV(el, val) {
                                            if (!el) return false;
                                            el.focus();
                                            el.value = val;
                                            el.dispatchEvent(new Event('input', { bubbles: true }));
                                            el.dispatchEvent(new Event('change', { bubbles: true }));
                                            el.blur();
                                            return true;
                                        }
                                        var inputs = document.querySelectorAll('input');
                                        var uEl = null;
                                        var pEl = null;
                                        for (var i = 0; i < inputs.length; i++) {
                                            var inp = inputs[i];
                                            var t = (inp.type || '').toLowerCase();
                                            var n = (inp.name || '').toLowerCase();
                                            var id = (inp.id || '').toLowerCase();
                                            var ph = (inp.placeholder || '').toLowerCase();
                                            if (t === 'password') {
                                                pEl = inp;
                                            } else if (!uEl && (t === 'text' || t === 'email' || t === 'number' || t === 'tel' || t === '' || n.includes('user') || n.includes('login') || n.includes('id') || id.includes('user') || id.includes('login') || ph.includes('user') || ph.includes('id'))) {
                                                uEl = inp;
                                            }
                                        }
                                        if (!uEl && inputs.length > 0) uEl = inputs[0];
                                        if (!pEl && inputs.length > 1) pEl = inputs[1];
                                        setV(uEl, u);
                                        setV(pEl, p);
                                        setTimeout(function() {
                                            var btns = document.querySelectorAll('button, input[type="submit"], a, div');
                                            for (var j = 0; j < btns.length; j++) {
                                                var btn = btns[j];
                                                var txt = (btn.innerText || btn.value || '').toLowerCase();
                                                if (txt.includes('login') || txt.includes('sign in') || txt.includes('submit') || txt.includes('log in') || (btn.type && btn.type.toLowerCase() === 'submit')) {
                                                    btn.click();
                                                    break;
                                                }
                                            }
                                        }, 300);
                                    })();
                                """.trimIndent()
                                webViewRef?.evaluateJavascript(script, null)
                                onCopy("Credentials Auto-Filled!")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BPGold,
                                contentColor = Slate900
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("webview_autofill_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Auto Fill",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AUTO-FILL", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // Loading Indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = BPGreenPrimary,
                        trackColor = Slate800
                    )
                }

                // In-App WebView
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    setSupportZoom(true)
                                    builtInZoomControls = false
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        CookieManager.getInstance().flush()
                                    }
                                }
                                webChromeClient = WebChromeClient()
                                loadUrl(targetUrl)
                                webViewRef = this
                            }
                        },
                        update = { webView ->
                            if (webView.url.isNullOrEmpty() && webView.originalUrl.isNullOrEmpty()) {
                                webView.loadUrl(targetUrl)
                            }
                        },
                        onRelease = { webView ->
                            webView.stopLoading()
                            webView.removeAllViews()
                            webView.destroy()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
