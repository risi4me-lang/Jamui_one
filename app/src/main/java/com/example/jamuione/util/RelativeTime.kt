package com.example.jamuione.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object RelativeTime {
    fun format(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        if (diff < 0) return "Just now"

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days == 1L -> "Yesterday"
            days < 365 -> {
                val calNow = Calendar.getInstance()
                val calPost = Calendar.getInstance().apply { timeInMillis = timestamp }
                val sameYear = calNow.get(Calendar.YEAR) == calPost.get(Calendar.YEAR)
                if (sameYear) {
                    SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
                } else {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
                }
            }
            else -> {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
