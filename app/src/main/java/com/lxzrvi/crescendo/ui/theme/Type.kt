package com.lxzrvi.crescendo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.R

/** Borel — used for the big "Crescendo" wordmark (res/font/borel.ttf). */
val BorelFamily = FontFamily(Font(R.font.borel))

/** SF Pro Display — primary app font. */
val SfProFamily = FontFamily(
    Font(R.font.sf_pro_display_regular, FontWeight.Normal),
    Font(R.font.sf_pro_display_medium, FontWeight.Medium),
    Font(R.font.sf_pro_display_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_display_bold, FontWeight.Bold),
)

val AppTypography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(
            fontFamily = SfProFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        ),
        titleLarge = base.titleLarge.copy(
            fontFamily = SfProFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),
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
        labelLarge = base.labelLarge.copy(
            fontFamily = SfProFamily,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
        ),
    )
}

/** Default text style used everywhere so every component inherits SF Pro. */
val DefaultAppTextStyle = TextStyle(
    fontFamily = SfProFamily,
    color = TextPrimary,
)
