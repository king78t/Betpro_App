package com.bp.uunwlm.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bp.uunwlm.ui.components.BPLogoIcon
import com.bp.uunwlm.ui.viewmodel.BPWalletViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reusable navigation controller for the BP Wallet Ultra-Premium Fintech Splash Screen startup flow.
 * Respects authentication state without altering any auth logic:
 * - Logged-in user -> Dashboard (USER_HOME or ADMIN_DASHBOARD)
 * - Non-logged-in user -> Login Screen
 */
object SplashNavigator {
    fun navigate(viewModel: BPWalletViewModel, onNavigate: () -> Unit = {}) {
        viewModel.onSplashCompleted()
        onNavigate()
    }
}

/**
 * Ultra-Premium Fintech Splash Screen Experience.
 * Features Particle Logo Reveal, 3D Logo Animation, Tagline Glass Card, Trust Animation, and Luxury Neon Loader.
 */
@Composable
fun SplashScreen(
    viewModel: BPWalletViewModel,
    onNavigate: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    // State sequence flags
    var isRevealed by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }
    var showLoader by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    // Loader progress state (0f..1f)
    var loaderProgress by remember { mutableStateOf(0f) }

    // Rotating trust messages index
    var trustMessageIndex by remember { mutableStateOf(0) }
    val trustMessages = remember { listOf("🔒 Secure", "⚡ Fast", "✅ Trusted") }

    // Coordinate overall 2.8 second splash sequence
    LaunchedEffect(Unit) {
        // Step 1: Particle Logo Reveal starts immediately
        isRevealed = true
        delay(400)

        // Step 3: Brand Title appears
        showTitle = true
        delay(350)

        // Step 4: Tagline Frosted Glass Card appears
        showTagline = true
        delay(250)

        // Step 5 & 6: Loader and trust indicators appear
        showLoader = true

        // Progress bar smooth fill from 1.0s to 2.5s (1.5 seconds duration)
        val startTime = System.currentTimeMillis()
        val durationMs = 1450L
        while (System.currentTimeMillis() - startTime < durationMs) {
            val elapsed = System.currentTimeMillis() - startTime
            loaderProgress = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            delay(16)
        }
        loaderProgress = 1f

        // Initiate subtle exit transition (fade out & slight scale down)
        isExiting = true
        delay(350)

        // Haptic feedback upon completion & navigate
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            // Ignore if device lacks haptic support
        }
        SplashNavigator.navigate(viewModel, onNavigate)
    }

    // Rotating trust messages timer every 500ms
    LaunchedEffect(showLoader) {
        if (showLoader) {
            while (true) {
                delay(500)
                trustMessageIndex = (trustMessageIndex + 1) % trustMessages.size
            }
        }
    }

    // Exit animation scale and alpha
    val exitAlpha by animateFloatAsState(
        targetValue = if (isExiting) 0f else 1f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "exitAlpha"
    )
    val exitScale by animateFloatAsState(
        targetValue = if (isExiting) 0.95f else 1f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "exitScale"
    )

    // Main Luxury Fintech Splash Canvas & Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0A1833), // Deep Navy / Teal highlight center
                        Color(0xFF060B19), // Deep Navy Blue
                        Color(0xFF020409)  // Rich Black outer edge
                    )
                )
            )
            .alpha(exitAlpha)
            .scale(exitScale),
        contentAlignment = Alignment.Center
    ) {
        // Soft Glow Rings & Ambient Light Rays Background
        BackgroundFintechGlow()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            // STEP 1 & STEP 2: Particle Logo Reveal + 3D Logo Animation
            LogoRevealAnimation(isRevealed = isRevealed) {
                FloatingLogo()
            }

            Spacer(modifier = Modifier.height(22.dp))

            // STEP 3: Brand Title Reveal
            val titleAlpha by animateFloatAsState(
                targetValue = if (showTitle) 1f else 0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "titleAlpha"
            )
            val titleSlide by animateFloatAsState(
                targetValue = if (showTitle) 0f else 18f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "titleSlide"
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                        append("BP ")
                    }
                    withStyle(SpanStyle(color = Color(0xFF00E676), fontWeight = FontWeight.ExtraBold)) {
                        append("Wallet")
                    }
                },
                fontSize = 34.sp,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleSlide.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // STEP 4: Tagline Reveal in Frosted Glass Card
            val taglineAlpha by animateFloatAsState(
                targetValue = if (showTagline) 1f else 0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "taglineAlpha"
            )
            val taglineSlide by animateFloatAsState(
                targetValue = if (showTagline) 0f else 22f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "taglineSlide"
            )

            Box(
                modifier = Modifier
                    .alpha(taglineAlpha)
                    .offset(y = taglineSlide.dp)
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF14223A).copy(alpha = 0.48f))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00E676).copy(alpha = 0.45f),
                                Color(0xFF14B8A6).copy(alpha = 0.20f)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👑 Imandari Ek Mehnga Shok Hai\nHar Kisi Ke Bas ka Ni",
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // STEP 5: Trust Animation & Shield Icon
            val trustAlpha by animateFloatAsState(
                targetValue = if (showLoader) 1f else 0f,
                animationSpec = tween(durationMillis = 400),
                label = "trustAlpha"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(trustAlpha)
            ) {
                TrustIndicator(
                    currentMessage = trustMessages[trustMessageIndex]
                )

                Spacer(modifier = Modifier.height(20.dp))

                // STEP 6: Premium Loading Indicator
                PremiumLoader(
                    progress = loaderProgress,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}

/**
 * STEP 1: Particle Logo Reveal Animation
 * Renders emerald light particles converging to center when [isRevealed] becomes true.
 */
@Composable
fun LogoRevealAnimation(
    isRevealed: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val revealProgress by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "revealProgress"
    )

    // Logo scale and alpha build-up
    val logoScale = 0.55f + (0.45f * revealProgress)
    val logoAlpha = (revealProgress * 1.3f).coerceIn(0f, 1f)

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        // Emerald particles canvas drawn while converging
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val particleCount = 24
            val radius = 80.dp.toPx() * (1f - revealProgress)

            for (i in 0 until particleCount) {
                val angle = (i * 360f / particleCount) * (Math.PI / 180f)
                val x = center.x + (cos(angle).toFloat() * radius)
                val y = center.y + (sin(angle).toFloat() * radius)
                val particleAlpha = ((1f - revealProgress) * 0.9f).coerceIn(0f, 1f)

                drawCircle(
                    color = if (i % 2 == 0) Color(0xFF00E676) else Color(0xFF14B8A6),
                    radius = if (i % 3 == 0) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = Offset(x, y),
                    alpha = particleAlpha
                )
            }
        }

        Box(
            modifier = Modifier
                .scale(logoScale)
                .alpha(logoAlpha),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * STEP 2: 3D Logo Animation with Floating Glassmorphism Effect,
 * Emerald Green Glow, subtle rotation, and glass reflection.
 */
@Composable
fun FloatingLogo(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "floatingLogo")

    // Slight vertical floating movement (-6dp to +6dp)
    val floatY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    // Soft breathing emerald glow alpha (0.35 to 0.75)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Subtle rotation (-1.8 to +1.8 deg)
    val rotation by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .offset(y = floatY.dp)
            .rotate(rotation)
            .size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Emerald Aura Glow
        Box(
            modifier = Modifier
                .size(145.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E676).copy(alpha = glowAlpha),
                            Color(0xFF14B8A6).copy(alpha = glowAlpha * 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Frosted Glass Circular Rim
        Box(
            modifier = Modifier
                .size(130.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    spotColor = Color(0xFF00E676),
                    ambientColor = Color(0xFF14B8A6)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF101D33).copy(alpha = 0.92f),
                            Color(0xFF0B1426).copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00E676).copy(alpha = 0.85f),
                            Color(0xFF14B8A6).copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.25f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Uploaded BP Wallet Logo (Mint Green Squircle with Black Italic 'b')
            BPLogoIcon(sizeDp = 108)

            // High-End Glass Reflection / Shimmer Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent,
                                Color.Transparent
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(250f, 250f)
                        )
                    )
            )
        }
    }
}

