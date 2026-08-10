package com.bp.uunwlm.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import kotlin.math.roundToInt
import com.bp.uunwlm.ui.components.BPTabSwitcher
import com.bp.uunwlm.ui.components.BPWalletLogo
import com.bp.uunwlm.ui.components.CurrencyCard
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
    // Note: All OTP / Security Verification popup dialogs are 100% removed.
    // User login directly navigates to UserHomeScreen (dashboard) without any verification step.
    val isUserTab by viewModel.isUserLoginTab.collectAsState()
    val errorShakeTrigger by viewModel.errorShakeTrigger.collectAsState()
    val shakeOffsetX = remember { Animatable(0f) }

    LaunchedEffect(errorShakeTrigger) {
        if (errorShakeTrigger > 0) {
            for (i in 0..2) {
                shakeOffsetX.animateTo(24f, animationSpec = tween(50, easing = LinearEasing))
                shakeOffsetX.animateTo(-24f, animationSpec = tween(50, easing = LinearEasing))
            }
            shakeOffsetX.animateTo(0f, animationSpec = tween(50, easing = LinearEasing))
        }
    }

    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var adminUsername by remember { mutableStateOf("Admin") }
    var adminPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0F2F1), // Light mint top
                        Color(0xFFF1FDF7)  // Very light green bottom
                    )
                )
            )
    ) {
        // Decorative soft circles
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.TopStart)
                .offset(x = (-100).dp, y = (-60).dp)
                .background(Color(0xFFD1F2EB).copy(alpha = 0.4f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = 40.dp)
                .background(Color(0xFFE8F8F5).copy(alpha = 0.5f), CircleShape)
        )

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

            Spacer(modifier = Modifier.height(36.dp))

            // BP WALLET SHIELD LOGO
            BPWalletLogo(sizeDp = 90, showSubtitle = true)

            Spacer(modifier = Modifier.height(18.dp))

            // Main Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isUserTab) "Welcome Back" else "Admin Portal",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1A1A),
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Please login to continue",
                    fontSize = 15.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                if (isUserTab) {
                    // USER LOGIN FIELDS
                    GlassInputField(
                        value = emailOrPhone,
                        onValueChange = { emailOrPhone = it },
                        placeholder = "Username",
                        leadingIcon = Icons.Default.Person,
                        testTag = "login_username_input",
                        errorShakeTrigger = errorShakeTrigger
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Password",
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp, start = 4.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )

                    GlassInputField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "••••••••",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible },
                        testTag = "login_password_input",
                        errorShakeTrigger = errorShakeTrigger
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Forgot Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Forgot Password?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BPGreenPrimary,
                            modifier = Modifier.clickable {
                                viewModel.setScreen(ScreenType.FORGOT_PASSWORD)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Login Button (Gradient)
                    Button(
                        onClick = { viewModel.loginUser(emailOrPhone, password) },
                        modifier = Modifier
                            .width(160.dp)
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = BPGreenPrimary.copy(alpha = 0.5f))
                            .testTag("login_button"),
                        shape = RoundedCornerShape(26.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF00E676), Color(0xFF00A344))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Login",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // OR Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.5.dp, color = Color(0xFFE0E0E0))
                        Surface(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            shape = CircleShape,
                            color = Color(0xFFF5F5F5),
                            border = BorderStroke(0.5.dp, Color(0xFFE0E0E0))
                        ) {
                            Text(
                                text = "OR",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color(0xFF9E9E9E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.5.dp, color = Color(0xFFE0E0E0))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // CREATE NEW ACCOUNT Button
                    OutlinedButton(
                        onClick = { viewModel.setScreen(ScreenType.REGISTER) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("create_account_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                    ) {
                        Text(
                            text = "CREATE NEW ACCOUNT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF212121),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                } else {
                    // ADMIN LOGIN
                    GlassInputField(
                        value = adminUsername,
                        onValueChange = { adminUsername = it },
                        placeholder = "Admin Username",
                        leadingIcon = Icons.Default.AdminPanelSettings,
                        testTag = "admin_username_input",
                        errorShakeTrigger = errorShakeTrigger
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    GlassInputField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        placeholder = "Admin Password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible },
                        testTag = "admin_password_input",
                        errorShakeTrigger = errorShakeTrigger
                    )
                    Spacer(modifier = Modifier.height(34.dp))
                    Button(
                        onClick = { viewModel.loginAdmin(adminUsername, adminPassword) },
                        modifier = Modifier
                            .width(180.dp)
                            .height(54.dp)
                            .shadow(8.dp, RoundedCornerShape(27.dp), spotColor = Color(0xFF00C853).copy(alpha = 0.5f))
                            .testTag("admin_login_button"),
                        shape = RoundedCornerShape(27.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF00E676), Color(0xFF00A344))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Admin Login",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {},
    testTag: String = "",
    errorShakeTrigger: Int = 0
) {
    val shakeOffsetX = remember { Animatable(0f) }

    LaunchedEffect(errorShakeTrigger) {
        if (errorShakeTrigger > 0) {
            for (i in 0..2) {
                shakeOffsetX.animateTo(24f, animationSpec = tween(50, easing = LinearEasing))
                shakeOffsetX.animateTo(-24f, animationSpec = tween(50, easing = LinearEasing))
            }
            shakeOffsetX.animateTo(0f, animationSpec = tween(50, easing = LinearEasing))
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Slate400,
                fontSize = 15.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = placeholder,
                tint = Slate400,
                modifier = Modifier.size(22.dp)
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = Slate400,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffsetX.value.roundToInt(), 0) }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Slate200,
            focusedBorderColor = if (errorShakeTrigger > 0 && shakeOffsetX.value != 0f) Color.Red else BPGreenPrimary,
            cursorColor = BPGreenPrimary,
            focusedTextColor = Slate900,
            unfocusedTextColor = Slate900
        )
    )
}

@Composable
fun GlassLoginButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(25.dp),
                spotColor = Color(0xFF10B981).copy(alpha = 0.35f)
            ),
        shape = RoundedCornerShape(25.dp),
        color = Color(0xFF10B981),
        border = BorderStroke(1.dp, Color(0xFF059669))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF10B981),
                            Color(0xFF059669)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
fun VerifyEmailScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val pendingEmail by viewModel.pendingVerificationEmail.collectAsState()
    val errorShakeTrigger by viewModel.errorShakeTrigger.collectAsState()
    var otpCode by remember { mutableStateOf("") }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1FDF7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MarkEmailRead,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = BPGreenPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Verify OTP",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "We've sent a 6-digit verification code to your email: ${pendingEmail.ifBlank { "your email" }}. Please enter it below to activate your account.",
                textAlign = TextAlign.Center,
                color = Slate600,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            GlassInputField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it },
                placeholder = "6-Digit Code",
                leadingIcon = Icons.Default.VpnKey,
                errorShakeTrigger = errorShakeTrigger
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    if (pendingEmail.isNotBlank()) {
                        viewModel.verifyOtp(pendingEmail, otpCode)
                    } else {
                        viewModel.showSnack("Email information missing. Please try logging in again.")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
            ) {
                Text("Verify Account", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { 
                    if (pendingEmail.isNotBlank()) {
                        viewModel.resendVerification(pendingEmail)
                    }
                }
            ) {
                Text("Resend Verification Code", color = BPGreenDark, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = { viewModel.logout() }
            ) {
                Text("Back to Login", color = Slate500, fontWeight = FontWeight.Medium)
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
    
    val errorShakeTrigger by viewModel.errorShakeTrigger.collectAsState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1FDF7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = BPGreenPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Forgot Password?",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Enter your registered email address to receive a password reset link.",
                textAlign = TextAlign.Center,
                color = Slate600,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            GlassInputField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email Address",
                leadingIcon = Icons.Default.Email,
                errorShakeTrigger = errorShakeTrigger
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { viewModel.forgotPassword(email) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
            ) {
                Text("Send Reset Link", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = { viewModel.setScreen(ScreenType.LOGIN) }
            ) {
                Text("Back to Login", color = BPGreenDark, fontWeight = FontWeight.Bold)
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
    var passwordVisible by remember { mutableStateOf(false) }
    
    val errorShakeTrigger by viewModel.errorShakeTrigger.collectAsState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1FDF7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = BPGreenPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Reset Password",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Please enter your new password below.",
                textAlign = TextAlign.Center,
                color = Slate600,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            GlassInputField(
                value = password,
                onValueChange = { password = it },
                placeholder = "New Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordToggle = { passwordVisible = !passwordVisible },
                errorShakeTrigger = errorShakeTrigger
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { viewModel.resetPassword(password) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
            ) {
                Text("Update Password", fontWeight = FontWeight.Bold)
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
    var passwordVisible by remember { mutableStateOf(false) }
    
    val errorShakeTrigger by viewModel.errorShakeTrigger.collectAsState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0F2F1),
                        Color(0xFFF1FDF7)
                    )
                )
            )
    ) {
        // Decorative blobs
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-40).dp)
                .background(Color(0xFFD1F2EB).copy(alpha = 0.4f), CircleShape)
        )

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

            Spacer(modifier = Modifier.height(30.dp))

            BPWalletLogo(sizeDp = 85, showSubtitle = true)

            Spacer(modifier = Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1A1A),
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Join BP Wallet for instant 24/7 payouts",
                    fontSize = 15.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Username
                Text(
                    text = "Username",
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp, start = 4.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                GlassInputField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "e.g. hamza_malik",
                    leadingIcon = Icons.Default.Person,
                    errorShakeTrigger = errorShakeTrigger
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Full Name
                Text(
                    text = "Full Name",
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp, start = 4.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                GlassInputField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = "e.g. Hamza Malik",
                    leadingIcon = Icons.Default.Badge,
                    errorShakeTrigger = errorShakeTrigger
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Email Address
                Text(
                    text = "Email Address",
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp, start = 4.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                GlassInputField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "hamza@bpexch.com",
                    leadingIcon = Icons.Default.Email,
                    errorShakeTrigger = errorShakeTrigger
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Country & Currency Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Country & Currency",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF424242)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                Spacer(modifier = Modifier.height(24.dp))

                // Mobile Number Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WhatsApp / Mobile Number",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Flag, // Use a generic flag icon or image if available
                            contentDescription = null,
                            tint = Color(0xFF388E3C),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selectedCurrency == "SAR") "Saudi Arabia (+966)" else if (selectedCurrency == "PKR") "Pakistan (+92)" else "UAE (+971)",
                            fontSize = 11.sp,
                            color = Color(0xFF388E3C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Prefix Card
                    Surface(
                        modifier = Modifier
                            .width(85.dp)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedPrefix,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1A1A1A)
                            )
                        }
                    }

                    // Mobile Input
                    GlassInputField(
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        placeholder = "501234567",
                        leadingIcon = Icons.Default.Phone,
                        modifier = Modifier.weight(1f),
                        errorShakeTrigger = errorShakeTrigger
                    )
                }

                Text(
                    text = "Locked to ${if (selectedCurrency == "SAR") "Saudi Arabia" else if (selectedCurrency == "PKR") "Pakistan" else "UAE"}. Enter local number (e.g. 501234567).",
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.align(Alignment.Start).padding(top = 8.dp, start = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Password Section
                Text(
                    text = "Password",
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp, start = 4.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                GlassInputField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "••••••••",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordToggle = { passwordVisible = !passwordVisible },
                    errorShakeTrigger = errorShakeTrigger
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Complete Register Button
                Button(
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
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFF00C853).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text(
                        text = "Complete Register",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(onClick = { viewModel.setScreen(ScreenType.LOGIN) }) {
                    Text(
                        text = "Already registered? Login here",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C853),
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun Premium3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = BPGreenPrimary,
    bottomShadowColor: Color = Color(0xFF0F6E38),
    textColor: Color = Color.White,
    icon: ImageVector? = null,
    isOutlined: Boolean = false
) {
    val containerCol = if (isOutlined) Color.White else backgroundColor
    val borderCol = if (isOutlined) BPGreenPrimary else bottomShadowColor
    val textCol = if (isOutlined) BPGreenDark else textColor

    Surface(
        onClick = onClick,
        modifier = modifier
            .widthIn(min = 140.dp)
            .height(50.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(25.dp),
                spotColor = backgroundColor.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(25.dp),
        color = containerCol,
        border = BorderStroke(if (isOutlined) 1.5.dp else 2.dp, borderCol)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isOutlined) {
                        Brush.verticalGradient(
                            colors = listOf(Color.White, Color(0xFFF1F8F3))
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor,
                                bottomShadowColor
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = text,
                    color = textCol,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                if (icon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = textCol,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}





