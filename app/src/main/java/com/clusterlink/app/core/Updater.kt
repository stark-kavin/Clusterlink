package com.clusterlink.app.core

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-update from this repo's GitHub Releases.
 *
 * A release counts as an update when its tag (e.g. `v1.2.0`) is newer than the installed versionName and it
 * has an `.apk` asset. The APK must be signed with the same key as the installed app, or Android refuses it.
 * Flow: [check] → [State.Available] → [start] downloads with progress → Android's installer asks the rider
 * to confirm → the app restarts as the new version.
 */
object Updater {

    const val REPO = "stark-kavin/Clusterlink"
    private const val CHECK_EVERY_MS = 6 * 60 * 60 * 1000L       // automatic check at most every 6 h
    private const val SNOOZE_MS = 24 * 60 * 60 * 1000L           // "Later" hides a version for a day

    data class Release(val version: String, val notes: String, val apkUrl: String, val apkSize: Long)

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Available(val release: Release) : State
        /** [bytesPerSec] is a smoothed download speed. */
        data class Downloading(val release: Release, val bytes: Long, val total: Long, val bytesPerSec: Long) : State
        /** Android needs "Install unknown apps" allowed for Clusterlink first. */
        data class NeedsPermission(val release: Release) : State
        data class Installing(val release: Release) : State
        data class Failed(val release: Release?, val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /** Whether the update dialog is showing (the rider can hide an available update with "Later"). */
    private val _dialog = MutableStateFlow(false)
    val dialog: StateFlow<Boolean> = _dialog.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun installedVersion(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "0"

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("clusterlink_updates", Context.MODE_PRIVATE)

    /** Automatic check (app start): at most every 6 h, online only, and a snoozed version stays hidden. */
    fun checkIfDue(ctx: Context) {
        val p = prefs(ctx)
        if (System.currentTimeMillis() - p.getLong("lastCheck", 0L) < CHECK_EVERY_MS) return
        check(ctx, manual = false)
    }

    /** Checks GitHub now. [manual] = the rider asked (Settings): always shows the result. */
    fun check(ctx: Context, manual: Boolean) {
        if (job?.isActive == true) return
        val app = ctx.applicationContext
        job = scope.launch {
            if (!isOnline(app)) { if (manual) _state.value = State.Failed(null, "No internet connection"); return@launch }
            _state.value = State.Checking
            val release = runCatching { latestRelease() }.getOrElse {
                _state.value = if (manual) State.Failed(null, "Couldn't reach GitHub") else State.Idle
                return@launch
            }
            prefs(app).edit().putLong("lastCheck", System.currentTimeMillis()).apply()
            if (release == null || !isNewer(release.version, installedVersion(app))) {
                _state.value = State.UpToDate
                return@launch
            }
            _state.value = State.Available(release)
            val p = prefs(app)
            val snoozed = p.getString("snoozedVersion", null) == release.version &&
                System.currentTimeMillis() - p.getLong("snoozedAt", 0L) < SNOOZE_MS
            if (manual || !snoozed) _dialog.value = true
        }
    }

    fun showDialog() { _dialog.value = true }

    /** "Later": hide this version for a day. */
    fun later(ctx: Context) {
        (state.value as? State.Available)?.let {
            prefs(ctx).edit().putString("snoozedVersion", it.release.version).putLong("snoozedAt", System.currentTimeMillis()).apply()
        }
        _dialog.value = false
    }

    /** Close the dialog after an error or "up to date". */
    fun dismiss() {
        _dialog.value = false
        val s = state.value
        if (s is State.Failed) _state.value = s.release?.let { State.Available(it) } ?: State.Idle
    }

    /** Download the APK with progress, then hand it to Android's installer. */
    fun start(ctx: Context, release: Release) {
        if (job?.isActive == true) return
        val app = ctx.applicationContext
        job = scope.launch {
            val file = File(app.cacheDir, "update.apk")
            try {
                download(release, file)
                if (!isActive) return@launch
                install(app, release, file)
            } catch (e: Exception) {
                if (isActive) _state.value = State.Failed(release, "Download failed. Check your connection and try again.")
            }
        }
    }

    fun cancel() {
        job?.cancel()
        (state.value as? State.Downloading)?.let { _state.value = State.Available(it.release) }
    }

    /** After the rider allowed "Install unknown apps" for Clusterlink. */
    fun retryInstall(ctx: Context) {
        val s = state.value as? State.NeedsPermission ?: return
        val file = File(ctx.applicationContext.cacheDir, "update.apk")
        if (!file.exists()) { start(ctx, s.release); return }
        scope.launch { install(ctx.applicationContext, s.release, file) }
    }

    // ---------------------------------------------------------------- GitHub

    private fun latestRelease(): Release? {
        val c = open("https://api.github.com/repos/$REPO/releases/latest")
        try {
            if (c.responseCode == 404) return null          // no releases yet
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            val json = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets") ?: return null
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) } ?: return null
            return Release(
                version = json.optString("tag_name").removePrefix("v").removePrefix("V"),
                notes = json.optString("body").trim(),
                apkUrl = apk.getString("browser_download_url"),
                apkSize = apk.optLong("size"),
            )
        } finally { c.disconnect() }
    }

