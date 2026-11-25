package com.example.jamuione.domain.repository

import com.example.jamuione.domain.model.Trip
import com.example.jamuione.util.Resource
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun observeTrips(fromKey: String, toKey: String, dayStart: Long, dayEnd: Long): Flow<Resource<List<Trip>>>
    fun observeMyTrips(uid: String): Flow<Resource<List<Trip>>>
    fun createTrip(trip: Trip): Flow<Resource<Boolean>>
    fun createTrips(list: List<Trip>): Flow<Resource<Boolean>>
    fun updateTripStatus(id: String, status: String): Flow<Resource<Boolean>>
    fun updateSeatsLeft(id: String, n: Int): Flow<Resource<Boolean>>
    fun deleteTrip(id: String): Flow<Resource<Boolean>>
    fun observeRequests(fromKey: String, toKey: String, dayStart: Long, dayEnd: Long): Flow<Resource<List<Trip>>>
    fun countRequests(fromKey: String, toKey: String, dayStart: Long, dayEnd: Long): Flow<Resource<Long>>
}
