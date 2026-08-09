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

    val showOtpDialog by viewModel.showOtpDialog.collectAsState()

    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeOtpDialog() },
            title = { Text("Verify Phone Number") },
            text = {
                var code by remember { mutableStateOf("") }
                Column {
                    Text("Enter the 6-digit code sent to your mobile number")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { if (it.length <= 6) code = it },
                        label = { Text("Verification Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.verifyOtp(code) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary)
                    ) {
                        Text("Verify & Login")
                    }
                }
            },
            confirmButton = {}
        )
    }

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
                        placeholder = "Mobile Number or Email",
                        leadingIcon = Icons.Default.Person,
                        testTag = "login_email_input"
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
                        testTag = "login_password_input"
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Remember Me & Forgot Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2196F3)),
                                modifier = Modifier.size(24.dp).testTag("remember_me_checkbox")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Remember Me",
                                fontSize = 14.sp,
                                color = Color(0xFF616161),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "Forgot Password?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00C853),
                            modifier = Modifier.clickable {
                                viewModel.showSnack("Please contact WhatsApp Helpline for password recovery.")
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
                            .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = Color(0xFF00C853).copy(alpha = 0.5f))
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
                        testTag = "admin_username_input"
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
                        testTag = "admin_password_input"
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
    testTag: String = ""
) {
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
        modifier = modifier.fillMaxWidth().testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Slate200,
            focusedBorderColor = Color(0xFF00C853),
            cursorColor = Color(0xFF00C853),
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
fun GlassCreateAccountButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0xFFBAE6DA).copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, Color(0xFFBAE6DA))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF1F8F6)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color(0xFF065F46),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("SAR") }
    var selectedPrefix by remember { mutableStateOf("+966") }
    var mobileNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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
                    leadingIcon = Icons.Default.Person
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
                    leadingIcon = Icons.Default.Email
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
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Locked to ${if (selectedCurrency == "SAR") "Saudi Arabia" else if (selectedCurrency == "PKR") "Pakistan" else "UAE"}. Enter local number (e.g. 501234567).",
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.align(Alignment.Start).padding(top = 8.dp, start = 4.dp)
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
                            pass = password
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





