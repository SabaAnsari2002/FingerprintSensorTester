package com.aurora.fingerprint.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.fingerprint.ui.theme.Electric

@Composable
fun AppScaffold(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(scheme.background, scheme.background, scheme.surface.copy(alpha = .82f)))
        ), contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 620.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                content()
            }
        }
    }
}

@Composable
fun Header(eyebrow: String, lightMode: Boolean, onToggleTheme: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = .12f), shape = RoundedCornerShape(13.dp)) {
            Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(12.dp).size(23.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("FINGERPRINT", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(eyebrow, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
        }
        IconButton(onClick = onToggleTheme) {
            Icon(if (lightMode) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                contentDescription = if (lightMode) "Switch to dark mode" else "Switch to light mode",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun GlowScanner(active: Boolean, success: Boolean = false, modifier: Modifier = Modifier) {
    // Stop the animation entirely when not waiting for the Android system prompt.
    val scale: Float
    val ringOpacity: Float
    if (active) {
        val infinite = rememberInfiniteTransition(label = "visual feedback only")
        val pulse = infinite.animateFloat(
            initialValue = .90f, targetValue = 1.07f,
            animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse), label = "glow"
        )
        val alpha = infinite.animateFloat(
            initialValue = .25f, targetValue = .62f,
            animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "brightness"
        )
        scale = pulse.value
        ringOpacity = alpha.value
    } else {
        scale = 1f
        ringOpacity = .24f
    }
    val accent = if (success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Box(modifier.size(232.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(214.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(1.dp, accent.copy(alpha = ringOpacity), CircleShape))
        Box(Modifier.size(181.dp).border(1.dp, accent.copy(alpha = .22f), CircleShape))
        Box(Modifier.size(150.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(accent.copy(alpha = .19f), accent.copy(alpha = .025f))))
            .border(1.dp, accent.copy(alpha = .38f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(if (success) Icons.Outlined.CheckCircle else Icons.Outlined.Fingerprint,
                contentDescription = if (success) "Authentication succeeded" else "Decorative fingerprint illustration",
                tint = accent, modifier = Modifier.size(83.dp))
        }
    }
}

@Composable
fun Tag(text: String, tint: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = RoundedCornerShape(100.dp), color = tint.copy(alpha = .12f),
        border = BorderStroke(1.dp, tint.copy(alpha = .26f))) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = .6.sp)
    }
}

@Composable
fun InfoCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .07f))) {
        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, tint: Color? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = tint ?: MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MainButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .35f))) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CenteredIntro(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(9.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
fun PrivacyFooter() {
    Spacer(Modifier.height(28.dp))
    Text("SECURE  •  ON-DEVICE  •  NO INTERNET",
        textAlign = TextAlign.Center, fontSize = 10.sp, letterSpacing = 1.25.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(16.dp))
}
