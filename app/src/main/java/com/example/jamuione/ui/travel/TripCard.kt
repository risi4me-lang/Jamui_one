package com.example.jamuione.ui.travel

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jamuione.BuildConfig
import com.example.jamuione.domain.model.Trip
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TripCard(
    trip: Trip,
    currentUserId: String?,
    onMarkFull: (String) -> Unit
) {
    val context = LocalContext.current
    val analytics = remember { FirebaseAnalytics.getInstance(context) }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        analytics.logEvent("trip_viewed", android.os.Bundle().apply {
            putString("trip_id", trip.id)
            putString("operator_id", trip.userId)
        })
    }

    val timeStr = remember(trip.departureAt) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(trip.departureAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Departure time & Seats chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                val (seatsText, seatsColor, seatsContainerColor) = when {
                    trip.seatsLeft <= 0 -> Triple("Full", MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surfaceContainerHighest)
                    trip.seatsLeft == 1 -> Triple("1 seat left", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer)
                    else -> Triple("${trip.seatsLeft} seats left", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = seatsContainerColor
                ) {
                    Text(
                        text = seatsText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = seatsColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Route line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = trip.fromName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = trip.toName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Fare
            Text(
                text = if (trip.farePerSeat == 0) "Ask for fare" else "₹${trip.farePerSeat} per seat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Chip row: Vehicle and pickup point
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = trip.vehicle,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (trip.pickupPoint.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "Pickup: ${trip.pickupPoint}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Operator row & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = trip.operatorName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = trip.operatorName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Verified operator",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Report") },
                            onClick = {
                                showMenu = false
                                showReportDialog = true
                            }
                        )
                        if (currentUserId != null && currentUserId == trip.userId) {
                            DropdownMenuItem(
                                text = { Text("Mark full") },
                                onClick = {
                                    showMenu = false
                                    onMarkFull(trip.id)
                                }
                            )
                        }
                    }
                }
            }

            // Actions row: Call, WhatsApp, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (trip.phone.isNotBlank()) {
                    Button(
                        onClick = {
                            analytics.logEvent("contact_tapped", android.os.Bundle().apply {
                                putString("trip_id", trip.id)
                                putString("method", "call")
                            })
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${trip.phone}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call")
                    }

                    OutlinedButton(
                        onClick = {
                            analytics.logEvent("contact_tapped", android.os.Bundle().apply {
                                putString("trip_id", trip.id)
                                putString("method", "whatsapp")
                            })
                            val msg = "Hi, I saw your ${trip.fromName} to ${trip.toName} trip on District One"
                            val encoded = Uri.encode(msg)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${trip.phone}?text=$encoded"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp")
                    }
                }

                IconButton(
                    onClick = {
                        analytics.logEvent("trip_shared", android.os.Bundle().apply {
                            putString("trip_id", trip.id)
                        })
                        val summary = "🚗 Travel Offer: ${trip.fromName} to ${trip.toName}\n" +
                                "📅 Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(trip.departureAt))}\n" +
                                "⏰ Time: $timeStr\n" +
                                "🚙 Vehicle: ${trip.vehicle}\n" +
                                "💺 Seats left: ${trip.seatsLeft}\n" +
                                "💰 Fare: ${if (trip.farePerSeat == 0) "Ask" else "₹${trip.farePerSeat}"}\n\n" +
                                "Download District One: https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, summary)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share trip via"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share trip", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Trip") },
            text = {
                Column {
                    Text("Please specify the reason for reporting this trip:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("e.g. Fake details, inappropriate content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReportDialog = false
                        val reportId = UUID.randomUUID().toString()
                        val report = mapOf(
                            "id" to reportId,
                            "reporterId" to (currentUserId ?: "anonymous"),
                            "targetId" to trip.id,
                            "targetType" to "trip",
                            "reason" to reportReason,
                            "status" to "open",
                            "createdAt" to System.currentTimeMillis()
                        )
                        firestore.collection("reports").document(reportId).set(report)
                        reportReason = ""
                    },
                    enabled = reportReason.isNotBlank()
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
