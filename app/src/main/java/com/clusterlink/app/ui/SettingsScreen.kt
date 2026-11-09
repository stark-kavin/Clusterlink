package com.clusterlink.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneMissed
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Textsms
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clusterlink.app.core.AppSettings
import com.clusterlink.app.core.Updater
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.clusterlink.app.core.ThemePreset
import com.clusterlink.app.core.ThemeSettings
import com.clusterlink.app.core.VehicleBus
import com.clusterlink.app.core.VehicleStore
import com.clusterlink.app.ui.theme.BorderWidth
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.clusterlink.app.ui.theme.Radius
import com.clusterlink.app.ui.theme.Space
import com.clusterlink.app.ui.theme.Vl
import com.clusterlink.app.ui.theme.VlType

/** Settings pages. HOME lists the others; BIKE, THEME and DASHBOARD keep a live preview pinned at the top. */
private enum class SettingsPage(val title: String, val subtitle: String) {
    HOME("Settings", "How your bike and phone work together"),
    BIKE("My bike", "Name, model, colour and photo"),
    THEME("Theme", "The app's colours"),
    DASHBOARD("Dashboard", "What your bike's screen shows"),
    CONNECTION("Connection", "Keep the bike link alive"),
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onOpenTestBench: () -> Unit,
    onOpenAbout: () -> Unit,
    /** The rider's own bike photo, if set (shown on the home screen instead of the model photo). */
    photo: ImageBitmap?,
    onPickPhoto: () -> Unit,
    onResetPhoto: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.HOME) }
    fun back() { if (page != SettingsPage.HOME) page = SettingsPage.HOME else onBack() }
    BackHandler(onBack = ::back)

    val ctx = LocalContext.current
    val s by AppSettings.values.collectAsStateWithLifecycle()
    val theme by ThemeSettings.values.collectAsStateWithLifecycle()
    val vehicles by VehicleStore.vehicles.collectAsStateWithLifecycle()
    val active by VehicleBus.activeAddress.collectAsStateWithLifecycle()
    val hero = vehicles.firstOrNull { it.address.equals(active, true) } ?: vehicles.firstOrNull()
    // My bike: which saved bike is being edited (defaults to the one on the home screen).
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val editBike = vehicles.firstOrNull { it.address == editing } ?: hero
    // The custom photo belongs to the home-screen bike.
    val editPhoto = if (editBike != null && editBike.address == hero?.address) photo else null

    // Re-check system state (battery exemption, permissions) whenever we come back from a system screen.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val batteryExempt = remember(resumes) { isBatteryExempt(ctx) }
    val listenerOn = remember(resumes) { isListenerOn(ctx) }
    var note by remember { mutableStateOf<String?>(null) }

    val askSendSms = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        AppSettings.update { it.copy(rejectSms = ok) }
        note = if (ok) null else "SMS permission was denied, so Clusterlink can't text callers."
    }
    val askReceiveSms = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        AppSettings.update { it.copy(smsCounter = ok) }
        note = if (ok) null else "SMS permission was denied, so new messages can't be counted."
    }

    val bars = WindowInsets.safeDrawing.asPaddingValues()
    Box(Modifier.fillMaxSize()) {
        AuroraBackground(Vl.Volt)
        AnimatedContent(
            page,
            transitionSpec = {
                val dir = if (targetState == SettingsPage.HOME) -1 else 1
                (slideInHorizontally(tween(260)) { it / 4 * dir } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 4 * dir } + fadeOut(tween(160)))
            },
            label = "settingsPage",
        ) { p ->
            Column(Modifier.fillMaxSize().padding(top = bars.calculateTopPadding() + Space.s)) {
                // Fixed part: title, then the page's live preview (stays visible while the options scroll).
                Column(Modifier.padding(horizontal = Space.Screen)) {
                    ScreenHeader(p.title, p.subtitle, onBack = ::back)
                    when (p) {
                        SettingsPage.BIKE -> { Gap(Space.m); BikePreview(editBike, editPhoto) }
                        SettingsPage.THEME -> { Gap(Space.m); ThemePreview(hero, photo) }
                        SettingsPage.DASHBOARD -> { Gap(Space.m); DashboardPreview(s) }
                        else -> {}
                    }
                }
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = Space.Screen, end = Space.Screen,
                        top = Space.m, bottom = bars.calculateBottomPadding() + Space.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    note?.let { msg -> item { Notice(msg, Vl.Amber) } }

                    when (p) {
                        SettingsPage.HOME -> {
                            item {
                                GlassCard(padding = Space.s) {
                                    ActionRow(Icons.Rounded.TwoWheeler, "My bike",
                                        hero?.let { v -> v.bike?.let { "${v.nickname} · ${it.color.name}" } ?: v.nickname } ?: "No bike added yet",
                                        tint = Vl.Volt, trailing = null, onClick = { page = SettingsPage.BIKE })
                                    ActionRow(Icons.Rounded.Palette, "Theme",
                                        when (theme.mode) {
                                            ThemeSettings.Mode.BIKE -> "Matches your bike"
                                            ThemeSettings.Mode.PRESET -> "Preset · ${ThemePreset.of(theme.preset).displayName}"
                                            ThemeSettings.Mode.CUSTOM -> "Your own colours"
                                        },
                                        tint = Vl.Volt, trailing = null, onClick = { page = SettingsPage.THEME })
                                    ActionRow(Icons.Rounded.Speed, "Dashboard", "Calls, counters and the GPS mark on your bike",
                                        tint = Vl.Volt, trailing = null, onClick = { page = SettingsPage.DASHBOARD })
                                    val ready = batteryExempt && listenerOn
                                    ActionRow(Icons.Rounded.Link, "Connection",
                                        if (ready) "All set" else "Needs your attention",
                                        tint = if (ready) Vl.Volt else Vl.Amber, trailing = if (ready) null else "Fix",
                                        onClick = { page = SettingsPage.CONNECTION })
                                }
                            }
                            item {
                                Section("More") {
                                    UpdatesRow()
                                    ActionRow(Icons.Rounded.Science, "Test bench", "Send test screens to the bike and see the packet log",
                                        tint = Vl.Cyan, trailing = null, onClick = onOpenTestBench)
                                    ActionRow(Icons.Rounded.Info, "About Clusterlink", "Version, supported bikes, credits and licences",
                                        tint = Vl.TextDim, trailing = null, onClick = onOpenAbout)
                                }
                            }
                        }

                        SettingsPage.BIKE -> bikeOptions(vehicles, editBike, editPhoto, onSelectBike = { editing = it },
                            onPickPhoto = onPickPhoto, onResetPhoto = onResetPhoto)

                        SettingsPage.THEME -> item { ThemeSection() }

                        SettingsPage.DASHBOARD -> {
                            item {
                                Section("Calls") {
                                    ToggleRow(Icons.Rounded.Call, "Answer from bike", "The answer button picks up a ringing call",
                                        s.bikeAnswer) { v -> AppSettings.update { it.copy(bikeAnswer = v) } }
                                    ToggleRow(Icons.Rounded.CallEnd, "Reject from bike", "The reject button declines a call or hangs up",
                                        s.bikeReject) { v -> AppSettings.update { it.copy(bikeReject = v, rejectSms = it.rejectSms && v, confirmReject = it.confirmReject && v) } }
                                    ToggleRow(Icons.Rounded.TouchApp, "Confirm before rejecting",
                                        if (s.bikeReject) "First press shows “Press again 5” on the bike; press again within 5 s to reject"
                                        else "Turn on Reject from bike first",
                                        s.confirmReject, enabled = s.bikeReject) { v -> AppSettings.update { it.copy(confirmReject = v) } }
                                    ToggleRow(Icons.Rounded.Textsms, "Text the caller when you reject",
                                        if (s.bikeReject) "Sends the message below after a reject from the bike" else "Turn on Reject from bike first",
                                        s.rejectSms, enabled = s.bikeReject) { v ->
                                        if (v && !granted(ctx, android.Manifest.permission.SEND_SMS)) askSendSms.launch(android.Manifest.permission.SEND_SMS)
                                        else AppSettings.update { it.copy(rejectSms = v) }
                                    }
                                    AnimatedVisibility(s.rejectSms, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                                        VlTextField(
                                            value = s.rejectText,
                                            onValueChange = { t -> AppSettings.update { it.copy(rejectText = t.take(160)) } },
                                            label = "Message",
                                            modifier = Modifier.fillMaxWidth().padding(top = Space.s),
                                            singleLine = false,
                                        )
                                    }
                                }
                            }
                            item {
                                Section("Counters") {
                                    ToggleRow(Icons.Rounded.PhoneMissed, "Missed-call counter", "Shows how many calls you missed this ride",
                                        s.missedCalls) { v -> AppSettings.update { it.copy(missedCalls = v) } }
                                    ToggleRow(Icons.Rounded.Sms, "New SMS counter", "Shows how many texts arrived this ride. Messages aren't read.",
                                        s.smsCounter) { v ->
                                        if (v && !granted(ctx, android.Manifest.permission.RECEIVE_SMS)) askReceiveSms.launch(android.Manifest.permission.RECEIVE_SMS)
                                        else AppSettings.update { it.copy(smsCounter = v) }
                                    }
                                }
                            }
                            item {
                                Section("Navigation") {
                                    ToggleRow(Icons.Rounded.GpsOff, "GPS-lost warning", "Shows the no-signal mark on the bike when your phone loses GPS",
                                        s.gpsWarning) { v -> AppSettings.update { it.copy(gpsWarning = v) } }
                                }
                            }
                        }

                        SettingsPage.CONNECTION -> item {
                            Section("Keep the link alive") {
                                ActionRow(
                                    Icons.Rounded.BatteryAlert,
                                    "Battery optimisation",
                                    if (batteryExempt) "Allowed to run in the background" else "May be stopped in the background. Tap to allow.",
                                    tint = if (batteryExempt) Vl.Volt else Vl.Amber,
                                    trailing = if (batteryExempt) "On" else "Allow",
                                    onClick = { requestBatteryExemption(ctx) },
                                )
                                ActionRow(
                                    Icons.Rounded.NotificationsActive,
                                    "Notification access",
                                    if (listenerOn) "Music and Google Maps turns are available" else "Needed for music and Google Maps turns",
                                    tint = if (listenerOn) Vl.Volt else Vl.Amber,
                                    trailing = if (listenerOn) "On" else "Allow",
                                    onClick = onOpenNotificationAccess,
                                )
                            }
                        }
                    }
                }
            }
        }
        SystemBarScrims()
    }
}

