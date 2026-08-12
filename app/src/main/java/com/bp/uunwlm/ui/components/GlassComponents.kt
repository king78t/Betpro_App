package com.bp.uunwlm.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bp.uunwlm.ui.theme.*

fun Modifier.glassEffect(
    cornerRadius: Int = 28,
    opacity: Float = 0.85f,
    borderColor: Color = Color.White
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(Color.White.copy(alpha = opacity))
    .border(
        width = 1.dp,
        color = borderColor.copy(alpha = 0.8f),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 16.dp,
    spotColor: Color = Color(0xFF22C55E).copy(alpha = 0.20f),
    borderColor: Color = Color.White,
    containerColor: Color = Color.White.copy(alpha = 0.94f),
    animateFloating: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_float")
    val offsetY by if (animateFloating) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -6f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "card_y"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Surface(
        modifier = modifier
            .offset(y = offsetY.dp)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = spotColor,
                ambientColor = Color(0xFF64748B).copy(alpha = 0.10f)
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    gradientColors: List<Color> = listOf(Color(0xFF22C55E), Color(0xFF16A34A)),
    glowColor: Color = Color(0xFF22C55E).copy(alpha = 0.45f)
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(56.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = glowColor,
                ambientColor = Color(0xFF16A34A).copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0xFFCBD5E1)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(colors = gradientColors)
                    } else {
                        Brush.horizontalGradient(colors = listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Slate500, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Slate900,
            unfocusedTextColor = Slate900,
            focusedBorderColor = BPGreenPrimary,
            unfocusedBorderColor = Slate200,
            errorBorderColor = Color(0xFFEF4444),
            cursorColor = BPGreenPrimary,
            focusedContainerColor = Color(0xFFF8FAFC),
            unfocusedContainerColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(16.dp),
        isError = isError,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation
    )
}

@Composable
fun FloatingBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_bg")
    
    val float1Y by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = -120f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "f1"
    )

    val float2Y by infiniteTransition.animateFloat(
        initialValue = 80f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "f2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8FAFC),
                        Color(0xFFF1F5F9)
                    )
                )
            )
    ) {
        // Soft Mint Top-Start Circle Glow
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.TopStart)
                .offset(x = (-80).dp, y = float1Y.dp)
                .clip(CircleShape)
                .background(Color(0xFFDCFCE7).copy(alpha = 0.70f))
                .blur(80.dp)
        )

        // Soft Emerald Bottom-End Circle Glow
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = float2Y.dp)
                .clip(CircleShape)
                .background(Color(0xFFECFDF5).copy(alpha = 0.85f))
                .blur(70.dp)
        )

        // Subtle Gold Accent Light Glow (Center-Right)
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 60.dp, y = (-50).dp)
                .clip(CircleShape)
                .background(Color(0xFFFEF3C7).copy(alpha = 0.45f))
                .blur(90.dp)
        )
    }
}
