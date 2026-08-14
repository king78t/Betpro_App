package com.bp.wallet.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bp.wallet.ui.theme.*

@Composable
fun FloatingBackground(
    modifier: Modifier = Modifier,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_float")
    val offsetY1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )
    val offsetY2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -25f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = BPGreenPrimary.copy(alpha = 0.08f),
                radius = 350.dp.toPx(),
                center = Offset(size.width * 0.8f, size.height * 0.15f + offsetY1)
            )
            drawCircle(
                color = BPGreenDark.copy(alpha = 0.05f),
                radius = 280.dp.toPx(),
                center = Offset(size.width * 0.1f, size.height * 0.7f + offsetY2)
            )
        }
        content?.invoke(this)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color.White,
    containerColor: Color = Color.White,
    borderColor: Color = Slate200,
    elevation: Dp = 8.dp,
    spotColor: Color = Slate900.copy(alpha = 0.08f),
    content: @Composable ColumnScope.() -> Unit
) {
    val effectiveColor = if (containerColor != Color.White) containerColor else backgroundColor
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, shape, spotColor = spotColor),
        shape = shape,
        color = effectiveColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            content()
        }
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    gradientColors: List<Color> = listOf(BPGreenPrimary, BPGreenDark),
    glowColor: Color = BPGreenPrimary.copy(alpha = 0.3f),
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .shadow(
                elevation = if (enabled && !isLoading) 6.dp else 0.dp,
                shape = shape,
                spotColor = glowColor
            ),
        shape = shape,
        color = Color.Transparent,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled && !isLoading) {
                        Brush.horizontalGradient(gradientColors)
                    } else {
                        Brush.horizontalGradient(listOf(Slate300, Slate400))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    val isPasswordTransformation = visualTransformation is PasswordVisualTransformation

    val effectiveVisualTransformation = if (isPasswordTransformation) {
        if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
    } else {
        visualTransformation
    }

    val effectiveTrailingIcon: @Composable (() -> Unit)? = when {
        trailingIcon != null -> trailingIcon
        isPasswordTransformation -> {
            {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = Slate400
                    )
                }
            }
        }
        else -> null
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = if (!label.isNullOrBlank()) {
            { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
        } else null,
        placeholder = if (!placeholder.isNullOrBlank()) {
            { Text(placeholder, color = Slate400, fontSize = 14.sp) }
        } else null,
        leadingIcon = leadingIcon,
        trailingIcon = effectiveTrailingIcon,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .height(56.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Slate900,
            unfocusedTextColor = Slate900,
            errorTextColor = Slate900,
            focusedContainerColor = Color(0xFFFFFFFF),
            unfocusedContainerColor = Color(0xFFFFFFFF),
            errorContainerColor = Color(0xFFFFFFFF),
            disabledContainerColor = Color(0xFFFFFFFF),
            focusedBorderColor = Color(0xFF22C55E),
            unfocusedBorderColor = Color(0xFFD1D5DB),
            errorBorderColor = Color(0xFFEF4444),
            cursorColor = Color(0xFF22C55E),
            errorCursorColor = Color(0xFFEF4444),
            focusedLabelColor = Color(0xFF22C55E),
            unfocusedLabelColor = Slate500,
            errorLabelColor = Color(0xFFEF4444),
            focusedLeadingIconColor = Color(0xFF22C55E),
            unfocusedLeadingIconColor = Slate400,
            errorLeadingIconColor = Color(0xFFEF4444),
            focusedTrailingIconColor = Slate400,
            unfocusedTrailingIconColor = Slate400,
            errorTrailingIconColor = Color(0xFFEF4444),
            focusedPlaceholderColor = Slate400,
            unfocusedPlaceholderColor = Slate400,
            errorPlaceholderColor = Slate400
        ),
        shape = RoundedCornerShape(14.dp),
        isError = isError,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = effectiveVisualTransformation
    )
}

@Composable
fun GlassPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Password",
    placeholder: String? = null,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = { Icon(Icons.Default.Lock, contentDescription = null, tint = Slate400) },
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    GlassTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        modifier = modifier,
        isError = isError,
        leadingIcon = leadingIcon,
        keyboardOptions = keyboardOptions,
        visualTransformation = PasswordVisualTransformation()
    )
}