// ---------------------------------------------------------------- my bike

/** My bike options (the preview is pinned above): which bike, nickname, model + colour, photo, forget. */
private fun LazyListScope.bikeOptions(
    vehicles: List<VehicleStore.Vehicle>,
    v: VehicleStore.Vehicle?,
    photo: ImageBitmap?,
    onSelectBike: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onResetPhoto: () -> Unit,
) {
    if (v == null) {
        item { Notice("Add your bike on the home screen first. Then you can change its name, model, colour and photo here.", Vl.Amber) }
        return
    }
    if (vehicles.size > 1) item {
        Section("Which bike") {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(Space.s),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                vehicles.forEach { o ->
                    val on = o.address == v.address
                    Box(
                        Modifier.heightIn(min = 40.dp).clip(CircleShape)
                            .background(if (on) Vl.Volt else Vl.Glass)
                            .border(BorderWidth, if (on) Vl.Volt else Vl.Stroke, CircleShape)
                            .selectable(on, role = Role.RadioButton) { onSelectBike(o.address) }
                            .padding(horizontal = Space.m),
                        contentAlignment = Alignment.Center,
                    ) { Text(o.nickname, style = VlType.Label, color = if (on) Vl.OnVolt else Vl.TextDim, maxLines = 1) }
                }
            }
        }
    }
    item { Section("Nickname") { NicknameField(v) } }
    item {
        Section("Model and colour") {
            Box(Modifier.padding(Space.s)) { BikePicker(v.bike, { VehicleStore.setBike(v.address, it) }, showPreview = false) }
        }
    }
    item {
        Section("Photo") {
            ActionRow(Icons.Rounded.AddAPhoto, "Use your own photo", "Shown on the home screen instead of the model photo",
                tint = Vl.Volt, trailing = null, onClick = onPickPhoto)
            if (photo != null) ActionRow(Icons.Rounded.Image, "Use the model photo", "Removes your own photo",
                tint = Vl.TextDim, trailing = null, onClick = onResetPhoto)
        }
    }
    item { ForgetBike(v) }
}

