package com.clusterlink.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clusterlink.app.R
import com.clusterlink.app.core.ThemePreset
import com.clusterlink.app.core.ThemeSettings
import com.clusterlink.app.core.VehicleBus
import com.clusterlink.app.core.VehicleStore

/**
 * The app's colours. The theme colours (accent "volt", second colour, and the tinted background, cards and
 * sheets) come from a [Palette] built from the current bike's colour edition, a preset, or the rider's custom
 * colours (Settings → Theme). Fixed meaning colours stay the same in every theme: amber = searching,
 * coral = calls, cyan = navigation.
 */
@Immutable
data class Palette(
    val accent: Color,
    val onAccent: Color,
    val second: Color,
    val bg: Color,
    val bgRaised: Color,
    /** Bottom sheets, dialogs and menus. */
    val sheet: Color,
) {
    companion object {
        /** Builds the full palette from two colours. [tint] = colour the background/cards with the accent. */
        fun of(accent: Color, second: Color, tint: Boolean): Palette {
            fun shade(base: Long, amount: Float) =
                if (tint) accent.copy(alpha = amount).compositeOver(Color(base)) else Color(base)
            return Palette(
                accent = accent,
                onAccent = onColor(accent),
                second = second,
                bg = shade(0xFF05060A, 0.10f),
                bgRaised = shade(0xFF0C0E13, 0.12f),
                sheet = shade(0xFF12151B, 0.10f),
            )
        }

        /** Dark or white text on [c], whichever has more contrast. */
        fun onColor(c: Color): Color = if (c.luminance() > 0.18f) Color(0xFF0B0D10) else Color.White
    }
}

object Vl {
    private val palette = mutableStateOf(
        Palette.of(Color(ThemePreset.DEFAULT.accent), Color(ThemePreset.DEFAULT.second), tint = false)
    )
    internal fun apply(p: Palette) { palette.value = p }

    // Theme colours — read from snapshot state, so everything that reads them updates when the theme changes.
    val Bg: Color get() = palette.value.bg
    val BgRaised: Color get() = palette.value.bgRaised
    val Sheet: Color get() = palette.value.sheet
    /** Accent: connected / primary action. */
    val Volt: Color get() = palette.value.accent
    val OnVolt: Color get() = palette.value.onAccent
    /** The second theme colour; used for the second background glow. */
    val Volt2: Color get() = palette.value.second

    val Glass = Color(0x0FFFFFFF)        // card fill (white 6%, picks up the tinted background)
    val GlassStrong = Color(0x1AFFFFFF)  // pressed / nested fill
    val Stroke = Color(0x17FFFFFF)       // hairline borders

    // Text contrast on the card fill (~#121518): Text 16:1, TextDim 6.1:1, TextFaint 4.8:1 (all ≥ 4.5:1).
    val Text = Color(0xFFF2F4F7)
    val TextDim = Color(0xFF8C96A3)
    val TextFaint = Color(0xFF7A8594)
    val Decor = Color(0xFF3A424D)        // non-text only (inactive bars, tracks)

    val Amber = Color(0xFFFFB547)        // searching / connecting / warnings
    val Coral = Color(0xFFFF6B5B)        // calls
    val Cyan = Color(0xFF5CE1FF)         // navigation
    val Idle = Color(0xFF6B7684)
}

/** 8dp spacing grid. Use these instead of ad-hoc dp values. */
object Space {
    val xs = 4.dp    // half-step, only inside dense rows
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val Screen = 16.dp       // horizontal screen margin
    val Touch = 48.dp        // minimum touch target
}

/** Corner radii and borders. Cards 24, inner tiles/badges 16, chips & buttons fully round. */
object Radius {
    val Card = 24.dp
    val Tile = 16.dp
    val Small = 8.dp
}
val BorderWidth = 1.dp

/**
 * Chakra Petch (SIL OFL 1.1, bundled in res/font; licence in docs/licenses): angular, instrument-cluster
 * letterforms for headings, numbers, buttons and labels. Body and caption text stay on the system font so
 * small text remains easy to read.
 */
val ChakraPetch = FontFamily(
    Font(R.font.chakra_petch_regular, FontWeight.Normal),
    Font(R.font.chakra_petch_medium, FontWeight.Medium),
    Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
    Font(R.font.chakra_petch_bold, FontWeight.Bold),
)

/**
 * Type scale (sp): Display 28 · Title 20 · Headline-row 17 · Body 15 · Label 13 · Caption 12 · Overline 11.
 * Every Text in the home screen uses one of these.
 */
object VlType {
    val Display = TextStyle(fontFamily = ChakraPetch, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
    val Title = TextStyle(fontFamily = ChakraPetch, fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
    val RowTitle = TextStyle(fontFamily = ChakraPetch, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val Body = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    val Label = TextStyle(fontFamily = ChakraPetch, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
    val Caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
    val Overline = TextStyle(fontFamily = ChakraPetch, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.8.sp)
    val Metric = TextStyle(fontFamily = ChakraPetch, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
    val Button = TextStyle(fontFamily = ChakraPetch, fontSize = 17.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
}

private fun colorScheme(p: Palette) = darkColorScheme(
    primary = p.accent,
    onPrimary = p.onAccent,
    primaryContainer = p.accent.copy(alpha = 0.18f).compositeOver(p.bg),
    onPrimaryContainer = p.accent,
    secondary = p.second,
    background = p.bg,
    onBackground = Vl.Text,
    surface = p.bgRaised,
    onSurface = Vl.Text,
    surfaceVariant = Color.White.copy(alpha = 0.06f).compositeOver(p.bgRaised),
    onSurfaceVariant = Vl.TextDim,
    surfaceContainer = p.sheet,
    outline = Color.White.copy(alpha = 0.14f).compositeOver(p.bg),
    error = Vl.Coral,
)

private val Type = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontFamily = ChakraPetch, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp),
    )
}

/**
 * The app is dark-first on purpose: it is glanced at on a bike, often outdoors or at night.
 * Theme colours come from Settings → Theme: the bike on the home screen (the one we're linked/linking to,
 * else the most recent), a preset, or custom colours. Changes fade over 600 ms.
 */
@Composable
fun ClusterlinkTheme(content: @Composable () -> Unit) {
    val vehicles by VehicleStore.vehicles.collectAsState()
    val active by VehicleBus.activeAddress.collectAsState()
    val settings by ThemeSettings.values.collectAsState()

    // Read every state unconditionally so this recomposes when any of them changes.
    val hero = vehicles.firstOrNull { it.address.equals(active, true) } ?: vehicles.firstOrNull()
    val bikeColor = hero?.bike?.color
    val preset = ThemePreset.of(settings.preset)
    val (accentTarget, secondTarget) = when (settings.mode) {
        ThemeSettings.Mode.BIKE ->
            if (bikeColor != null) Color(bikeColor.primary) to Color(bikeColor.secondary)
            else Color(ThemePreset.DEFAULT.accent) to Color(ThemePreset.DEFAULT.second)
        ThemeSettings.Mode.PRESET -> Color(preset.accent) to Color(preset.second)
        ThemeSettings.Mode.CUSTOM -> Color(settings.customAccent) to Color(settings.customSecond)
    }
    val accent by animateColorAsState(accentTarget, tween(600), label = "accent")
    val second by animateColorAsState(secondTarget, tween(600), label = "second")
    val palette = Palette.of(accent, second, settings.tintBackground)
    SideEffect { Vl.apply(palette) }
    MaterialTheme(colorScheme = colorScheme(palette), typography = Type, content = content)
}
