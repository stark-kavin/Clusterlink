package com.clusterlink.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The maker card on the About screen: name, GitHub handle and profile picture, read from GitHub's public
 * API. Fetched at most once a day, only while the About screen is open and the phone is online. The last
 * result is cached (prefs + a file); until the first fetch succeeds only the link is shown.
 */
object MakerProfile {

    /** The GitHub account the card points to. Only this is fixed; everything else is loaded. */
    const val LOGIN = "stark-kavin"
    private const val REFRESH_MS = 24 * 60 * 60 * 1000L

    data class Profile(
        val name: String,
        val login: String,
        val url: String,
        /** Null → not loaded yet; the card shows "-". */
        val avatar: Bitmap? = null,
    )

    /** Before the first successful fetch: only the link is known; name and picture show "-". */
    const val UNKNOWN = "-"
    private val fallback = Profile(name = UNKNOWN, login = LOGIN, url = "https://github.com/$LOGIN")
    private val _profile = MutableStateFlow(fallback)
    val profile: StateFlow<Profile> = _profile.asStateFlow()
    @Volatile private var loaded = false

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("maker_profile", Context.MODE_PRIVATE)
    private fun avatarFile(ctx: Context) = File(ctx.applicationContext.filesDir, "maker_avatar_cache.jpg")

    /** Loads the cached profile (if any), then refreshes it from GitHub when it's over a day old. */
    suspend fun refresh(ctx: Context) = withContext(Dispatchers.IO) {
        if (!loaded) { loadCache(ctx); loaded = true }
        val p = prefs(ctx)
        if (System.currentTimeMillis() - p.getLong("fetchedAt", 0L) < REFRESH_MS || !isOnline(ctx)) return@withContext
        runCatching {
            val json = JSONObject(get("https://api.github.com/users/$LOGIN").decodeToString())
            val login = json.optString("login").ifBlank { LOGIN }
            val name = json.optString("name").takeUnless { it.isBlank() || it == "null" } ?: login
            val url = json.optString("html_url").ifBlank { "https://github.com/$login" }
            val avatarUrl = json.optString("avatar_url")
            var avatar = _profile.value.avatar
            if (avatarUrl.isNotBlank()) {
                val bytes = get(avatarUrl + (if ('?' in avatarUrl) "&" else "?") + "s=256")
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { bmp ->
                    avatarFile(ctx).writeBytes(bytes)
                    avatar = bmp
                }
            }
            p.edit().putString("name", name).putString("login", login).putString("url", url)
                .putLong("fetchedAt", System.currentTimeMillis()).apply()
            _profile.value = Profile(name, login, url, avatar)
        } // Offline / rate-limited / bad response: keep what we have and try again next time the page opens.
    }

    private fun loadCache(ctx: Context) {
        val p = prefs(ctx)
        val avatar = runCatching { avatarFile(ctx).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) } }.getOrNull()
        _profile.value = Profile(
            name = p.getString("name", null) ?: fallback.name,
            login = p.getString("login", null) ?: fallback.login,
            url = p.getString("url", null) ?: fallback.url,
            avatar = avatar,
        )
    }

    private fun isOnline(ctx: Context): Boolean = runCatching {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }.getOrDefault(true)

    private fun get(url: String): ByteArray {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 8000; c.readTimeout = 8000
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("User-Agent", "Clusterlink-Android")
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.use { it.readBytes() }
        } finally { c.disconnect() }
    }
}
