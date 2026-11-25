package com.example.jamuione.data.repository

import android.content.Context
import android.os.Bundle
import com.example.jamuione.domain.model.Trip
import com.example.jamuione.domain.repository.TripRepository
import com.example.jamuione.util.Resource
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) : TripRepository {

    override fun observeTrips(
        fromKey: String,
        toKey: String,
        dayStart: Long,
        dayEnd: Long
    ): Flow<Resource<List<Trip>>> = callbackFlow {
        trySend(Resource.Loading())
        var query: Query = firestore.collection("trips")
            .whereEqualTo("type", "operator")
            .whereEqualTo("status", "open")
        if (fromKey.isNotBlank()) query = query.whereEqualTo("fromKey", fromKey)
        if (toKey.isNotBlank()) query = query.whereEqualTo("toKey", toKey)
        if (dayStart > 0 && dayEnd > 0) {
            query = query.whereGreaterThanOrEqualTo("departureAt", dayStart)
                .whereLessThanOrEqualTo("departureAt", dayEnd)
        }
        query = query.orderBy("departureAt", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Failed to fetch trips"))
                return@addSnapshotListener
            }
            snapshot?.let {
                trySend(Resource.Success(it.toObjects(Trip::class.java)))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun observeMyTrips(uid: String): Flow<Resource<List<Trip>>> = callbackFlow {
        trySend(Resource.Loading())
        val query = firestore.collection("trips")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Failed to fetch my trips"))
                return@addSnapshotListener
            }
            snapshot?.let {
                trySend(Resource.Success(it.toObjects(Trip::class.java)))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun createTrip(trip: Trip): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("trips").document(trip.id).set(trip).await()
            val analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle().apply {
                putString("trip_id", trip.id)
                putString("from_key", trip.fromKey)
                putString("to_key", trip.toKey)
            }
            if (trip.type == "request") {
                analytics.logEvent("ride_requested", bundle)
            } else {
                analytics.logEvent("trip_posted", bundle)
            }
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to create trip"))
        }
    }

    override fun createTrips(list: List<Trip>): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val batch = firestore.batch()
            list.take(14).forEach { trip ->
                val ref = firestore.collection("trips").document(trip.id)
                batch.set(ref, trip)
            }
            batch.commit().await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to create trips"))
        }
    }

    override fun updateTripStatus(id: String, status: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("trips").document(id).update(
                mapOf("status" to status, "updatedAt" to System.currentTimeMillis())
            ).await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update trip status"))
        }
    }

    override fun updateSeatsLeft(id: String, n: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("trips").document(id).update(
                mapOf("seatsLeft" to n, "updatedAt" to System.currentTimeMillis())
            ).await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update seats left"))
        }
    }

    override fun deleteTrip(id: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("trips").document(id).delete().await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to delete trip"))
        }
    }

    override fun observeRequests(
        fromKey: String,
        toKey: String,
        dayStart: Long,
        dayEnd: Long
    ): Flow<Resource<List<Trip>>> = callbackFlow {
        trySend(Resource.Loading())
        var query: Query = firestore.collection("trips")
            .whereEqualTo("type", "request")
            .whereEqualTo("status", "open")
        if (fromKey.isNotBlank()) query = query.whereEqualTo("fromKey", fromKey)
        if (toKey.isNotBlank()) query = query.whereEqualTo("toKey", toKey)
        if (dayStart > 0 && dayEnd > 0) {
            query = query.whereGreaterThanOrEqualTo("departureAt", dayStart)
                .whereLessThanOrEqualTo("departureAt", dayEnd)
        }
        query = query.orderBy("departureAt", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.message ?: "Failed to fetch requests"))
                return@addSnapshotListener
            }
            snapshot?.let {
                trySend(Resource.Success(it.toObjects(Trip::class.java)))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun countRequests(
        fromKey: String,
        toKey: String,
        dayStart: Long,
        dayEnd: Long
    ): Flow<Resource<Long>> = flow {
        emit(Resource.Loading())
        try {
            var query: Query = firestore.collection("trips")
                .whereEqualTo("type", "request")
                .whereEqualTo("status", "open")
            if (fromKey.isNotBlank()) query = query.whereEqualTo("fromKey", fromKey)
            if (toKey.isNotBlank()) query = query.whereEqualTo("toKey", toKey)
            if (dayStart > 0 && dayEnd > 0) {
                query = query.whereGreaterThanOrEqualTo("departureAt", dayStart)
                    .whereLessThanOrEqualTo("departureAt", dayEnd)
            }
            val countSnapshot = query.count().get(AggregateSource.SERVER).await()
            emit(Resource.Success(countSnapshot.count))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to count requests"))
        }
    }
}