/** Saves as you type; the preview above updates live. */
@Composable
private fun NicknameField(v: VehicleStore.Vehicle) {
    // Reset when switching bikes, or when picking a model renames the bike automatically.
    var name by remember(v.address, v.model) { mutableStateOf(v.nickname) }
    VlTextField(
        value = name,
        onValueChange = { t -> name = t.take(40); VehicleStore.rename(v.address, name) },
        label = "Name shown in the app",
        modifier = Modifier.fillMaxWidth().padding(Space.s),
    )
}

/** "Forget this bike", with a confirmation. */
@Composable
private fun ForgetBike(v: VehicleStore.Vehicle) {
    var asking by remember { mutableStateOf(false) }
    Section("Remove") {
        ActionRow(Icons.Rounded.DeleteOutline, "Forget this bike", "Removes it from Your vehicles. You can add it again later.",
            tint = Vl.Coral, trailing = null, onClick = { asking = true })
    }
    if (asking) AlertDialog(
        onDismissRequest = { asking = false },
        containerColor = Vl.Sheet,
        title = { Text("Forget ${v.nickname}?", style = VlType.Title, color = Vl.Text) },
        text = { Text("It's removed from Your vehicles and won't reconnect by itself. To use it again, add it from the home screen.",
            style = VlType.Body, color = Vl.TextDim) },
        confirmButton = { TextButton({ asking = false; VehicleStore.forget(v.address) }) { Text("Forget", color = Vl.Coral) } },
        dismissButton = { TextButton({ asking = false }) { Text("Cancel", color = Vl.TextDim) } },
    )
}