/**
 * STEP 5: Trust Indicator with rotating trust messages and Security Shield check mark badge.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TrustIndicator(
    currentMessage: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF101E33).copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                color = Color(0xFF00E676).copy(alpha = 0.35f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Green Shield Icon with Check Mark Badge
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Security Shield",
                tint = Color(0xFF00E676),
                modifier = Modifier.size(18.dp)
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = Color(0xFF14B8A6),
                modifier = Modifier
                    .size(9.dp)
                    .offset(x = 6.dp, y = 6.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Rotating Trust Messages with smooth fade
        AnimatedContent(
            targetState = currentMessage,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "trustMessage"
        ) { msg ->
            Text(
                text = msg,
                color = Color(0xFFE2E8F0),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * STEP 6: Luxury Neon Progress Bar
 * Glass background track with Emerald glowing left-to-right fill and status text.
 */
@Composable
fun PremiumLoader(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF101C30))
                .border(
                    width = 0.8.dp,
                    color = Color(0xFF00E676).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(50)
                )
        ) {
            // Emerald neon filled track
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00E676),
                                Color(0xFF14B8A6),
                                Color(0xFF69F0AE)
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Loading Secure Wallet...",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Subtle background ambient light rays and glowing rings canvas.
 */
@Composable
private fun BackgroundFintechGlow() {
    val infiniteTransition = rememberInfiniteTransition(label = "bgGlow")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.38f)
        val maxRadius = size.minDimension * 0.65f * pulseRadius

        // Outer soft emerald ring
        drawCircle(
            color = Color(0xFF00E676),
            radius = maxRadius,
            center = center,
            alpha = 0.05f
        )
        // Inner teal aura ring
        drawCircle(
            color = Color(0xFF14B8A6),
            radius = maxRadius * 0.7f,
            center = center,
            alpha = 0.07f
        )
    }
}
