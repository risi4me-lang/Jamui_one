package com.example.jamuione.util

import android.content.Context
import com.example.jamuione.domain.model.Place
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

object PlaceCatalog {
    private var places: List<Place> = emptyList()

    fun init(context: Context) {
        if (places.isNotEmpty()) return
        try {
            val inputStream = context.assets.open("places.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            places = Json.decodeFromString<List<Place>>(jsonString)
        } catch (e: Exception) {
            places = emptyList()
        }
    }

    fun getAllPlaces(context: Context): List<Place> {
        init(context)
        return places
    }

    fun search(context: Context, query: String): List<Place> {
        init(context)
        if (query.isBlank()) return places
        val q = query.trim().lowercase()
        return places.filter {
            it.name.lowercase().contains(q) ||
            it.nameHi.contains(q) ||
            it.key.contains(q)
        }
    }

    fun byKey(context: Context, key: String): Place? {
        init(context)
        return places.find { it.key.equals(key, ignoreCase = true) }
    }
}
