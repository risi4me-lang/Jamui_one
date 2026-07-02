package com.example.jamuione.util

object BrandingUtil {
    fun getCommunityName(district: String?): String {
        return if (district.isNullOrBlank()) {
            "District One"
        } else {
            "$district One"
        }
    }
}
