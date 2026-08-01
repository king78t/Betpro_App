package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.ui.components.BPTabSwitcher
import com.example.ui.components.BPWalletLogo
import com.example.ui.components.CurrencyCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.BPWalletViewModel
import com.example.ui.viewmodel.ScreenType

@Composable
fun LoginScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
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
    var adminUsername by remember { mutableStateOf("Book") }
    var adminPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showGooglePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (showGooglePicker) {
        GoogleSignInAccountPickerModal(
            onDismiss = { showGooglePicker = false },
            onAccountSelected = { em, name ->
                showGooglePicker = false
                viewModel.signInWithGoogle(em, name, null)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background pastel glassmorphism circles from image
        Box(
            modifier = Modifier
                .size(310.dp)
                .align(Alignment.TopStart)
                .offset(x = (-80).dp, y = (-70).dp)
                .background(
                    color = Color(0xFFD1F4E0).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(1000.dp)
                )
        )
        Box(
            modifier = Modifier
                .size(330.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
                .background(
                    color = Color(0xFFDCEEFE).copy(alpha = 0.8f),
                    shape = RoundedCornerShape(1000.dp)
                )
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-70).dp, y = 140.dp)
                .background(
                    color = Color(0xFFDCEEFE).copy(alpha = 0.75f),
                    shape = RoundedCornerShape(1000.dp)
                )
        )
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 110.dp, y = 160.dp)
                .background(
                    color = Color(0xFFD1F4E0).copy(alpha = 0.85f),
                    shape = RoundedCornerShape(1000.dp)
                )
        )
        // Two floating frosted rounded squares behind card
        Box(
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.TopStart)
                .offset(x = 36.dp, y = 60.dp)
                .background(
                    color = Color.White.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(24.dp)
                )
        )
        Box(
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = 80.dp)
                .background(
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(26.dp)
                )
        )

        // Main Frosted Glass Card
        Card(
            modifier = Modifier
                .offset { IntOffset(x = shakeOffsetX.value.roundToInt(), y = 0) }
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFAFDFB).copy(alpha = 0.90f)
            ),
            border = BorderStroke(1.5.dp, Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isUserTab) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            onClick = { viewModel.setLoginTab(true) },
                            shape = RoundedCornerShape(12.dp),
                            color = Slate100,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to User Login",
                                    tint = BPGreenDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Back to User Login",
                                    color = BPGreenDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Logo & Title
                BPWalletLogo(sizeDp = 72, showSubtitle = isUserTab)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isUserTab) "Welcome Back" else "Admin Portal Login",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isUserTab) "Please login to continue" else "Enter admin credentials to continue",
                    fontSize = 15.sp,
                    color = Slate500
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (isUserTab) {
                    // USER LOGIN FIELDS
                    GlassInputField(
                        value = emailOrPhone,
                        onValueChange = { emailOrPhone = it },
                        placeholder = "Mobile Number or Email",
                        leadingIcon = Icons.Default.Person
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    GlassInputField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Remember Me & Forgot Password?
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
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF10B981)
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Remember Me",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate700
                            )
                        }

                        Text(
                            text = "Forgot Password?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            modifier = Modifier.clickable {
                                viewModel.showSnack("Please contact WhatsApp Helpline for instant password reset.")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Green Login Button (same exact design style as image!)
                    GlassLoginButton(
                        text = "Login",
                        onClick = {
                            viewModel.loginUser(emailOrPhone, password)
                        }
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Spacer(modifier = Modifier.height(8.dp))

                    // CREATE NEW ACCOUNT Button
                    GlassCreateAccountButton(
                        text = "CREATE NEW ACCOUNT",
                        onClick = { viewModel.setScreen(ScreenType.REGISTER) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ADMIN LOGIN BUTTON BELOW CREATE NEW ACCOUNT
                    Surface(
                        onClick = { viewModel.setLoginTab(false) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Login",
                                tint = Slate700,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Admin",
                                color = Slate900,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                } else {
                    // ADMIN LOGIN FIELDS
                    GlassInputField(
                        value = adminUsername,
                        onValueChange = { adminUsername = it },
                        placeholder = "Admin Username",
                        leadingIcon = Icons.Default.AdminPanelSettings
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    GlassInputField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        placeholder = "Admin Password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Exactly the SAME COLOR and STYLE green button as User Login page!
                    GlassLoginButton(
                        text = "Admin Login",
                        onClick = {
                            viewModel.loginAdmin(adminUsername, adminPassword)
                        }
                    )
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
    onPasswordToggle: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Slate500,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = placeholder,
                tint = Slate500,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = Slate500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color(0xFFF1F5F9),
            focusedContainerColor = Color(0xFFF1F5F9),
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color(0xFF10B981),
            cursorColor = Color(0xFF10B981),
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
            .height(52.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(50),
                spotColor = Color(0xFF10B981),
                ambientColor = Color(0xFF059669)
            ),
        shape = RoundedCornerShape(50),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
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
                letterSpacing = 0.5.sp
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
            .height(50.dp),
        shape = RoundedCornerShape(50),
        color = Color(0xFFE4F2EE),
        border = BorderStroke(1.2.dp, Color(0xFFBAE6DA))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color(0xFF065F46),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: BPWalletViewModel,
    modifier: Modifier = Modifier
) {
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

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("PKR") }
    var selectedPrefix by remember { mutableStateOf("+92") }
    var mobileNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showGooglePicker by remember { mutableStateOf(false) }
    val pendingRegistration by viewModel.pendingRegistration.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (showGooglePicker) {
        GoogleSignInAccountPickerModal(
            onDismiss = { showGooglePicker = false },
            onAccountSelected = { em, name ->
                showGooglePicker = false
                viewModel.signInWithGoogle(em, name, null)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .offset { IntOffset(x = shakeOffsetX.value.roundToInt(), y = 0) }
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo & Title
                BPWalletLogo(sizeDp = 68, showSubtitle = true)

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Create Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Join BP Wallet for instant 24/7 payouts",
                    fontSize = 13.sp,
                    color = Slate500
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Full Name",
                            tint = Slate500
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BPGreenPrimary,
                        unfocusedBorderColor = Slate500
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Email Address
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = Slate500
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BPGreenPrimary,
                        unfocusedBorderColor = Slate500
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Select Country & Currency label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Select Country & Currency",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate700
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // 3 Cards: PKR, AED, SAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CurrencyCard(
                        code = "PKR",
                        prefix = "+92",
                        country = "Pakistan",
                        isSelected = selectedCurrency == "PKR",
                        onClick = {
                            selectedCurrency = "PKR"
                            selectedPrefix = "+92"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    CurrencyCard(
                        code = "AED",
                        prefix = "+971",
                        country = "UAE",
                        isSelected = selectedCurrency == "AED",
                        onClick = {
                            selectedCurrency = "AED"
                            selectedPrefix = "+971"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    CurrencyCard(
                        code = "SAR",
                        prefix = "+966",
                        country = "Saudi Arabia",
                        isSelected = selectedCurrency == "SAR",
                        onClick = {
                            selectedCurrency = "SAR"
                            selectedPrefix = "+966"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // WhatsApp / Mobile Number label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "WhatsApp / Mobile Number",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    leadingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = selectedPrefix,
                                fontWeight = FontWeight.ExtraBold,
                                color = BPGreenDark,
                                fontSize = 14.sp
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BPGreenPrimary,
                        unfocusedBorderColor = Slate500
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Choose Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Choose Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = Slate500
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password",
                                tint = Slate500
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BPGreenPrimary,
                        unfocusedBorderColor = Slate500
                    )
                )

                Spacer(modifier = Modifier.height(26.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Premium3DButton(
                        text = "Complete Register",
                        onClick = {
                            viewModel.registerUser(
                                fullName = fullName,
                                email = email,
                                currency = selectedCurrency,
                                mobileNumber = "$selectedPrefix $mobileNumber",
                                pass = password
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = { viewModel.setScreen(ScreenType.LOGIN) }
                ) {
                    Text(
                        text = "Already registered? Login here",
                        fontWeight = FontWeight.Bold,
                        color = BPGreenDark,
                        fontSize = 14.sp
                    )
                }
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
            .widthIn(min = 130.dp, max = 165.dp)
            .height(36.dp)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = backgroundColor,
                ambientColor = backgroundColor
            ),
        shape = RoundedCornerShape(18.dp),
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
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = text,
                    color = textCol,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                if (icon != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = textCol,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("google_sign_in_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "G",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4285F4)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continue with Google",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )
        }
    }
}

@Composable
fun GoogleSignInAccountPickerModal(
    onDismiss: () -> Unit,
    onAccountSelected: (email: String, name: String) -> Unit
) {
    val context = LocalContext.current
    val deviceAccounts = remember {
        try {
            android.accounts.AccountManager.get(context)
                .getAccountsByType("com.google")
                .map { Pair(it.name, it.name.substringBefore("@").replace(".", " ").replaceFirstChar { c -> c.uppercase() }) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    var customEmail by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE3F2FD),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "G",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4285F4)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Sign in with Google",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Connect your real Google Account",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (deviceAccounts.isNotEmpty()) {
                    Text(
                        text = "Google Accounts on this device:",
                        fontSize = 13.sp,
                        color = Slate700,
                        fontWeight = FontWeight.Bold
                    )
                    deviceAccounts.forEach { (email, name) ->
                        Surface(
                            onClick = { onAccountSelected(email, name) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_account_$email")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF4285F4),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = email,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = Slate500
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Select Account",
                                    tint = Color(0xFF4285F4),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = "Enter your Google account email to connect:",
                    fontSize = 13.sp,
                    color = Slate700,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = customEmail,
                    onValueChange = {
                        customEmail = it
                        errorMessage = ""
                    },
                    label = { Text("Google Email Address") },
                    placeholder = { Text("yourname@gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4285F4),
                        unfocusedBorderColor = Slate300
                    )
                )

                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Full Name (Optional)") },
                    placeholder = { Text("e.g. John Doe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4285F4),
                        unfocusedBorderColor = Slate300
                    )
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val email = customEmail.trim()
                    if (email.isEmpty() || !email.contains("@")) {
                        errorMessage = "Please enter a valid Google email address"
                        return@Button
                    }
                    val name = customName.trim().ifEmpty {
                        email.substringBefore("@").replace(".", " ").replaceFirstChar { c -> c.uppercase() }
                    }
                    onAccountSelected(email, name)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Connect Google Account", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate500, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

fun triggerGoogleSignIn(
    context: Context,
    scope: CoroutineScope,
    onNeedPicker: () -> Unit,
    onTokenReceived: (email: String, name: String, idToken: String?) -> Unit
) {
    scope.launch {
        try {
            val credentialManager = androidx.credentials.CredentialManager.create(context)
            val webClientId = "1234567890-example.apps.googleusercontent.com"
            val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = androidx.credentials.GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is androidx.credentials.CustomCredential &&
                credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential =
                    com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                val email = googleIdTokenCredential.id
                val name = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                onTokenReceived(email, name, googleIdTokenCredential.idToken)
            } else {
                onNeedPicker()
            }
        } catch (e: Exception) {
            onNeedPicker()
        }
    }
}



