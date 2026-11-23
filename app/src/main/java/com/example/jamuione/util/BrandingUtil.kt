package com.example.jamuione.util

import java.util.Locale

object BrandingUtil {
    const val SUPPORT_WHATSAPP_URL = "https://wa.me/919876543210?text=Hello%20District%20One,%20I%20am%20missing%20a%20community."
    const val SUPPORT_EMAIL = "support@districtone.app"

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
