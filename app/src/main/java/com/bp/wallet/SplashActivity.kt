package com.bp.wallet

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bp.wallet.data.BPWalletRepository
import com.bp.wallet.ui.theme.*
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            BPWalletRepository.initContext(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.e("SplashActivity", "Error initializing repository", e)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var startAnimation by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (startAnimation) 1f else 0.6f,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    label = "scale"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (startAnimation) 1f else 0f,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    label = "alpha"
                )

                LaunchedEffect(Unit) {
                    startAnimation = true
                    delay(1200)
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF022C19), // Dark Emerald Green
                                    Color(0xFF06140D), // Dark Forest
                                    Color(0xFF000000)  // Obsidian Black
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .scale(scale)
                            .alpha(alpha)
                    ) {
                        // Center: Circular app logo with green glow/shadow effect
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .shadow(
                                    elevation = 24.dp,
                                    shape = CircleShape,
                                    spotColor = Color(0xFF00E676),
                                    ambientColor = Color(0xFF00E676)
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF00E676), Color(0xFF00C853), Color(0xFF01579B))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "BP Wallet Logo",
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Title: "BP" in white, "Wallet" in bright green (#00E676)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "BP ",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Wallet",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Badge: "Trusted" with shield + checkmark icon
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF00E676).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Trusted",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Tagline Box: Glass-morphism card with 👑 "Imandari Ek Mehnga Shok Hai Har Kisi Ke Bas ka Ni"
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "👑 Imandari Ek Mehnga Shok Hai Har Kisi Ke Bas ka Ni",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Bottom: Green horizontal progress bar + "Loading Secure Wallet..." text
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF00E676),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading Secure Wallet...",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

