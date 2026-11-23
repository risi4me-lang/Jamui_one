package com.example.jamuione.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jamuione.ui.feed.FeedScope

@Composable
fun ScopeSelector(
    selectedScope: FeedScope,
    nativeDistrict: String?,
    onScopeSelected: (FeedScope) -> Unit
) {
    val scopes = FeedScope.entries.filter { it != FeedScope.NATIVE_DISTRICT || nativeDistrict != null }
    val selectedIndex = scopes.indexOf(selectedScope).coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 16.dp,
        containerColor = Color.Transparent,
        divider = {},
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty() && selectedIndex in tabPositions.indices) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        scopes.forEachIndexed { index, scope ->
            val label = when (scope) {
                FeedScope.LOCALITY -> "My Locality"
                FeedScope.DISTRICT -> "My District"
                FeedScope.STATE -> "My State"
                FeedScope.NATIVE_DISTRICT -> "From $nativeDistrict"
            }
            val isSelected = selectedScope == scope
            Tab(
                selected = isSelected,
                onClick = { onScopeSelected(scope) },
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                text = {
                    Text(
                        text = label,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}
