package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.R
import com.example.ui.theme.*

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
                elevation = 12.dp,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = Color(0xFF10B981),
                ambientColor = Color(0xFF059669)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6EF3C2), // Bright mint green top-left
                        Color(0xFF34DA9B), // Vibrant mint green middle
                        Color(0xFF23C885)  // Rich mint green bottom-right
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val bPath = Path().apply {
                fillType = PathFillType.EvenOdd

                // Outer contour of bold sleek italic 'b'
                moveTo(0.26f * w, 0.77f * h)
                cubicTo(0.30f * w, 0.70f * h, 0.44f * w, 0.38f * h, 0.52f * w, 0.20f * h)
                cubicTo(0.54f * w, 0.16f * h, 0.59f * w, 0.18f * h, 0.58f * w, 0.23f * h)
                cubicTo(0.54f * w, 0.32f * h, 0.50f * w, 0.40f * h, 0.47f * w, 0.45f * h)
                cubicTo(0.62f * w, 0.38f * h, 0.77f * w, 0.42f * h, 0.80f * w, 0.56f * h)
                cubicTo(0.83f * w, 0.69f * h, 0.73f * w, 0.78f * h, 0.58f * w, 0.78f * h)
                cubicTo(0.44f * w, 0.78f * h, 0.32f * w, 0.81f * h, 0.26f * w, 0.77f * h)
                close()

                // Inner hole (counter) contour of 'b'
                moveTo(0.58f * w, 0.47f * h)
                cubicTo(0.68f * w, 0.47f * h, 0.71f * w, 0.60f * h, 0.61f * w, 0.67f * h)
                cubicTo(0.53f * w, 0.72f * h, 0.45f * w, 0.66f * h, 0.46f * w, 0.57f * h)
                cubicTo(0.47f * w, 0.50f * h, 0.51f * w, 0.47f * h, 0.58f * w, 0.47f * h)
                close()
            }

            // Draw subtle shadow behind the 'b' (like in the image)
            val shadowOffset = (sizeDp * 0.035f).dp.toPx()
            translate(left = shadowOffset, top = shadowOffset * 1.5f) {
                drawPath(
                    path = bPath,
                    color = Color.Black.copy(alpha = 0.22f)
                )
            }

            // Draw solid rich black 'b'
            drawPath(
                path = bPath,
                color = Color(0xFF111827)
            )
        }
    }
}

@Composable
fun BPWalletLogo(
    modifier: Modifier = Modifier,
    sizeDp: Int = 80,
    showSubtitle: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BPLogoIcon(sizeDp = sizeDp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "BP WALLET",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = Slate900,
            letterSpacing = 1.sp
        )
        if (showSubtitle) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "OFFICIAL WALLET & DEPOSIT SERVICE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
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
            .clip(RoundedCornerShape(50.dp))
            .background(Slate100)
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        // User Tab Button
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50.dp))
                .background(if (isUserTab) Color.White else Color.Transparent)
                .clickable { onTabChange(true) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = if (isUserTab) BPGreenDark else Slate500,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "User",
                    fontWeight = if (isUserTab) FontWeight.Bold else FontWeight.Medium,
                    color = if (isUserTab) Slate900 else Slate500,
                    fontSize = 14.sp
                )
            }
        }

        // Admin Tab Button
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50.dp))
                .background(if (!isUserTab) Color.White else Color.Transparent)
                .clickable { onTabChange(false) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Admin",
                    tint = if (!isUserTab) BPGreenDark else Slate500,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Admin",
                    fontWeight = if (!isUserTab) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isUserTab) Slate900 else Slate500,
                    fontSize = 14.sp
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
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) BPGreenPrimary else Slate200,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BPGreenPrimary else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = country,
                tint = if (isSelected) Color.White else BPGreenDark,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = code,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = if (isSelected) Color.White else Slate900
            )
            Text(
                text = prefix,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else Slate500
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, textColor) = when (status.lowercase()) {
        "active", "approved", "fixed" -> BPGreenLight to BPGreenDark
        "pending", "waiting" -> BPGoldSoft to BPGoldDark
        "blocked", "rejected" -> Color(0xFFFFE5E5) to Color(0xFFD32F2F)
        else -> Slate100 to Slate700
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.uppercase(),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
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
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BPGreenPrimary,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = "WhatsApp Helpline",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "WhatsApp Helpline",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}
