package com.bp.wallet.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bp.wallet.model.CelebrationEvent
import com.bp.wallet.ui.theme.*

@Composable
fun BPWalletLogo(
    modifier: Modifier = Modifier,
    sizeDp: Int = 72,
    size: Dp = sizeDp.dp,
    showSubtitle: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(12.dp, CircleShape, spotColor = BPGreenPrimary.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(BPGreenPrimary, BPGreenDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "BP Wallet",
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
        }
        if (showSubtitle) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "BP WALLET",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 1.sp,
                color = Slate900
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
            .clip(RoundedCornerShape(14.dp))
            .background(Slate100)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isUserTab) Color.White else Color.Transparent)
                .clickable { onTabChange(true) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "User",
                color = if (isUserTab) Slate900 else Slate500,
                fontWeight = if (isUserTab) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(if (!isUserTab) Color.White else Color.Transparent)
                .clickable { onTabChange(false) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Admin",
                color = if (!isUserTab) Slate900 else Slate500,
                fontWeight = if (!isUserTab) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun BPTabSwitcher(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Slate100)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Slate900 else Slate500,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun CurrencyCard(
    currency: String = "",
    code: String = currency,
    prefix: String = "",
    country: String = "",
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayCurrency = code.ifBlank { currency }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) BPGreenPrimary.copy(alpha = 0.1f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) BPGreenPrimary else Slate200
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) BPGreenPrimary else Slate100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = displayCurrency,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Slate900
                    )
                    if (country.isNotBlank()) {
                        Text(
                            text = country,
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = BPGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun BPLottieLoadingView(
    size: Int = 180,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader_anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loader_scale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size((size * 0.7f).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(BPGreenPrimary, BPGreenDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size((size * 0.35f).dp)
                )
            }
            CircularProgressIndicator(
                modifier = Modifier.size(size.dp),
                color = BPGreenAccent,
                strokeWidth = 4.dp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Processing...",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun BPLoader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = BPGreenPrimary,
        strokeWidth = 3.5.dp
    )
}

@Composable
fun TransactionCelebrationDialog(
    event: CelebrationEvent,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(BPGreenPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = BPGreenPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = event.message,
                    fontSize = 14.sp,
                    color = Slate600,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BPGreenPrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