/** Pinned preview of the bike as the home screen shows it: photo (yours, else the model's), name, colour. */
@Composable
private fun BikePreview(v: VehicleStore.Vehicle?, photo: ImageBitmap?) {
    val bike = v?.bike
    val name = v?.nickname
    GlassCard(padding = Space.m) {
        Box(Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
            FloorGlow(Vl.Volt, 0.8f, Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.8f).height(32.dp))
            val mod = Modifier.fillMaxWidth().height(124.dp)
            when {
                photo != null -> androidx.compose.foundation.Image(photo, name, mod, contentScale = ContentScale.Fit)
                bike != null -> BikePhoto(bike.color, name, mod)
                else -> Box(Modifier.size(96.dp).clip(CircleShape).background(Vl.Glass).border(BorderWidth, Vl.Stroke, CircleShape),
                    contentAlignment = Alignment.Center) { Icon(Icons.Rounded.TwoWheeler, null, tint = Vl.TextFaint, modifier = Modifier.size(52.dp)) }
            }
        }
        Gap(Space.xs)
        Text(name ?: "No bike added", style = VlType.Title, color = Vl.Text, maxLines = 1,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        // Don't repeat the model when the nickname already is the model name.
        val sub = when {
            bike == null -> "Model not set"
            bike.model.displayName == name -> bike.color.name
            else -> "${bike.model.displayName} · ${bike.color.name}"
        }
        Text(sub, style = VlType.Caption, color = Vl.TextDim, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------- theme preview

/** A small copy of the home screen in the current theme, so colour changes show right away. */
@Composable
private fun ThemePreview(hero: VehicleStore.Vehicle?, photo: ImageBitmap?) {
    val shape = RoundedCornerShape(Radius.Card)
    Box(
        Modifier.fillMaxWidth().height(210.dp).clip(shape).background(Vl.Bg).border(BorderWidth, Vl.Stroke, shape),
    ) {
        AuroraBackground(Vl.Volt, Modifier.matchParentSize())
        Column(Modifier.fillMaxSize().padding(Space.m)) {
            Eyebrow("Clusterlink", color = Vl.Volt)
            Text("Your garage", style = VlType.Title, color = Vl.Text)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                FloorGlow(Vl.Volt, 1f, Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.7f).height(24.dp))
                val bike = hero?.bike
                val mod = Modifier.fillMaxWidth(0.8f).fillMaxHeight()
                when {
                    photo != null -> androidx.compose.foundation.Image(photo, null, mod, contentScale = ContentScale.Fit)
                    bike != null -> BikePhoto(bike.color, null, mod)
                    else -> Icon(Icons.Rounded.TwoWheeler, null, tint = Vl.TextFaint, modifier = Modifier.size(52.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Row(
                    Modifier.clip(CircleShape).background(Vl.Glass)
                        .border(BorderWidth, Vl.Stroke, CircleShape).padding(horizontal = Space.s, vertical = Space.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Vl.Idle))
                    Spacer(Modifier.width(Space.xs))
                    Text("Not connected", style = VlType.Caption, color = Vl.Text)
                }
                Box(
                    Modifier.weight(1f).height(36.dp).clip(CircleShape).background(Vl.Volt),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(hero?.let { "Connect to ${it.nickname}" } ?: "Add your vehicle",
                        style = VlType.Label, color = Vl.OnVolt, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- dashboard preview

/**
 * A mock of the bike's screen with what these settings change: missed-call and SMS counters, the call screen
 * (answer / reject / "Press again" countdown) and the GPS-lost mark. Turned-off parts fade out.
 */
@Composable
private fun DashboardPreview(s: AppSettings.Values) {
    val shape = RoundedCornerShape(20.dp)
    Column {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape).background(Color(0xFF020304))
                .border(2.dp, Color(0xFF2A2F37), shape).padding(14.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("12:30", style = VlType.Label, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    HudBadge(Icons.Rounded.PhoneMissed, "3", Vl.Coral, s.missedCalls)
                    Spacer(Modifier.width(Space.s))
                    HudBadge(Icons.Rounded.Sms, "2", Vl.Cyan, s.smsCounter)
                }
                Spacer(Modifier.weight(1f))
                Text("INCOMING CALL", style = VlType.Overline, color = Color(0xFF8C96A3))
                Text("Alex", style = VlType.Title, color = Color.White)
                Gap(Space.xs)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    HudKey(Icons.Rounded.Call, "Answer", Color(0xFF3DDC84), s.bikeAnswer)
                    HudKey(Icons.Rounded.CallEnd, if (s.confirmReject) "Press again 5" else "Reject", Vl.Coral, s.bikeReject)
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Navigation, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Space.xs))
                    Text("200 m · MG Road", style = VlType.Caption, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    HudBadge(Icons.Rounded.GpsOff, null, Vl.Amber, s.gpsWarning)
                }
            }
        }
        Gap(Space.xs)
        Text("Preview. Your bike's screen looks different but shows the same things.",
            style = VlType.Caption, color = Vl.TextFaint, modifier = Modifier.padding(start = Space.xs))
    }
}

@Composable
private fun HudBadge(icon: ImageVector, text: String?, color: Color, on: Boolean) {
    val a by animateFloatAsState(if (on) 1f else 0.15f, tween(250), label = "hudBadge")
    Row(
        Modifier.alpha(a).clip(CircleShape).background(color.copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        if (text != null) { Spacer(Modifier.width(3.dp)); Text(text, style = VlType.Caption, color = Color.White) }
    }
}

@Composable
private fun HudKey(icon: ImageVector, label: String, color: Color, on: Boolean) {
    val a by animateFloatAsState(if (on) 1f else 0.15f, tween(250), label = "hudKey")
    Row(
        Modifier.alpha(a).clip(CircleShape).border(BorderWidth, color, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = VlType.Caption, color = Color.White, maxLines = 1)
    }
}

// ---------------------------------------------------------------- theme

/** Settings → Theme: match the bike, a preset, or custom colours; plus the background tint switch. */
@Composable
private fun ThemeSection() {
    val t by ThemeSettings.values.collectAsStateWithLifecycle()
    val vehicles by VehicleStore.vehicles.collectAsStateWithLifecycle()
    val active by VehicleBus.activeAddress.collectAsStateWithLifecycle()
    val bike = (vehicles.firstOrNull { it.address.equals(active, true) } ?: vehicles.firstOrNull())?.bike
    Section("Colours") {
        Column(Modifier.padding(Space.s).animateContentSize()) {
            ModeTabs(t.mode) { m -> ThemeSettings.update { it.copy(mode = m) } }
            Gap(Space.m)
            when (t.mode) {
                ThemeSettings.Mode.BIKE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    TwoToneDot(Vl.Volt, Vl.Volt2, 24.dp)
                    Spacer(Modifier.width(Space.s))
                    Text(
                        if (bike != null) "Colours from your ${bike.model.displayName} · ${bike.color.name}"
                        else "Pick your bike's model (the ⋮ menu on a saved bike) to use its colours. " +
                            "Until then: ${ThemePreset.DEFAULT.displayName}.",
                        style = VlType.Caption, color = Vl.TextDim,
                    )
                }
                ThemeSettings.Mode.PRESET -> PresetGrid(t.preset) { id -> ThemeSettings.update { it.copy(preset = id) } }
                ThemeSettings.Mode.CUSTOM -> {
                    ColorPicker("Main colour", t.customAccent) { c -> ThemeSettings.update { it.copy(customAccent = c) } }
                    Gap(Space.m)
                    ColorPicker("Second colour", t.customSecond) { c -> ThemeSettings.update { it.copy(customSecond = c) } }
                }
            }
        }
        ToggleRow(Icons.Rounded.Palette, "Tint the background", "Colours the background, cards and sheets with the theme",
            t.tintBackground) { v -> ThemeSettings.update { it.copy(tintBackground = v) } }
    }
}

@Composable
private fun ModeTabs(mode: ThemeSettings.Mode, onSelect: (ThemeSettings.Mode) -> Unit) {
    val labels = listOf(
        ThemeSettings.Mode.BIKE to "Match my bike",
        ThemeSettings.Mode.PRESET to "Presets",
        ThemeSettings.Mode.CUSTOM to "Custom",
    )
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(Vl.Glass).padding(Space.xs),
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        labels.forEach { (m, label) ->
            val on = m == mode
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp).clip(CircleShape)
                    .background(if (on) Vl.Volt else Color.Transparent)
                    .selectable(on, role = Role.Tab) { onSelect(m) },
                contentAlignment = Alignment.Center,
            ) { Text(label, style = VlType.Label, color = if (on) Vl.OnVolt else Vl.TextDim, maxLines = 1) }
        }
    }
}

@Composable
private fun PresetGrid(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
        ThemePreset.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                row.forEach { p ->
                    val on = p.id == selected
                    val accent = Color(p.accent)
                    Row(
                        Modifier.weight(1f).heightIn(min = Space.Touch).clip(RoundedCornerShape(Radius.Tile))
                            .background(if (on) accent.copy(alpha = 0.14f) else Vl.Glass)
                            .border(if (on) 2.dp else BorderWidth, if (on) accent else Vl.Stroke, RoundedCornerShape(Radius.Tile))
                            .selectable(on, role = Role.RadioButton) { onSelect(p.id) }
                            .padding(horizontal = Space.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TwoToneDot(accent, Color(p.second), 22.dp)
                        Spacer(Modifier.width(Space.s))
                        Text(p.displayName, style = VlType.Label, color = if (on) Vl.Text else Vl.TextDim, maxLines = 1)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private val Swatches = listOf(
    0xFFF2F4F7, 0xFFCFD6DF, 0xFFA3ABB5, 0xFFFF3B4A, 0xFFFF8A3D, 0xFFFFC814, 0xFFE4F23C,
    0xFF3DDC84, 0xFF2DD4BF, 0xFF5CE1FF, 0xFF4C82FF, 0xFFA78BFA, 0xFFF472B6, 0xFFE8B44C,
)

/** Swatches for quick picks, plus a hue slider to fine-tune. */
@Composable
private fun ColorPicker(label: String, value: Long, onChange: (Long) -> Unit) {
    val current = Color(value)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = VlType.Label, color = Vl.Text, modifier = Modifier.weight(1f))
        Box(Modifier.size(22.dp).clip(CircleShape).background(current).border(BorderWidth, Vl.Stroke, CircleShape))
    }
    Gap(Space.s)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Swatches.forEach { sw ->
            val on = sw == value
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Color(sw))
                    .border(if (on) 3.dp else BorderWidth, if (on) Vl.Text else Vl.Stroke, CircleShape)
                    .selectable(on, role = Role.RadioButton) { onChange(sw) },
            )
        }
    }
    Gap(Space.xs)
    val hue = remember(value) { FloatArray(3).also { android.graphics.Color.colorToHSV(value.toInt(), it) }[0] }
    Box(Modifier.fillMaxWidth().height(Space.Touch), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(8.dp).clip(CircleShape)
                .background(Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 0.72f, 1f) })),
        )
        Slider(
            value = hue, valueRange = 0f..359f,
            onValueChange = { h ->
                onChange(android.graphics.Color.HSVToColor(floatArrayOf(h, 0.72f, 1f)).toLong() and 0xFFFFFFFFL)
            },
            colors = SliderDefaults.colors(
                thumbColor = current, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TwoToneDot(a: Color, b: Color, size: androidx.compose.ui.unit.Dp) {
    Row(Modifier.size(size).clip(CircleShape).border(BorderWidth, Vl.Stroke, CircleShape)) {
        Box(Modifier.weight(1f).fillMaxHeight().background(a))
        Box(Modifier.weight(1f).fillMaxHeight().background(b))
    }
}

// ---------------------------------------------------------------- building blocks (shared with the test bench)

@Composable
fun ScreenHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Space.Touch, filled = false, onClick = onBack)
        Spacer(Modifier.width(Space.m))
        Column {
            Text(title, style = VlType.Title, color = Vl.Text)
            Text(subtitle, style = VlType.Caption, color = Vl.TextDim)
        }
    }
}

