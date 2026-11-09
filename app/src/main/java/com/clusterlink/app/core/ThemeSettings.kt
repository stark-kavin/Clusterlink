package com.clusterlink.app.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How the app picks its colours (Settings → Theme), persisted in SharedPreferences.
 * - [Mode.BIKE]: follow the current bike's colour edition (default).
 * - [Mode.PRESET]: one of the fixed [ThemePreset]s.
 * - [Mode.CUSTOM]: the rider's own accent + second colour.
 */
object ThemeSettings {

    enum class Mode { BIKE, PRESET, CUSTOM }

    data class Values(
        val mode: Mode = Mode.BIKE,
        val preset: String = ThemePreset.DEFAULT.id,
        val customAccent: Long = 0xFF4C82FF,
        val customSecond: Long = 0xFFFF3B4A,
        /** Tint the background, cards and sheets with the accent (off = neutral near-black, the default). */
        val tintBackground: Boolean = false,
    )

    private var prefs: SharedPreferences? = null
    private val _values = MutableStateFlow(Values())
    val values: StateFlow<Values> = _values.asStateFlow()

    fun init(ctx: Context) {
        if (prefs != null) return
        val p = ctx.applicationContext.getSharedPreferences("clusterlink_theme", Context.MODE_PRIVATE)
        prefs = p
        val d = Values()
        _values.value = Values(
            mode = runCatching { Mode.valueOf(p.getString("mode", null) ?: d.mode.name) }.getOrDefault(d.mode),
            preset = p.getString("preset", d.preset) ?: d.preset,
            customAccent = p.getLong("customAccent", d.customAccent),
            customSecond = p.getLong("customSecond", d.customSecond),
            // Key renamed when the default changed to off, so earlier saved "on" values are not reused.
            tintBackground = p.getBoolean("tintBg", d.tintBackground),
        )
    }

    fun update(change: (Values) -> Values) {
        val v = change(_values.value)
        _values.value = v
        prefs?.edit()
            ?.putString("mode", v.mode.name)
            ?.putString("preset", v.preset)
            ?.putLong("customAccent", v.customAccent)
            ?.putLong("customSecond", v.customSecond)
            ?.putBoolean("tintBg", v.tintBackground)
            ?.apply()
    }
}

/** Fixed colour themes offered in Settings → Theme. [DEFAULT] is also used when no bike colour is known. */
enum class ThemePreset(val id: String, val displayName: String, val accent: Long, val second: Long) {
    GRAPHITE("graphite", "Graphite", 0xFFA3ABB5, 0xFF5C6672),
    VOLT("volt", "Volt", 0xFFD4FF4F, 0xFF5CE1FF),
    RACING_RED("racing_red", "Racing red", 0xFFFF3B4A, 0xFFF2F4F7),
    OCEAN("ocean", "Ocean", 0xFF4C82FF, 0xFF5CE1FF),
    DESERT("desert", "Desert", 0xFFFF8A3D, 0xFFFFD27A),
    FOREST("forest", "Forest", 0xFF3DDC84, 0xFFB5E86C),
    ROYAL("royal", "Royal", 0xFFA78BFA, 0xFFF472B6),
    GOLD("gold", "Gold", 0xFFE8B44C, 0xFFF2F4F7);

    companion object {
        val DEFAULT = GRAPHITE
        fun of(id: String?): ThemePreset = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
