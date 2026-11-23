package com.example.jamuione.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.jamuione.domain.model.User
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopicManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("topic_manager_prefs", Context.MODE_PRIVATE)
    private val TAG = "TOPIC_DEBUG"

    fun updateTopics(user: User) {
        val sState = sanitizeTopicValue(user.state)
        val sDistrict = sanitizeTopicValue(user.district)
        val sLocality = sanitizeTopicValue(user.locality)

        val newDistrictTopic = "district_${sState}_${sDistrict}"
        val newLocalityTopic = "locality_${sState}_${sDistrict}_${sLocality}"

        val newTopics = setOf(newDistrictTopic, newLocalityTopic)
        val oldTopics = prefs.getStringSet("subscribed_topics", emptySet()) ?: emptySet()

        if (oldTopics == newTopics) {
            Log.d(TAG, "Topics unchanged: $newTopics")
            return
        }

        // Unsubscribe from old topics not in new topics
        oldTopics.filter { it !in newTopics }.forEach { oldTopic ->
            Log.d(TAG, "Unsubscribing from old topic: $oldTopic")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(oldTopic).await()
                    Log.d(TAG, "Successfully unsubscribed from $oldTopic")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unsubscribe from $oldTopic", e)
                }
            }
        }

        // Subscribe to new topics not in old topics
        newTopics.filter { it !in oldTopics }.forEach { newTopic ->
            Log.d(TAG, "Subscribing to new topic: $newTopic")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    FirebaseMessaging.getInstance().subscribeToTopic(newTopic).await()
                    Log.d(TAG, "Successfully subscribed to $newTopic")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to subscribe to $newTopic", e)
                }
            }
        }

        prefs.edit().putStringSet("subscribed_topics", newTopics).apply()
    }

    fun unsubscribeAll() {
        val oldTopics = prefs.getStringSet("subscribed_topics", emptySet()) ?: emptySet()
        oldTopics.forEach { topic ->
            Log.d(TAG, "Unsubscribing from topic on logout/deletion: $topic")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
                    Log.d(TAG, "Successfully unsubscribed from $topic")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unsubscribe from $topic", e)
                }
            }
        }
        prefs.edit().remove("subscribed_topics").apply()
    }

    private fun sanitizeTopicValue(value: String): String {
        val lowercase = value.trim().lowercase(Locale.ROOT)
        return lowercase.replace(Regex("[^a-z0-9_]"), "_")
    }
}
