package com.bp.wallet.ui.screens

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
import com.bp.wallet.ui.components.*
import com.bp.wallet.ui.theme.*
import com.bp.wallet.ui.viewmodel.BPWalletViewModel
import com.bp.wallet.ui.viewmodel.ScreenType
import com.bp.wallet.BuildConfig
import com.bp.wallet.R

@Composable
fun ModernLightBlobBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blob_anim")
    val offsetY1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )
    val offsetY2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFE8F5E9).copy(alpha = 0.85f),
                radius = 290.dp.toPx(),
                center = Offset(size.width * 0.85f, size.height * 0.15f + offsetY1)
            )
            drawCircle(
                color = Color(0xFFE3F2FD).copy(alpha = 0.85f),
                radius = 270.dp.toPx(),
                center = Offset(size.width * 0.12f, size.height * 0.68f + offsetY2)
            )
            drawCircle(
                color = Color(0xFFC8E6C9).copy(alpha = 0.35f),
                radius = 180.dp.toPx(),
                center = Offset(size.width * 0.5f, size.height * 0.88f)
            )
        }
        content()
    }
}

@Composable
fun AuthTabSwitcher(
    isUserTab: Boolean,
    onTabChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color(0xFFEEEEEE),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(if (isUserTab) Color(0xFF00C853) else Color.Transparent)
                    .clickable { onTabChange(true) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "User",
                    fontWeight = if (isUserTab) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (isUserTab) Color.White else Color(0xFF757575)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(if (!isUserTab) Color(0xFF00C853) else Color.Transparent)
                    .clickable { onTabChange(false) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = if (!isUserTab) Color.White else Color(0xFF757575),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Admin",
                        fontWeight = if (!isUserTab) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (!isUserTab) Color.White else Color(0xFF757575)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernFilledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF9E9E9E),
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    ),
                    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isPassword) {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color(0xFF9E9E9E)
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    val isUserTab by viewModel.isUserLoginTab.collectAsState()
    val errorShakeTrigger by viewModel.errorShakeTrigger.collectAsState()
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(errorShakeTrigger) {
        if (errorShakeTrigger > 0) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -20f at 50
                    20f at 100
                    -16f at 150
                    16f at 200
                    -10f at 250
                    10f at 300
                    -4f at 350
                    0f at 400
                }
            )
        }
    }
    
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var adminUsername by remember { mutableStateOf("Admin") }
    var adminPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }

    ModernLightBlobBackground(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // USER / ADMIN TOGGLE (Above Card)
            AuthTabSwitcher(
                isUserTab = isUserTab,
                onTabChange = { viewModel.setLoginTab(it) },
                modifier = Modifier
                    .width(210.dp)
                    .height(42.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // CENTER GLASS CARD (90% width, max 400.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 400.dp)
                    .offset(x = shakeOffset.value.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color(0xFF00C853).copy(alpha = 0.15f))
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circular Logo Image
                    Image(
                        painter = painterResource(id = R.drawable.bp_logo),
                        contentDescription = "BP Wallet Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(2.dp, Color(0xFF00C853)), CircleShape)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "BP WALLET",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isUserTab) "Welcome Back" else "Admin Login",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (isUserTab) "Please sign in to continue" else "Enter admin credentials to continue",
                        fontSize = 14.sp,
                        color = Color(0xFF757575),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isUserTab) {
                        // USER INPUT FIELDS
                        ModernFilledTextField(
                            value = emailOrPhone,
                            onValueChange = { emailOrPhone = it },
                            placeholder = "Username or Email",
                            leadingIcon = Icons.Default.Person
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        ModernFilledTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Password",
                            leadingIcon = Icons.Default.Lock,
                            isPassword = true
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // SIGN IN BUTTON (Bright green gradient, pill shape 26.dp)
                        Button(
                            onClick = { viewModel.loginUser(emailOrPhone, password) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = Color(0xFF00C853)),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF00C853), Color(0xFF00E676))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // ROW BELOW BUTTON: Remember Me + Forgot Password
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { rememberMe = !rememberMe }
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF00C853),
                                        uncheckedColor = Color(0xFF9E9E9E)
                                    )
                                )
                                Text(
                                    text = "Remember Me",
                                    fontSize = 13.sp,
                                    color = Color(0xFF616161)
                                )
                            }

                            Text(
                                text = "Forgot Password?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00C853),
                                modifier = Modifier.clickable {
                                    viewModel.setScreen(ScreenType.FORGOT_PASSWORD)
                                }
                            )
                        }

                        // DIVIDER: OR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE0E0E0)
                            )
                            Text(
                                text = "OR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9E9E9E),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE0E0E0)
                            )
                        }

                        // CREATE NEW ACCOUNT BUTTON
                        OutlinedButton(
                            onClick = { viewModel.setScreen(ScreenType.REGISTER) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(26.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
                        ) {
                            Text(
                                text = "CREATE NEW ACCOUNT",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )
                        }
                    } else {
                        // ADMIN LOGIN FIELDS (Clean & Minimal)
                        ModernFilledTextField(
                            value = adminUsername,
                            onValueChange = { adminUsername = it },
                            placeholder = "Admin Username",
                            leadingIcon = Icons.Default.AdminPanelSettings
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        ModernFilledTextField(
                            value = adminPassword,
                            onValueChange = { adminPassword = it },
                            placeholder = "Admin Password",
                            leadingIcon = Icons.Default.Lock,
                            isPassword = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.loginAdmin(adminUsername, adminPassword) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = Color(0xFF00C853)),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF00C853), Color(0xFF00E676))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
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
    val isLoading by viewModel.isLoading.collectAsState()
    var userEnteredEmail by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verifyPassword by remember { mutableStateOf("") }
    var showPasswordCheck by remember { mutableStateOf(false) }
    
    val activeEmail = pendingEmail.ifBlank { userEnteredEmail }.trim()

    // Countdown Timer Logic for Resend
    var timeLeftSec by remember { mutableIntStateOf(60) }
    var isTimerActive by remember { mutableStateOf(true) }

    LaunchedEffect(isTimerActive, timeLeftSec) {
        if (isTimerActive && timeLeftSec > 0) {
            delay(1000L)
            timeLeftSec -= 1
        } else if (timeLeftSec == 0) {
            isTimerActive = false
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        FloatingBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 20.dp, horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(0.94f),
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Verify Your Account",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                if (pendingEmail.isNotBlank()) {
                    Text(
                        text = "Verification link & 6-digit code sent to:",
                        textAlign = TextAlign.Center,
                        color = Slate500,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = pendingEmail,
                        textAlign = TextAlign.Center,
                        color = BPGreenDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    Text(
                        text = "Enter your registered email address to verify your account or activate instantly.",
                        textAlign = TextAlign.Center,
                        color = Slate500,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GlassTextField(
                        value = userEnteredEmail,
                        onValueChange = { userEnteredEmail = it },
                        label = "Your Email Address",
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Slate400) }
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                // 6-DIGIT OTP CODE INPUT SECTION
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = BPGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enter 6-Digit Code",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        GlassTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 8) otpCode = it },
                            label = "6-Digit OTP / Code",
                            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = Slate400) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { otpCode = "123456" },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Slate300),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Fill 123456", fontSize = 12.sp, color = Slate700)
                            }

                            Button(
                                onClick = {
                                    if (activeEmail.isNotBlank()) {
                                        viewModel.verifyEmailWithOtp(activeEmail, otpCode)
                                    } else {
                                        viewModel.showSnack("Please provide your email address")
                                    }
                                },
                                enabled = !isLoading && otpCode.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text("Verify Code", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // INSTANT ACTIVATION BUTTON (If email code not received)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.5.dp, Color(0xFFFDE68A))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Code Not Received?",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Email server delayed or blocked? Activate your account instantly with 1 tap.",
                            fontSize = 12.sp,
                            color = Color(0xFF78350F),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GlassButton(
                            onClick = {
                                if (activeEmail.isNotBlank()) {
                                    viewModel.instantActivateUser(activeEmail)
                                } else {
                                    viewModel.showSnack("Please enter your email or username first.")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.9f),
                            text = "⚡ Instant Activate Account",
                            gradientColors = listOf(Color(0xFFD97706), Color(0xFFB45309)),
                            glowColor = Color(0xFFFBBF24).copy(alpha = 0.4f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.CenterHorizontally),
                        color = BPGreenPrimary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Password Verification Check
                if (!showPasswordCheck) {
                    TextButton(
                        onClick = { showPasswordCheck = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Already Confirmed? Log In with Password",
                            color = BPGreenDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GlassTextField(
                            value = verifyPassword,
                            onValueChange = { verifyPassword = it },
                            label = "Password",
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate400) },
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                if (activeEmail.isNotBlank()) {
                                    viewModel.checkAndVerifyUser(activeEmail, verifyPassword)
                                } else {
                                    viewModel.showSnack("Please enter your email address first.")
                                }
                            },
                            enabled = !isLoading && verifyPassword.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, BPGreenPrimary),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Text(
                                text = "Check Status & Log In",
                                color = BPGreenDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Resend Confirmation Email Option
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
                            text = "Resend email in ${timeLeftSec}s",
                            fontSize = 13.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        TextButton(
                            onClick = { 
                                if (activeEmail.isNotBlank()) {
                                    viewModel.resendVerification(activeEmail)
                                    timeLeftSec = 60
                                    isTimerActive = true
                                } else {
                                    viewModel.showSnack("Please enter your email address first.")
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Text(
                                text = "Resend Verification Email",
                                color = BPGreenPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { viewModel.setScreen(ScreenType.LOGIN) }
                    ) {
                        Text("Back to Login", color = Slate500, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                    TextButton(
                        onClick = { viewModel.setScreen(ScreenType.REGISTER) }
                    ) {
                        Text("New Account", color = Slate500, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
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
    val errorShakeTrigger by viewModel.errorShakeTrigger.collectAsState()
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(errorShakeTrigger) {
        if (errorShakeTrigger > 0) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -20f at 50
                    20f at 100
                    -16f at 150
                    16f at 200
                    -10f at 250
                    10f at 300
                    -4f at 350
                    0f at 400
                }
            )
        }
    }

    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("SAR") }
    var selectedPrefix by remember { mutableStateOf("+966") }
    var mobileNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    ModernLightBlobBackground(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // USER / ADMIN TOGGLE (Above Card)
            AuthTabSwitcher(
                isUserTab = true,
                onTabChange = { isUser ->
                    if (!isUser) {
                        viewModel.setLoginTab(false)
                        viewModel.setScreen(ScreenType.LOGIN)
                    }
                },
                modifier = Modifier
                    .width(210.dp)
                    .height(42.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 90% Width Floating White Glass Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 400.dp)
                    .offset(x = shakeOffset.value.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color(0xFF00C853).copy(alpha = 0.15f))
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bp_logo),
                        contentDescription = "BP Wallet Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(2.dp, Color(0xFF00C853)), CircleShape)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "BP WALLET",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Create Account",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Join BP Wallet for instant payouts",
                        fontSize = 14.sp,
                        color = Color(0xFF757575),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ModernFilledTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = "Username",
                        leadingIcon = Icons.Default.Person
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ModernFilledTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = "Full Name",
                        leadingIcon = Icons.Default.Badge
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ModernFilledTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email Address",
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Select Currency",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF616161),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "WhatsApp / Mobile Number",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF616161),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(85.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F5F5)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = selectedPrefix,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.Black
                                )
                            }
                        }

                        ModernFilledTextField(
                            value = mobileNumber,
                            onValueChange = { mobileNumber = it },
                            placeholder = "Mobile Number",
                            leadingIcon = Icons.Default.Phone,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ModernFilledTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

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
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF00C853), Color(0xFF00E676))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { viewModel.setScreen(ScreenType.LOGIN) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Already have an account? ",
                                fontSize = 14.sp,
                                color = Color(0xFF757575)
                            )
                            Text(
                                text = "Sign In",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00C853)
                            )
                        }
                    }
                }
            }
        }
    }
}
