package id.ac.ub.athra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.ac.ub.athra.R

// Static instances preserve the actual Inter weight on every supported Android API.
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun athraText(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = height.sp,
)

val AthraTypography = Typography(
    displayLarge = athraText(40, 46, FontWeight.Bold),
    displayMedium = athraText(36, 42, FontWeight.Bold),
    displaySmall = athraText(32, 38, FontWeight.Bold),
    headlineLarge = athraText(26, 32, FontWeight.Bold),
    headlineMedium = athraText(24, 30, FontWeight.Bold),
    headlineSmall = athraText(18, 24, FontWeight.SemiBold),
    titleLarge = athraText(28, 34, FontWeight.Bold),
    titleMedium = athraText(16, 22, FontWeight.SemiBold),
    titleSmall = athraText(14, 20, FontWeight.SemiBold),
    bodyLarge = athraText(15, 22),
    bodyMedium = athraText(13, 19),
    bodySmall = athraText(12, 17),
    labelLarge = athraText(15, 20, FontWeight.SemiBold),
    labelMedium = athraText(12, 16, FontWeight.Medium),
    labelSmall = athraText(10, 14, FontWeight.Medium),
)
