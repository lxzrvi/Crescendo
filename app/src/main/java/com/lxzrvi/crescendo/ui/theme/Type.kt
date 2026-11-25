package com.lxzrvi.crescendo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.R

/** Borel is reserved for the Crescendo wordmark. */
val BorelFamily = FontFamily(Font(R.font.borel))

/** SF Pro Display is the app-wide typeface. */
val SfProFamily = FontFamily(
    Font(R.font.sf_pro_display_regular, FontWeight.Normal),
    Font(R.font.sf_pro_display_medium, FontWeight.Medium),
    Font(R.font.sf_pro_display_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_display_bold, FontWeight.Bold),
)

val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = SfProFamily),
        displayMedium = base.displayMedium.copy(fontFamily = SfProFamily),
        displaySmall = base.displaySmall.copy(fontFamily = SfProFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = SfProFamily),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = SfProFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        ),
        headlineSmall = base.headlineSmall.copy(fontFamily = SfProFamily),
        titleLarge = base.titleLarge.copy(
            fontFamily = SfProFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),
        titleMedium = base.titleMedium.copy(fontFamily = SfProFamily),
        titleSmall = base.titleSmall.copy(fontFamily = SfProFamily),
        bodyLarge = base.bodyLarge.copy(
            fontFamily = SfProFamily,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.15.sp,
        ),
        bodyMedium = base.bodyMedium.copy(
            fontFamily = SfProFamily,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp,
        ),
        bodySmall = base.bodySmall.copy(fontFamily = SfProFamily),
        labelLarge = base.labelLarge.copy(
            fontFamily = SfProFamily,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
        ),
        labelMedium = base.labelMedium.copy(fontFamily = SfProFamily),
        labelSmall = base.labelSmall.copy(fontFamily = SfProFamily),
    )
}

/** Default text style for components that do not specify a typography role. */
val DefaultAppTextStyle = TextStyle(fontFamily = SfProFamily)
