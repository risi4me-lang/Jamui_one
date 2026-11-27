package com.example.jamuione.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jamuione.domain.model.User

sealed interface AudienceOption {
    data class Locality(val value: String) : AudienceOption
    data class District(val value: String) : AudienceOption
    data class Hometown(val value: String) : AudienceOption
}

@Composable
fun AudiencePickerRow(
    user: User?,
    selectedAudience: AudienceOption,
    onAudienceSelected: (AudienceOption) -> Unit
) {
    if (user == null) return
    val options = mutableListOf<AudienceOption>()
    if (user.locality.isNotBlank() && user.locality != "unknown") {
        options.add(AudienceOption.Locality(user.locality))
    }
    if (user.district.isNotBlank()) {
        options.add(AudienceOption.District(user.district))
    }
    if (user.nativeDistrict.isNotBlank()) {
        options.add(AudienceOption.Hometown(user.nativeDistrict))
    }

    if (options.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Post to", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val label = when (option) {
                    is AudienceOption.Locality -> "My locality (${option.value.replaceFirstChar { it.uppercase() }})"
                    is AudienceOption.District -> "My district (${option.value.replaceFirstChar { it.uppercase() }})"
                    is AudienceOption.Hometown -> "My hometown (${option.value.replaceFirstChar { it.uppercase() }})"
                }
                FilterChip(
                    selected = selectedAudience == option,
                    onClick = { onAudienceSelected(option) },
                    label = { Text(label, maxLines = 1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
