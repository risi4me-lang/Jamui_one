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
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PostTripViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _userProfile = MutableStateFlow<Resource<com.example.jamuione.domain.model.User?>>(Resource.Idle())
    val userProfile: StateFlow<Resource<com.example.jamuione.domain.model.User?>> = _userProfile

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

    private val _vehicle = MutableStateFlow("Sumo")
    val vehicle: StateFlow<String> = _vehicle

    private val _seatsTotal = MutableStateFlow(6)
    val seatsTotal: StateFlow<Int> = _seatsTotal

    private val _farePerSeat = MutableStateFlow(350)
    val farePerSeat: StateFlow<Int> = _farePerSeat

    private val _pickupPoint = MutableStateFlow("")
    val pickupPoint: StateFlow<String> = _pickupPoint

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes

    private val _repeatOption = MutableStateFlow("Just this day")
    val repeatOption: StateFlow<String> = _repeatOption

    private val _operatorName = MutableStateFlow("")
    val operatorName: StateFlow<String> = _operatorName

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
                _userProfile.value = resource
                if (resource is Resource.Success && resource.data != null) {
                    val user = resource.data!!
                    _operatorName.value = user.operatorProfile.businessName.ifBlank { user.name }
                    _phone.value = user.operatorProfile.phone
                    if (user.operatorProfile.vehicle.isNotBlank()) {
                        _vehicle.value = user.operatorProfile.vehicle
                    }
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
    fun setVehicle(v: String) { _vehicle.value = v }
    fun setSeats(seats: Int) { _seatsTotal.value = seats }
    fun setFare(fare: Int) { _farePerSeat.value = fare }
    fun setPickupPoint(p: String) { _pickupPoint.value = p }
    fun setNotes(n: String) { _notes.value = n }
    fun setRepeatOption(option: String) { _repeatOption.value = option }

    fun postTrips() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _createResult.value = Resource.Loading()
            try {
                val daysCount = when (_repeatOption.value) {
                    "Next 3 days" -> 3
                    "Next 7 days" -> 7
                    else -> 1
                }

                val tripsList = mutableListOf<Trip>()
                val cal = Calendar.getInstance().apply { timeInMillis = _departureDate.value }

                for (i in 0 until daysCount) {
                    val tripId = UUID.randomUUID().toString()
                    val tripTime = cal.timeInMillis
                    tripsList.add(
                        Trip(
                            id = tripId,
                            userId = uid,
                            type = "operator",
                            operatorName = _operatorName.value,
                            phone = _phone.value,
                            vehicle = _vehicle.value,
                            fromKey = _fromKey.value,
                            toKey = _toKey.value,
                            fromName = _fromName.value,
                            toName = _toName.value,
                            departureAt = tripTime,
                            seatsTotal = _seatsTotal.value,
                            seatsLeft = _seatsTotal.value,
                            farePerSeat = _farePerSeat.value,
                            pickupPoint = _pickupPoint.value,
                            notes = _notes.value,
                            status = "open",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }

                tripRepository.createTrips(tripsList).collectLatest { res ->
                    _createResult.value = res
                }
            } catch (e: Exception) {
                _createResult.value = Resource.Error(e.message ?: "Failed to post trips")
            }
        }
    }
}