@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Eyebrow(title, modifier = Modifier.padding(start = Space.xs, bottom = Space.s))
        GlassCard(padding = Space.s) { content() }
    }
}

@Composable
fun Notice(text: String, color: androidx.compose.ui.graphics.Color) {
    GlassCard(accent = color, padding = Space.m) { Text(text, style = VlType.Label, color = Vl.Text) }
}

@Composable
private fun ToggleRow(
    icon: ImageVector, title: String, subtitle: String, checked: Boolean, enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = Space.s, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, if (checked && enabled) Vl.Volt else Vl.TextDim, size = Space.Touch - Space.s)
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = VlType.Body, color = if (enabled) Vl.Text else Vl.TextFaint)
            Text(subtitle, style = VlType.Caption, color = if (enabled) Vl.TextDim else Vl.TextFaint)
        }
        Spacer(Modifier.width(Space.s))
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Vl.OnVolt, checkedTrackColor = Vl.Volt,
                uncheckedThumbColor = Vl.TextDim, uncheckedTrackColor = Vl.GlassStrong, uncheckedBorderColor = Vl.Stroke,
            ),
        )
    }
}

@Composable
fun ActionRow(
    icon: ImageVector, title: String, subtitle: String, tint: androidx.compose.ui.graphics.Color,
    trailing: String?, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick)
            .padding(horizontal = Space.s, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint, size = Space.Touch - Space.s)
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = VlType.Body, color = Vl.Text)
            Text(subtitle, style = VlType.Caption, color = Vl.TextDim)
        }
        if (trailing != null) Text(trailing, style = VlType.Label, color = tint)
        Icon(Icons.Rounded.ChevronRight, null, tint = Vl.TextFaint)
    }
}

