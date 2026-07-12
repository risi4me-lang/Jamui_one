package com.example.jamuione.util

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.InputStream

/**
 * Singleton provider for Indian states and districts data.
 * Note: District boundaries and names are subject to change by state governments.
 * This list should be reviewed periodically for accuracy.
 */
object LocationDataProvider {
    private var locationData: Map<String, List<String>> = emptyMap()
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val jsonString = context.assets.open("india_states_districts.json").bufferedReader().use { it.readText() }
            locationData = Json.decodeFromString<Map<String, List<String>>>(jsonString)
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to empty if initialization fails
            locationData = emptyMap()
        }
    }

    fun getStates(): List<String> {
        return locationData.keys.toList().sorted()
    }

    fun getDistricts(state: String): List<String> {
        return locationData[state]?.sorted() ?: emptyList()
    }
}
