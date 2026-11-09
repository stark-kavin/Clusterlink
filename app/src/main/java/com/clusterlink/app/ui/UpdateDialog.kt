package com.clusterlink.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clusterlink.app.R
import com.clusterlink.app.core.Updater
import com.clusterlink.app.core.Updater.State
import com.clusterlink.app.ui.theme.BorderWidth
import com.clusterlink.app.ui.theme.Space
import com.clusterlink.app.ui.theme.Vl
import com.clusterlink.app.ui.theme.VlType

/**
 * "Update available" dialog. Tapping Update turns it into a download screen (percent, progress bar, MB and
 * speed), then "Installing", then Android's own confirm prompt. Lives above every screen (MainActivity).
 */
@Composable
fun UpdateDialog() {
    val ctx = LocalContext.current
    val show by Updater.dialog.collectAsStateWithLifecycle()
    val state by Updater.state.collectAsStateWithLifecycle()
    val release = when (val s = state) {
        is State.Available -> s.release
        is State.Downloading -> s.release
        is State.NeedsPermission -> s.release
        is State.Installing -> s.release
        is State.Failed -> s.release
        else -> null
    }
    // Coming back from "Install unknown apps": carry on installing.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (state is State.NeedsPermission && ctx.packageManager.canRequestPackageInstalls()) Updater.retryInstall(ctx)
    }
    if (!show || release == null) return

    val busy = state is State.Downloading || state is State.Installing
    val installed = remember { Updater.installedVersion(ctx) }
    Dialog(
        onDismissRequest = { if (state is State.Available) Updater.later(ctx) else if (!busy) Updater.dismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy, usePlatformDefaultWidth = false),
    ) {
        BlurBehind()
        Column(
            Modifier.padding(horizontal = Space.l).fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)).background(Vl.Sheet)
                .border(BorderWidth, Vl.Stroke, RoundedCornerShape(28.dp))
                .padding(Space.l),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.logo_mark), null, Modifier.size(44.dp))
                Spacer(Modifier.width(Space.m))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (state) {
                            is State.Downloading -> "Downloading update"
                            is State.Installing -> "Installing update"
                            is State.NeedsPermission -> "One more step"
                            is State.Failed -> "Update didn't finish"
                            else -> "Update available"
                        },
                        style = VlType.Title, color = Vl.Text,
                    )
                    Text("v$installed  →  v${release.version}", style = VlType.Label, color = Vl.Volt)
                }
            }
            Gap(Space.l)

            AnimatedContent(
                state::class,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "updateStep",
            ) { _ ->
                when (val s = state) {
                    is State.Available -> Column {
                        Text("What's new", style = VlType.Overline, color = Vl.TextDim)
                        Gap(Space.s)
                        Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                            Text(s.release.notes.ifBlank { "Fixes and improvements." }, style = VlType.Body, color = Vl.Text)
                        }
                        Gap(Space.l)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton({ Updater.later(ctx) }) { Text("Later", style = VlType.Label, color = Vl.TextDim) }
                            Spacer(Modifier.width(Space.s))
                            PrimaryAction("Update", PrimaryKind.Go, Icons.Rounded.Download, { Updater.start(ctx, s.release) }, Modifier.weight(1f))
                        }
                    }

                    is State.Downloading -> Column {
                        val fraction = if (s.total > 0) s.bytes.toFloat() / s.total else 0f
                        val shown by animateFloatAsState(fraction, tween(250), label = "updPct")
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${(shown * 100).toInt()}", style = VlType.Display, color = Vl.Text)
                            Text("%", style = VlType.Title, color = Vl.TextDim, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
                            Spacer(Modifier.weight(1f))
                            Text(speed(s.bytesPerSec), style = VlType.Label, color = Vl.Volt, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Gap(Space.s)
                        UpdateProgressBar(shown)
                        Gap(Space.s)
                        Row {
                            Text("${mb(s.bytes)} of ${mb(s.total)}", style = VlType.Caption, color = Vl.TextDim)
                            Spacer(Modifier.weight(1f))
                            eta(s)?.let { Text(it, style = VlType.Caption, color = Vl.TextDim) }
                        }
                        Gap(Space.m)
                        TextButton({ Updater.cancel() }, Modifier.align(Alignment.End)) {
                            Text("Cancel", style = VlType.Label, color = Vl.TextDim)
                        }
                    }

                    is State.Installing -> Column {
                        Text("Almost done", style = VlType.Display, color = Vl.Text)
                        Gap(Space.s)
                        UpdateProgressBar(null)
                        Gap(Space.s)
                        Text("Confirm in the Android prompt. Clusterlink restarts on the new version.",
                            style = VlType.Caption, color = Vl.TextDim)
                    }

                    is State.NeedsPermission -> Column {
                        Text("Allow Clusterlink to install its updates. Android asks for this only once.",
                            style = VlType.Body, color = Vl.Text)
                        Gap(Space.l)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton({ Updater.dismiss() }) { Text("Later", style = VlType.Label, color = Vl.TextDim) }
                            Spacer(Modifier.width(Space.s))
                            PrimaryAction("Allow", PrimaryKind.Go, Icons.Rounded.Settings, {
                                runCatching {
                                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.packageName)))
                                }
                            }, Modifier.weight(1f))
                        }
                    }

                    is State.Failed -> Column {
                        Text(s.message, style = VlType.Body, color = Vl.Coral)
                        Gap(Space.l)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton({ Updater.dismiss() }) { Text("Close", style = VlType.Label, color = Vl.TextDim) }
                            Spacer(Modifier.width(Space.s))
                            s.release?.let { r ->
                                PrimaryAction("Try again", PrimaryKind.Go, Icons.Rounded.Refresh, { Updater.start(ctx, r) }, Modifier.weight(1f))
                            }
                        }
                    }

                    else -> Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}