@Composable
fun VlTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = singleLine,
        modifier = modifier, shape = RoundedCornerShape(Radius.Tile), textStyle = VlType.Body,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Vl.Volt, unfocusedBorderColor = Vl.Stroke, cursorColor = Vl.Volt,
            focusedLabelColor = Vl.Volt, unfocusedLabelColor = Vl.TextDim,
            focusedTextColor = Vl.Text, unfocusedTextColor = Vl.Text,
        ),
    )
}

// ---------------------------------------------------------------- system helpers

private fun granted(ctx: Context, perm: String) =
    ctx.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED

fun isBatteryExempt(ctx: Context): Boolean =
    (ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isIgnoringBatteryOptimizations(ctx.packageName) == true

/** Ask Android to stop battery-optimising Clusterlink (system dialog); falls back to the settings list. */
@SuppressLint("BatteryLife")
fun requestBatteryExemption(ctx: Context) {
    if (isBatteryExempt(ctx)) {
        runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        return
    }
    val ask = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName))
    runCatching { ctx.startActivity(ask) }
        .onFailure { runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } }
}

private fun isListenerOn(ctx: Context): Boolean =
    Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")?.contains(ctx.packageName) == true

// ---------------------------------------------------------------- updates

/** Settings → More → App updates: current version and update state; tap to check GitHub or open the update. */
@Composable
private fun UpdatesRow() {
    val ctx = LocalContext.current
    val state by Updater.state.collectAsStateWithLifecycle()
    val version = remember { Updater.installedVersion(ctx) }
    val (subtitle, available) = when (val s = state) {
        is Updater.State.Checking -> "Checking GitHub…" to false
        is Updater.State.UpToDate -> "Version $version · Up to date" to false
        is Updater.State.Available -> "Version ${s.release.version} is available" to true
        is Updater.State.Downloading -> "Downloading… ${if (s.total > 0) s.bytes * 100 / s.total else 0}%" to true
        is Updater.State.Installing -> "Installing…" to true
        is Updater.State.NeedsPermission -> "Needs one permission to install" to true
        is Updater.State.Failed -> s.message to (s.release != null)
        else -> "Version $version · Tap to check" to false
    }
    ActionRow(
        Icons.Rounded.SystemUpdate, "App updates", subtitle,
        tint = if (available) Vl.Volt else Vl.TextDim,
        trailing = if (state is Updater.State.Available) "Update" else null,
        onClick = { if (available) Updater.showDialog() else Updater.check(ctx, manual = true) },
    )
}
