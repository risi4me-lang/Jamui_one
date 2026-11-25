package com.example.jamuione.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Place(
    val key: String = "",
    val name: String = "",
    val nameHi: String = "",
    val type: String = ""
)
