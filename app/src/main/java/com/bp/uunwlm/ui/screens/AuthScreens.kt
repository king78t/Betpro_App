package com.bp.uunwlm.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.bp.uunwlm.ui.components.BPTabSwitcher
import com.bp.uunwlm.ui.components.BPWalletLogo
import com.bp.uunwlm.ui.components.CurrencyCard
import com.bp.uunwlm.ui.components.GlassCard
import com.bp.uunwlm.ui.components.GlassButton
import com.bp.uunwlm.ui.components.GlassTextField
import com.bp.uunwlm.ui.components.FloatingBackground
import com.bp.uunwlm.ui.theme.*
import com.bp.uunwlm.ui.viewmodel.BPWalletViewModel
import com.bp.uunwlm.ui.viewmodel.ScreenType
import com.bp.uunwlm.BuildConfig
import com.bp.uunwlm.R

@Composable
fun LoginScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val isUserTab by viewModel.isUserLoginTab.collectAsState()
    
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var adminUsername by remember { mutableStateOf("Admin") }
    var adminPassword by remember { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        FloatingBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // HEADER TABS (User / Admin)
            BPTabSwitcher(
                isUserTab = isUserTab,
                onTabChange = { viewModel.setLoginTab(it) },
                modifier = Modifier
                    .width(220.dp)
                    .height(46.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // BP WALLET SHIELD LOGO WITH CIRCULAR FLOATING GLASS CONTAINER
            BPWalletLogo(sizeDp = 80, showSubtitle = true)

            Spacer(modifier = Modifier.height(28.dp))

            // Main Content Area (88% Width Floating Glass Card)
            if (isUserTab) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(bottom = 32.dp),
                    elevation = 20.dp,
                    spotColor = Color(0xFF22C55E).copy(alpha = 0.22f)
                ) {
                    Text(
                        text = "Welcome Back",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "Login to access your BP Wallet account",
                        fontSize = 14.sp,
                        color = Slate500,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // USER LOGIN FIELDS
                    GlassTextField(
                        value = emailOrPhone,
                        onValueChange = { emailOrPhone = it },
                        label = "Username or Email",
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Slate400) }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    GlassTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate400) },
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Forgot Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = "Forgot Password?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BPGreenPrimary,
                            modifier = Modifier.clickable {
                                viewModel.setScreen(ScreenType.FORGOT_PASSWORD)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Login Button (65% width, centered, green gradient)
                    GlassButton(
                        onClick = { viewModel.loginUser(emailOrPhone, password) },
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .align(Alignment.CenterHorizontally),
                        text = "Login"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // CREATE NEW ACCOUNT Link
                    TextButton(
                        onClick = { viewModel.setScreen(ScreenType.REGISTER) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Don't have an account? ",
                                fontSize = 14.sp,
                                color = Slate500
                            )
                            Text(
                                text = "Create account",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BPGreenPrimary
                            )
                        }
                    }
                }
            } else {
                // ADMIN LOGIN FIELDS WITH GOLD ACCENT HIGHLIGHTS
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(bottom = 32.dp),
                    elevation = 22.dp,
                    spotColor = BPGold.copy(alpha = 0.35f),
                    borderColor = BPGoldSoft,
                    containerColor = Color.White.copy(alpha = 0.96f)
                ) {
                    // Gold Admin Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BPGoldSoft,
                        border = BorderStroke(1.dp, BPGold.copy(alpha = 0.5f)),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = BPGoldDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ENTERPRISE ADMIN PORTAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BPGoldDark,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Admin Access",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "Secure management console",
                        fontSize = 14.sp,
                        color = Slate500,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    GlassTextField(
                        value = adminUsername,
                        onValueChange = { adminUsername = it },
                        label = "Admin Username",
                        leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = BPGoldDark) }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    GlassTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        label = "Admin Password",
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BPGoldDark) },
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    GlassButton(
                        onClick = { viewModel.loginAdmin(adminUsername, adminPassword) },
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .align(Alignment.CenterHorizontally),
                        text = "Admin Login",
                        gradientColors = listOf(BPGold, BPGoldDark),
                        glowColor = BPGold.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun VerifyEmailScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val pendingEmail by viewModel.pendingVerificationEmail.collectAsState()
    var otpDigits by remember { mutableStateOf(List(6) { "" }) }
    
    // Countdown Timer Logic
    var timeLeftSec by remember { mutableStateOf(60) }
    var isTimerActive by remember { mutableStateOf(true) }

    LaunchedEffect(isTimerActive, timeLeftSec) {
        if (isTimerActive && timeLeftSec > 0) {
            delay(1000L)
            timeLeftSec -= 1
        } else if (timeLeftSec == 0) {
            isTimerActive = false
        }
    }

    val fullOtpCode = otpDigits.joinToString("")

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        FloatingBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth(0.88f),
                elevation = 20.dp
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(BPGreenLight)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = BPGreenPrimary
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "OTP Verification",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "We sent a 6-digit verification code to:\n${pendingEmail.ifBlank { "your email" }}",
                    textAlign = TextAlign.Center,
                    color = Slate500,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(28.dp))

                // 6 OTP Digit Boxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    repeat(6) { index ->
                        val digit = otpDigits.getOrElse(index) { "" }
                        OutlinedTextField(
                            value = digit,
                            onValueChange = { input ->
                                if (input.length <= 1) {
                                    val newDigits = otpDigits.toMutableList()
                                    newDigits[index] = input
                                    otpDigits = newDigits
                                }
                            },
                            modifier = Modifier
                                .width(46.dp)
                                .height(56.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Slate900
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BPGreenPrimary,
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))

                // Countdown Timer & Resend Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTimerActive) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Resend code in ${timeLeftSec}s",
                            fontSize = 13.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        TextButton(
                            onClick = { 
                                if (pendingEmail.isNotBlank()) {
                                    viewModel.resendVerification(pendingEmail)
                                    timeLeftSec = 60
                                    isTimerActive = true
                                }
                            }
                        ) {
                            Text(
                                text = "Resend Verification Code",
                                color = BPGreenPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                // Verify Account Button
                GlassButton(
                    onClick = { 
                        if (pendingEmail.isNotBlank()) {
                            viewModel.verifyOtp(pendingEmail, fullOtpCode)
                        } else {
                            viewModel.showSnack("Email information missing. Please login again.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.70f)
                        .align(Alignment.CenterHorizontally),
                    text = "Verify & Proceed",
                    enabled = fullOtpCode.length == 6
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Back to Login", color = Slate500, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        FloatingBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(0.88f),
                elevation = 20.dp
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(BPGreenLight)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = BPGreenPrimary
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Forgot Password",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "Enter your registered email address to receive a secure password reset link.",
                    textAlign = TextAlign.Center,
                    color = Slate500,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                GlassTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Slate400) }
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                GlassButton(
                    onClick = { viewModel.forgotPassword(email) },
                    modifier = Modifier
                        .fillMaxWidth(0.70f)
                        .align(Alignment.CenterHorizontally),
                    text = "Send Reset Link"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { viewModel.setScreen(ScreenType.LOGIN) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Back to Login", color = Slate500, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ResetPasswordScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        FloatingBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(0.88f),
                elevation = 20.dp
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(BPGreenLight)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = BPGreenPrimary
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Reset Password",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "Please enter your new strong password below.",
                    textAlign = TextAlign.Center,
                    color = Slate500,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                GlassTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "New Password",
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate400) },
                    visualTransformation = PasswordVisualTransformation()
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                GlassButton(
                    onClick = { viewModel.resetPassword(password) },
                    modifier = Modifier
                        .fillMaxWidth(0.70f)
                        .align(Alignment.CenterHorizontally),
                    text = "Update Password"
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("SAR") }
    var selectedPrefix by remember { mutableStateOf("+966") }
    var mobileNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        FloatingBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Tabs
            BPTabSwitcher(
                isUserTab = true,
                onTabChange = { if (!it) viewModel.setScreen(ScreenType.LOGIN) },
                modifier = Modifier
                    .width(220.dp)
                    .height(46.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            BPWalletLogo(sizeDp = 75, showSubtitle = true)

            Spacer(modifier = Modifier.height(24.dp))

            // 88% Width Floating White Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(bottom = 32.dp),
                elevation = 20.dp
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "Join BP Wallet for instant payouts",
                    fontSize = 14.sp,
                    color = Slate500,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(28.dp))

                GlassTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Slate400) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                GlassTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Full Name",
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Slate400) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                GlassTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Slate400) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Select Currency",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Slate600
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("PKR", "+92", "Pakistan"),
                        Triple("AED", "+971", "UAE"),
                        Triple("SAR", "+966", "Saudi Arabia")
                    ).forEach { (code, prefix, country) ->
                        CurrencyCard(
                            code = code,
                            prefix = prefix,
                            country = country,
                            isSelected = selectedCurrency == code,
                            onClick = {
                                selectedCurrency = code
                                selectedPrefix = prefix
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "WhatsApp / Mobile Number",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate600
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Prefix Display
                    Surface(
                        modifier = Modifier
                            .width(85.dp)
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = selectedPrefix,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Slate900
                            )
                        }
                    }

                    // Mobile Input
                    GlassTextField(
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        label = "Mobile Number",
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Slate400) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate400) },
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(28.dp))

                GlassButton(
                    onClick = {
                        viewModel.registerUser(
                            fullName = fullName,
                            email = email,
                            currency = selectedCurrency,
                            mobileNumber = "$selectedPrefix $mobileNumber",
                            pass = password,
                            username = username
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .align(Alignment.CenterHorizontally),
                    text = "Complete Register"
                )

                Spacer(modifier = Modifier.height(18.dp))

                TextButton(
                    onClick = { viewModel.setScreen(ScreenType.LOGIN) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Already registered? Login here",
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
