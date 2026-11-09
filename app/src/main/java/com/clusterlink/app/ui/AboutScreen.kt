package com.clusterlink.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clusterlink.app.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import com.clusterlink.app.core.MakerProfile
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clusterlink.app.ui.theme.BorderWidth
import com.clusterlink.app.ui.theme.Space
import com.clusterlink.app.ui.theme.Vl
import com.clusterlink.app.ui.theme.VlType

private const val REPO_URL = "https://github.com/${MakerProfile.LOGIN}/Clusterlink"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val ctx = LocalContext.current
    val version = remember { versionName(ctx) }
    var showLicence by remember { mutableStateOf(false) }
    // Maker card: cached GitHub profile, refreshed online at most once a day while this page is open.
    val maker by MakerProfile.profile.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { MakerProfile.refresh(ctx) }

    val bars = WindowInsets.safeDrawing.asPaddingValues()
    Box(Modifier.fillMaxSize()) {
        AuroraBackground(Vl.Volt)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Space.Screen, end = Space.Screen,
                top = bars.calculateTopPadding() + Space.s, bottom = bars.calculateBottomPadding() + Space.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            item { ScreenHeader("About", "", onBack) }

            item {
                Column(Modifier.fillMaxWidth().padding(top = Space.l, bottom = Space.s)) {
                    // Logo: the dial mark + the slanted wordmark, "LINK" in brand red (same as docs/branding).
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.logo_mark), null, Modifier.size(52.dp))
                        Spacer(Modifier.width(Space.m))
                        Text(
                            buildAnnotatedString {
                                append("CLUSTER")
                                withStyle(SpanStyle(color = Color(0xFFE3192B))) { append("LINK") }
                            },
                            style = VlType.Display.copy(fontStyle = FontStyle.Italic), color = Vl.Text,
                            modifier = Modifier.semantics { contentDescription = "Clusterlink" },
                        )
                    }
                    Gap(Space.xs)
                    Text("Calls, music, Maps directions and phone status on your Hero TFT dashboard.",
                        style = VlType.Body, color = Vl.TextDim)
                    Gap(Space.s)
                    Text("$version · still in development · built on a Karizma XMR CE",
                        style = VlType.Caption, color = Vl.TextFaint)
                }
            }

            item {
                Eyebrow("Made by", modifier = Modifier.padding(start = Space.xs, bottom = Space.s))
                GlassCard(onClick = { open(ctx, maker.url) }, padding = Space.m) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val avatarMod = Modifier.size(56.dp).clip(CircleShape).border(BorderWidth, Vl.Stroke, CircleShape)
                        val avatar = maker.avatar
                        if (avatar != null) Image(avatar.asImageBitmap(), "${maker.name}'s GitHub profile picture",
                            avatarMod, contentScale = ContentScale.Crop)
                        else Box(avatarMod.background(Vl.Glass), contentAlignment = Alignment.Center) {
                            Text(MakerProfile.UNKNOWN, style = VlType.RowTitle, color = Vl.TextFaint)
                        }
                        Spacer(Modifier.width(Space.m))
                        Column(Modifier.weight(1f)) {
                            Text(maker.name, style = VlType.RowTitle, color = Vl.Text, maxLines = 1)
                            Text(maker.url.removePrefix("https://").removePrefix("http://"), style = VlType.Caption, color = Vl.TextDim, maxLines = 1)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = Vl.TextFaint)
                    }
                }
            }

            item {
                GlassCard(padding = Space.s) {
                    ActionRow(Icons.Rounded.Code, "Source code", REPO_URL.removePrefix("https://"),
                        tint = Vl.TextDim, trailing = null, onClick = { open(ctx, REPO_URL) })
                }
            }

            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = Space.xs, vertical = Space.s)) {
                    Eyebrow("Credits")
                    Gap(Space.s)
                    Text("Made with help from Claude (Anthropic).", style = VlType.Caption, color = Vl.TextDim)
                    Gap(Space.xs)
                    Row(Modifier.heightIn(min = Space.Touch).clickable { showLicence = true },
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Chakra Petch font, SIL Open Font License · ", style = VlType.Caption, color = Vl.TextDim)
                        Text("view licence", style = VlType.Caption, color = Vl.Volt, textDecoration = TextDecoration.Underline)
                    }
                }
            }

            item {
                Text(
                    "Not affiliated with Hero MotoCorp. Don't use the app while riding.",
                    style = VlType.Caption, color = Vl.TextFaint, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = Space.l),
                )
            }
        }
        SystemBarScrims()
    }

    if (showLicence) LicenceDialog(onDismiss = { showLicence = false })
}

@Composable
private fun LicenceDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val text = remember {
        runCatching { ctx.resources.openRawResource(R.raw.chakra_petch_ofl).bufferedReader().use { it.readText() } }
            .getOrDefault("Licence text unavailable.")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Vl.Sheet,
        title = { Text("Chakra Petch licence", style = VlType.Title, color = Vl.Text) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(text, color = Vl.TextDim, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Close", color = Vl.Volt) } },
    )
}

private fun versionName(ctx: Context): String = runCatching {
    "v" + ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
}.getOrDefault("")

private fun open(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
