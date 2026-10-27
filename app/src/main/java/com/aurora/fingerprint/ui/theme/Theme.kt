package com.aurora.fingerprint.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DeepNavy = Color(0xFF07111F)
val CardNavy = Color(0xFF102238)
val Electric = Color(0xFF4DA9FF)
val Mint = Color(0xFF36D8A1)
val Muted = Color(0xFF99AEC7)
val Amber = Color(0xFFFFCB74)

private val dark = darkColorScheme(
    primary = Electric,
    onPrimary = DeepNavy,
    secondary = Mint,
    background = DeepNavy,
    surface = CardNavy,
    onSurface = Color(0xFFF3F7FF),
    onBackground = Color(0xFFF3F7FF),
    surfaceVariant = Color(0xFF182D46),
    onSurfaceVariant = Muted,
    error = Color(0xFFFF9096)
)
private val light = lightColorScheme(
    primary = Color(0xFF0865BC),
    onPrimary = Color.White,
    secondary = Color(0xFF087E65),
    background = Color(0xFFF1F6FC),
    surface = Color.White,
    onSurface = Color(0xFF11233D),
    onBackground = Color(0xFF11233D),
    surfaceVariant = Color(0xFFE1ECF7),
    onSurfaceVariant = Color(0xFF4D627C),
    error = Color(0xFFC52C48)
)
private val text = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

@Composable
fun FingerprintTheme(lightMode: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (lightMode) light else dark, typography = text, content = content)
}
