package com.ivy.design.system

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.ui.R

/** Ivy's original type families, shared by the legacy and Material presentations. */
object JvyFonts {
    val Interface = FontFamily(
        Font(R.font.raleway_regular, FontWeight.Normal),
        Font(R.font.raleway_medium, FontWeight.Medium),
        Font(R.font.raleway_semibold, FontWeight.SemiBold),
        Font(R.font.raleway_bold, FontWeight.Bold),
        Font(R.font.raleway_extrabold, FontWeight.ExtraBold),
        Font(R.font.raleway_black, FontWeight.Black),
    )
    val Numbers = FontFamily(
        Font(R.font.opensans_regular, FontWeight.Normal),
        Font(R.font.opensans_regular, FontWeight.Medium),
        Font(R.font.opensans_semibold, FontWeight.SemiBold),
        Font(R.font.opensans_bold, FontWeight.Bold),
        Font(R.font.opensans_extrabold, FontWeight.ExtraBold),
    )
}

object JvyTypography {
    private fun text(size: Int, line: Int, weight: FontWeight) = TextStyle(
        fontFamily = JvyFonts.Interface, fontWeight = weight,
        fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp,
    )
    val PageTitle = text(28, 36, FontWeight.ExtraBold)
    val SectionTitle = text(20, 28, FontWeight.Bold)
    val Body = text(16, 24, FontWeight.Medium)
    val Supporting = text(14, 20, FontWeight.Medium)
    val Label = text(12, 16, FontWeight.SemiBold)
    val Amount = TextStyle(fontFamily = JvyFonts.Numbers, fontSize = 24.sp, lineHeight = 32.sp,
        fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum", letterSpacing = 0.sp)
    val AmountLarge = Amount.copy(fontSize = 32.sp, lineHeight = 40.sp)
    val AmountSmall = Amount.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    val Material = Typography(
        displayLarge = PageTitle, displayMedium = PageTitle, displaySmall = PageTitle,
        headlineLarge = PageTitle, headlineMedium = PageTitle, headlineSmall = text(24, 32, FontWeight.Bold),
        titleLarge = SectionTitle, titleMedium = text(18, 24, FontWeight.Bold), titleSmall = text(16, 24, FontWeight.SemiBold),
        bodyLarge = Body, bodyMedium = Body, bodySmall = Supporting,
        labelLarge = text(14, 20, FontWeight.Bold), labelMedium = Label, labelSmall = Label,
    )
}

object JvyShapes {
    val Logo = RoundedCornerShape(percent = 22)
    val Control = RoundedCornerShape(16.dp)
    val Card = RoundedCornerShape(20.dp)
    val Sheet = RoundedCornerShape(24.dp)
    val Material = Shapes(extraSmall = Control, small = Control, medium = Card, large = Sheet, extraLarge = Sheet)
}

/** One rhythm for layout ownership; don't add a second gutter inside a padded host. */
object JvySpacing {
    val Related = 8.dp
    val Group = 16.dp
    val Section = 24.dp
    val PageGutter = 24.dp
    val ModalMargin = 16.dp
    val ActionInset = 12.dp
    val TouchTarget = 48.dp
}

object JvyMotion {
    const val FeedbackMillis = 160
    const val TransitionMillis = 240
    const val ChartUpdateMillis = 360
    const val PressedScale = 0.985f
}

/** Semantic financial accents, not replacements for the user's category/account colors. */
object JvyFinanceColors {
    @Composable fun asset(): Color = MaterialTheme.colorScheme.secondary
    @Composable fun assetSurface(): Color = MaterialTheme.colorScheme.secondaryContainer
    @Composable fun debt(): Color = MaterialTheme.colorScheme.tertiary
    @Composable fun debtSurface(): Color = MaterialTheme.colorScheme.tertiaryContainer
}

/** Unchanged transparent artwork on white, with a shared proportional runtime corner mask. */
@Composable
fun JvyLogo(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.jvy_wallet_logo), "Jvy Wallet logo",
        modifier.clip(JvyShapes.Logo).background(Color.White), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
}

@Composable
fun JvyButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
              colors: ButtonColors = ButtonDefaults.buttonColors(), content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.Button(onClick, modifier.heightIn(min = 48.dp), enabled, shape = JvyShapes.Control, colors = colors, content = content)
}

@Composable
fun JvyOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                      colors: ButtonColors = ButtonDefaults.outlinedButtonColors(), content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled, shape = JvyShapes.Control, colors = colors, content = content)
}

@Composable
fun JvyTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                  content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.TextButton(onClick, modifier.heightIn(min = 48.dp), enabled, shape = JvyShapes.Control, content = content)
}
