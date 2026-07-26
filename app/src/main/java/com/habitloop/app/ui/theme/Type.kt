package com.habitloop.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.habitloop.app.R

// Bundled directly as font resources rather than fetched via the
// Downloadable Fonts API — that path needs Google's Play Services
// font-provider certificate hashes to match exactly or font loading
// silently fails at runtime; bundling the real font files avoids that
// failure mode entirely.
//
// Raleway for display/headline text (distinctive, elegant at large sizes —
// this is what gives the app its own visual identity instead of looking
// like default Material). Nunito for body copy (rounded, warm, highly
// legible at small sizes — matches the friendly/encouraging tone of the
// habit-tracking domain rather than a clinical sans).
val DisplayFontFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Bold))
val BodyFontFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val BodyFontFamilyMedium = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Medium))

val HabitLoopTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.7).sp),
    headlineMedium = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 34.sp, letterSpacing = (-0.35).sp),
    headlineSmall = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = BodyFontFamilyMedium, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = BodyFontFamilyMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.35.sp)
)
