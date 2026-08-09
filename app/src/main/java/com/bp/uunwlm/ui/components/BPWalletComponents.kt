package com.bp.uunwlm.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bp.uunwlm.R
import com.bp.uunwlm.model.CelebrationEvent
import com.bp.uunwlm.ui.theme.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.*

@Composable
fun BPLogoIcon(
    sizeDp: Int = 80,
    modifier: Modifier = Modifier
) {
    val cornerRadius = (sizeDp * 0.26f).dp
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = Color(0xFF10B981),
                ambientColor = Color(0xFF047857)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF78F5C9), // Bright HD luminous mint top-left
                        Color(0xFF42E4A8), // Vibrant emerald mint middle
                        Color(0xFF15B874)  // Deep rich emerald bottom-right
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. HD Glass Specular Sheen (Top Gloss Highlight)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    center = Offset(w * 0.3f, h * 0.2f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.3f, h * 0.2f),
                radius = w * 0.65f
            )

            // 2. Mathematically refined sleek bold italic 'b' path matching HD logo
            val bPath = Path().apply {
                fillType = PathFillType.EvenOdd

                // Outer contour of bold sleek italic 'b'
                moveTo(0.25f * w, 0.77f * h)
                cubicTo(0.29f * w, 0.70f * h, 0.44f * w, 0.38f * h, 0.52f * w, 0.20f * h)
                cubicTo(0.54f * w, 0.16f * h, 0.60f * w, 0.18f * h, 0.58f * w, 0.24f * h)
                cubicTo(0.54f * w, 0.33f * h, 0.50f * w, 0.41f * h, 0.47f * w, 0.46f * h)
                cubicTo(0.63f * w, 0.38f * h, 0.79f * w, 0.42f * h, 0.82f * w, 0.57f * h)
                cubicTo(0.85f * w, 0.70f * h, 0.74f * w, 0.80f * h, 0.58f * w, 0.80f * h)
                cubicTo(0.44f * w, 0.80f * h, 0.31f * w, 0.82f * h, 0.25f * w, 0.77f * h)
                close()

                // Inner hole (counter) contour of 'b'
                moveTo(0.58f * w, 0.48f * h)
                cubicTo(0.68f * w, 0.48f * h, 0.71f * w, 0.61f * h, 0.61f * w, 0.68f * h)
                cubicTo(0.53f * w, 0.73f * h, 0.45f * w, 0.67f * h, 0.46f * w, 0.58f * h)
                cubicTo(0.47f * w, 0.51f * h, 0.51f * w, 0.48f * h, 0.58f * w, 0.48f * h)
                close()
            }

            // 3. HD Multi-layer Drop Shadow for 3D depth
            val shadowSoft = (sizeDp * 0.045f).dp.toPx()
            translate(left = shadowSoft, top = shadowSoft * 1.4f) {
                drawPath(
                    path = bPath,
                    color = Color.Black.copy(alpha = 0.18f)
                )
            }
            val shadowSharp = (sizeDp * 0.022f).dp.toPx()
            translate(left = shadowSharp, top = shadowSharp * 1.1f) {
                drawPath(
                    path = bPath,
                    color = Color.Black.copy(alpha = 0.28f)
                )
            }

            // 4. Subtle 3D Rim Highlight on top-left edge of 'b'
            translate(left = -1.dp.toPx(), top = -1.dp.toPx()) {
                drawPath(
                    path = bPath,
                    color = Color.White.copy(alpha = 0.25f)
                )
            }

            // 5. Solid rich graphite/anodized metallic dark gradient 'b'
            drawPath(
                path = bPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF20293A), // Polished dark slate top
                        Color(0xFF0F172A), // Deep graphite middle
                        Color(0xFF020617)  // Ultra black graphite bottom
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )
        }
    }
}

@Composable
fun BPWalletLogo(
    modifier: Modifier = Modifier,
    sizeDp: Int = 90,
    showSubtitle: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Logo Image (Mint Green Squircle with Black Italic 'b')
        BPLogoIcon(sizeDp = sizeDp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "BP WALLET",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            color = Color(0xFF00C853),
            letterSpacing = (-0.5).sp
        )
        
        if (showSubtitle) {
            Text(
                text = "OFFICIAL WALLET & DEPOSIT SERVICE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun BPTabSwitcher(
    isUserTab: Boolean,
    onTabChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        // User Tab
        Surface(
            onClick = { onTabChange(true) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(20.dp),
            color = if (isUserTab) Color.White else Color.Transparent,
            shadowElevation = if (isUserTab) 4.dp else 0.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "User",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isUserTab) Slate900 else Slate500,
                    fontSize = 15.sp
                )
            }
        }

        // Admin Tab
        Surface(
            onClick = { onTabChange(false) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(20.dp),
            color = if (!isUserTab) Color.White else Color.Transparent,
            shadowElevation = if (!isUserTab) 4.dp else 0.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = if (!isUserTab) Color(0xFF00C853) else Slate400,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Admin",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (!isUserTab) Slate900 else Slate500,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun CurrencyCard(
    code: String,
    prefix: String,
    country: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(78.dp)
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isSelected) Color(0xFF00C853).copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF00C853) else Color(0xFFEEEEEE)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Flag indicator (Simplified circle with text or generic icon)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when(code) {
                        "PKR" -> "🇵🇰"
                        "AED" -> "🇦🇪"
                        "SAR" -> "🇸🇦"
                        else -> "🏳️"
                    },
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = code,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = if (isSelected) Color(0xFF00C853) else Color(0xFF424242)
            )
            
            Text(
                text = prefix,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF00C853).copy(alpha = 0.7f) else Color(0xFF9E9E9E)
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val isLocked = status.equals("locked", ignoreCase = true)
    val (bg, textColor) = when {
        isLocked -> Color(0xFFFFFBEB) to Color(0xFFB45309)
        status.lowercase() in listOf("active", "approved", "fixed") -> BPGreenLight to BPGreenDark
        status.lowercase() in listOf("pending", "waiting") -> BPGoldSoft to BPGoldDark
        status.lowercase() in listOf("pending_super_admin", "pending super admin") -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        status.lowercase() in listOf("blocked", "rejected") -> Color(0xFFFFE5E5) to Color(0xFFD32F2F)
        else -> Slate100 to Slate700
    }
    val displayStatus = if (status.equals("pending_super_admin", true)) "PENDING SUPER ADMIN" else status.uppercase()
    Box(
        modifier = modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = displayStatus,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun WhatsAppHelplineButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BPGreenPrimary,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = "WhatsApp Helpline",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "WhatsApp Helpline",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        Color(0xFFE2E8F0),
        Color(0xFFF1F5F9),
        Color(0xFFE2E8F0)
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

@Composable
fun ShimmerDashboardSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(brush)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(95.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(95.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(brush)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(brush)
        )
    }
}

@Composable
fun ShimmerDepositSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
            )
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
            )
        }

        Box(
            modifier = Modifier
                .width(260.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush)
        )

        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(brush)
        )
    }
}

@Composable
fun ShimmerWithdrawSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
            )
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
        }

        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(brush)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(brush)
        )
    }
}

@Composable
fun TransactionCelebrationDialog(
    event: CelebrationEvent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.celebration_confetti)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .shadow(24.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = Color.White
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Lottie Animation Container
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        BPGreenLight,
                                        Color.White
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(180.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = event.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Amount Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BPGreenLight,
                        border = BorderStroke(1.dp, BPGreenPrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${event.currency} ${event.amount.toInt()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = BPGreenDark,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = event.subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    if (event.referenceOrDetails.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = event.referenceOrDetails,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate500,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BPGreenPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CONTINUE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}