    private suspend fun download(release: Release, file: File) {
        val c = open(release.apkUrl)
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            val total = c.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
            var bytes = 0L
            var speed = 0.0
            var lastT = System.nanoTime(); var lastB = 0L; var lastEmit = 0L
            _state.value = State.Downloading(release, 0, total, 0)
            c.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        if (!currentCoroutineContext().isActive) return     // Cancel pressed
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        bytes += n
                        val now = System.nanoTime()
                        if (now - lastEmit > 80_000_000L) {               // ~12 updates a second
                            val dt = (now - lastT) / 1e9
                            if (dt > 0) speed = if (speed == 0.0) (bytes - lastB) / dt else speed * 0.7 + (bytes - lastB) / dt * 0.3
                            lastT = now; lastB = bytes; lastEmit = now
                            _state.value = State.Downloading(release, bytes, total, speed.toLong())
                        }
                    }
                }
            }
            if (total > 0 && bytes != total) error("Incomplete download")
            _state.value = State.Downloading(release, bytes, bytes, speed.toLong())
        } finally { c.disconnect() }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000; readTimeout = 20_000
            instanceFollowRedirects = true                          // release assets redirect to a CDN
            setRequestProperty("Accept", if ("api.github.com" in url) "application/vnd.github+json" else "application/octet-stream")
            setRequestProperty("User-Agent", "Clusterlink-Android")
        }

    // ---------------------------------------------------------------- install

    private fun install(ctx: Context, release: Release, file: File) {
        if (!ctx.packageManager.canRequestPackageInstalls()) { _state.value = State.NeedsPermission(release); return }
        _state.value = State.Installing(release)
        try {
            val installer = ctx.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(ctx.packageName)
                if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            val id = installer.createSession(params)
            installer.openSession(id).use { session ->
                session.openWrite("clusterlink.apk", 0, file.length()).use { out ->
                    file.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
                val intent = Intent(ctx, InstallResultReceiver::class.java)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                session.commit(PendingIntent.getBroadcast(ctx, id, intent, flags).intentSender)
            }
        } catch (e: Exception) {
            _state.value = State.Failed(release, "Couldn't start the install.")
        }
    }

    internal fun onInstallResult(ctx: Context, intent: Intent) {
        val release = (state.value as? State.Installing)?.release
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                else intent.getParcelableExtra(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { runCatching { ctx.startActivity(it) } }
            }
            PackageInstaller.STATUS_SUCCESS -> { _state.value = State.UpToDate; _dialog.value = false }
            PackageInstaller.STATUS_FAILURE_ABORTED ->
                _state.value = State.Failed(release, "Update cancelled.")
            PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                _state.value = State.Failed(release, "This update is signed differently from the installed app, so Android won't install it.")
            else -> _state.value = State.Failed(release,
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Install failed.")
        }
    }

    // ---------------------------------------------------------------- helpers

    /** True if [a] is a newer version than [b] ("1.10.0" > "1.9.2"; missing parts count as 0). */
    fun isNewer(a: String, b: String): Boolean {
        val x = a.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    private fun isOnline(ctx: Context): Boolean = runCatching {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }.getOrDefault(true)
}

/** Receives Android's install result for [Updater] (confirmation prompt, success, failure). */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Updater.onInstallResult(context, intent)
}
