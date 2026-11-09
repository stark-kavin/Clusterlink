package com.clusterlink.app.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * The rider's garage: every vehicle Clusterlink has connected to, most recent first.
 * A vehicle is saved the first time a connection to it completes (the original app does the same
 * with its SaveNdConnectVdsMapping list after pairing).
 */
object VehicleStore {

    data class Vehicle(
        val address: String,
        /** Raw Bluetooth name the vehicle advertises, e.g. "MCU01************". */
        val bleName: String,
        /** What the rider calls it; editable. */
        val nickname: String,
        val lastConnected: Long,
        /** [BikeModel.id] picked by the rider, or null if not set. */
        val model: String? = null,
        /** [BikeColor.id] within that model. */
        val color: String? = null,
    ) {
        val bike: BikeChoice? get() = BikeModel.of(model)?.let { BikeChoice(it, it.color(color)) }
    }

    /** Bike chosen in the add-vehicle sheet, applied when that bike's connection completes. */
    private val pendingBikes = java.util.concurrent.ConcurrentHashMap<String, BikeChoice>()

    fun preassignBike(address: String, bike: BikeChoice?) {
        if (bike == null) pendingBikes.remove(address.uppercase()) else pendingBikes[address.uppercase()] = bike
    }

    /** Default name for a new vehicle. Hero cluster names start with "MCU". */
    fun defaultName(bleName: String): String = when {
        bleName.startsWith("MCU", ignoreCase = true) -> "My bike"
        bleName.isNotBlank() && !bleName.matches(Regex("[A-Z0-9_\\-]{4,}")) -> bleName
        else -> "My vehicle"
    }

    private var prefs: SharedPreferences? = null
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    fun init(ctx: Context) {
        if (prefs != null) return
        val p = ctx.applicationContext.getSharedPreferences("clusterlink_garage", Context.MODE_PRIVATE)
        prefs = p
        _vehicles.value = runCatching {
            val arr = JSONArray(p.getString("vehicles", "[]"))
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Vehicle(o.getString("address"), o.optString("bleName"), o.optString("nickname"), o.optLong("lastConnected"),
                    o.optString("model").ifBlank { null }, o.optString("color").ifBlank { null })
            }
        }.getOrDefault(emptyList()).sortedByDescending { it.lastConnected }
    }

    /** Called by the BLE service when a connection to [address] completes. */
    fun recordConnected(address: String, bleName: String) {
        val now = System.currentTimeMillis()
        val cur = _vehicles.value
        val existing = cur.firstOrNull { it.address.equals(address, true) }
        val picked = pendingBikes.remove(address.uppercase())
        var updated = existing?.copy(lastConnected = now, bleName = bleName.ifBlank { existing.bleName })
            ?: Vehicle(address, bleName, defaultName(bleName), now)
        if (picked != null) updated = withBike(updated, picked)
        save(listOf(updated) + cur.filterNot { it.address.equals(address, true) })
    }

    fun setBike(address: String, bike: BikeChoice) =
        save(_vehicles.value.map { if (it.address.equals(address, true)) withBike(it, bike) else it })

    /** Sets model + colour; also renames the vehicle if it still has an automatic name. */
    private fun withBike(v: Vehicle, bike: BikeChoice): Vehicle {
        val autoName = v.nickname == defaultName(v.bleName) || BikeModel.entries.any { it.displayName == v.nickname }
        return v.copy(model = bike.model.id, color = bike.color.id, nickname = if (autoName) bike.model.displayName else v.nickname)
    }

    fun rename(address: String, nickname: String) {
        val name = nickname.trim().ifBlank { return }
        save(_vehicles.value.map { if (it.address.equals(address, true)) it.copy(nickname = name.take(40)) else it })
    }

    fun forget(address: String) = save(_vehicles.value.filterNot { it.address.equals(address, true) })

    fun find(address: String?): Vehicle? =
        address?.let { a -> _vehicles.value.firstOrNull { it.address.equals(a, true) } }

    private fun save(list: List<Vehicle>) {
        val sorted = list.sortedByDescending { it.lastConnected }
        _vehicles.value = sorted
        val arr = JSONArray()
        sorted.forEach {
            arr.put(JSONObject().put("address", it.address).put("bleName", it.bleName)
                .put("nickname", it.nickname).put("lastConnected", it.lastConnected).put("model", it.model ?: "").put("color", it.color ?: ""))
        }
        prefs?.edit()?.putString("vehicles", arr.toString())?.apply()
    }
}
