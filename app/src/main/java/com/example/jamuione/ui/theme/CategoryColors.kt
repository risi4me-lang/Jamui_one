package com.example.jamuione.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryStyle(
    val icon: ImageVector,
    val color: Color
)

@Composable
fun categoryStyle(category: String): CategoryStyle {
    return when (category.lowercase().trim()) {
        "blood donation" -> CategoryStyle(Icons.Default.Bloodtype, Color(0xFFE5484D))
        "jobs" -> CategoryStyle(Icons.Default.Work, Color(0xFF3E8BFF))
        "travel" -> CategoryStyle(Icons.Default.TravelExplore, Color(0xFF2BB5A8))
        "rent/flatmate" -> CategoryStyle(Icons.Default.Home, Color(0xFF8E6CF0))
        "buy & sell" -> CategoryStyle(Icons.Default.ShoppingCart, Color(0xFFF5A524))
        "lost & found" -> CategoryStyle(Icons.Default.Search, Color(0xFFEC6AA6))
        "help needed" -> CategoryStyle(Icons.Default.Favorite, Color(0xFFFF7A45))
        "announcement" -> CategoryStyle(Icons.Default.Campaign, Color(0xFF6C7BFF))
        "event" -> CategoryStyle(Icons.Default.Event, Color(0xFF2FBF71))
        else -> CategoryStyle(Icons.Default.Info, MaterialTheme.colorScheme.primary)
    }
}
