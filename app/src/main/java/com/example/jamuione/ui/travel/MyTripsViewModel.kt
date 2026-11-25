package com.example.jamuione.ui.travel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.Trip
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.TripRepository
import com.example.jamuione.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MyTripsViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val uid = authRepository.getCurrentUser()?.uid ?: ""

    val myTrips: StateFlow<Resource<List<Trip>>> = tripRepository.observeMyTrips(uid)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading()
        )

    fun updateSeats(tripId: String, currentSeats: Int, delta: Int) {
        val newSeats = maxOf(0, currentSeats + delta)
        viewModelScope.launch {
            tripRepository.updateSeatsLeft(tripId, newSeats).collectLatest {}
            if (newSeats == 0) {
                tripRepository.updateTripStatus(tripId, "full").collectLatest {}
            }
        }
    }

    fun markFull(tripId: String) {
        viewModelScope.launch {
            tripRepository.updateTripStatus(tripId, "full").collectLatest {}
            tripRepository.updateSeatsLeft(tripId, 0).collectLatest {}
        }
    }

    fun cancelTrip(tripId: String) {
        viewModelScope.launch {
            tripRepository.updateTripStatus(tripId, "cancelled").collectLatest {}
        }
    }

    fun repeatTomorrow(trip: Trip) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val newTrip = trip.copy(
                id = newId,
                departureAt = trip.departureAt + (24 * 3600 * 1000L),
                seatsLeft = trip.seatsTotal,
                status = "open",
                createdAt = System.currentTimeMillis()
            )
            tripRepository.createTrip(newTrip).collectLatest {}
        }
    }
}