/**
 * Rounded progress bar in the theme colours: a soft glow under the fill and a light sweep moving across it.
 * [fraction] null = indeterminate (installing).
 */
@Composable
private fun UpdateProgressBar(fraction: Float?) {
    val t = rememberInfiniteTransition(label = "updBar")
    val sweep by t.animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "sweep")
    val pulse by t.animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val f = fraction?.coerceIn(0f, 1f) ?: 1f
    val fill = Brush.horizontalGradient(listOf(Vl.Volt.copy(alpha = 0.55f), Vl.Volt))
    Box(Modifier.fillMaxWidth().height(14.dp)) {
        // Glow under the filled part (Android 12+; plain fill on older versions).
        if (f > 0f) Box(
            Modifier.fillMaxWidth(f).fillMaxHeight().padding(vertical = 2.dp).blur(10.dp)
                .background(Vl.Volt.copy(alpha = if (fraction == null) pulse * 0.5f else 0.45f), CircleShape),
        )
        Box(Modifier.fillMaxWidth().fillMaxHeight().clip(CircleShape).background(Vl.Glass).border(BorderWidth, Vl.Stroke, CircleShape)) {
            if (f > 0f) Box(
                Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape)
                    .background(if (fraction == null) Brush.horizontalGradient(listOf(Vl.Volt.copy(alpha = pulse * 0.6f), Vl.Volt.copy(alpha = pulse))) else fill)
                    .drawWithContent {
                        drawContent()
                        val x = size.width * sweep
                        val band = size.width * 0.25f
                        drawRect(Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                            startX = x - band, endX = x + band,
                        ), topLeft = Offset.Zero, size = size)
                    },
            )
        }
    }
}

/** Blur and dim the app behind the dialog (blur needs Android 12+). */
@Composable
private fun BlurBehind() {
    val view = LocalView.current
    SideEffect {
        val w = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        w.setDimAmount(0.55f)
        if (Build.VERSION.SDK_INT >= 31) {
            w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            w.attributes = w.attributes.apply { blurBehindRadius = 48 }
        }
    }
}

private fun mb(bytes: Long): String = if (bytes <= 0) "–" else "%.1f MB".format(bytes / 1_048_576.0)

private fun speed(bps: Long): String = when {
    bps <= 0 -> ""
    bps >= 1_048_576 -> "%.1f MB/s".format(bps / 1_048_576.0)
    else -> "${bps / 1024} KB/s"
}

private fun eta(s: State.Downloading): String? {
    if (s.bytesPerSec <= 0 || s.total <= 0) return null
    val left = ((s.total - s.bytes) / s.bytesPerSec).coerceAtLeast(0)
    return if (left < 60) "${left}s left" else "${left / 60} min left"
}
