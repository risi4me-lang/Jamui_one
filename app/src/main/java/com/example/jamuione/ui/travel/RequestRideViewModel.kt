package com.example.jamuione.ui.travel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.Trip
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.TripRepository
import com.example.jamuione.domain.repository.UserRepository
import com.example.jamuione.util.PlaceCatalog
import com.example.jamuione.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RequestRideViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _fromKey = MutableStateFlow("jamui")
    val fromKey: StateFlow<String> = _fromKey
    private val _toKey = MutableStateFlow("patna")
    val toKey: StateFlow<String> = _toKey
    private val _fromName = MutableStateFlow("Jamui")
    val fromName: StateFlow<String> = _fromName
    private val _toName = MutableStateFlow("Patna")
    val toName: StateFlow<String> = _toName

    private val _departureDate = MutableStateFlow(System.currentTimeMillis())
    val departureDate: StateFlow<Long> = _departureDate

    private val _seatsNeeded = MutableStateFlow(1)
    val seatsNeeded: StateFlow<Int> = _seatsNeeded

    private val _timeWindow = MutableStateFlow("Morning")
    val timeWindow: StateFlow<String> = _timeWindow

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone

    private val _createResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val createResult: StateFlow<Resource<Boolean>> = _createResult

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserProfile(uid).collectLatest { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    val user = resource.data!!
                    _phone.value = user.operatorProfile.phone.ifBlank { "" }
                    if (user.district.isNotBlank()) {
                        val key = user.district.lowercase().replace(" ", "_")
                        _fromKey.value = key
                        PlaceCatalog.byKey(context, key)?.let { _fromName.value = it.name }
                    }
                }
            }
        }
    }

    fun setFrom(key: String, name: String) {
        _fromKey.value = key
        _fromName.value = name
    }

    fun setTo(key: String, name: String) {
        _toKey.value = key
        _toName.value = name
    }

    fun setDepartureDate(date: Long) { _departureDate.value = date }
    fun setSeatsNeeded(seats: Int) { _seatsNeeded.value = seats }
    fun setTimeWindow(tw: String) { _timeWindow.value = tw }
    fun setNotes(n: String) { _notes.value = n }
    fun setPhone(p: String) { _phone.value = p }

    fun postRequest() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _createResult.value = Resource.Loading()
            try {
                val tripId = UUID.randomUUID().toString()
                val requestTrip = Trip(
                    id = tripId,
                    userId = uid,
                    type = "request",
                    operatorName = "Passenger",
                    phone = _phone.value,
                    vehicle = "Car",
                    fromKey = _fromKey.value,
                    toKey = _toKey.value,
                    fromName = _fromName.value,
                    toName = _toName.value,
                    departureAt = _departureDate.value,
                    seatsNeeded = _seatsNeeded.value,
                    timeWindow = _timeWindow.value,
                    notes = _notes.value,
                    status = "open",
                    createdAt = System.currentTimeMillis()
                )
                tripRepository.createTrip(requestTrip).collectLatest { res ->
                    _createResult.value = res
                }
            } catch (e: Exception) {
                _createResult.value = Resource.Error(e.message ?: "Failed to post ride request")
            }
        }
    }
}
