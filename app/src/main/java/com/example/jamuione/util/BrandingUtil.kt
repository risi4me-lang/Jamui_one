package com.example.jamuione.util

import java.util.Locale

object BrandingUtil {
    fun getCommunityName(district: String?): String {
        return if (district.isNullOrBlank()) {
            "District One"
        } else {
            val formattedDistrict = district.trim().replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
            "$formattedDistrict One"
        }
    }
}
