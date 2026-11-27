package com.example.jamuione.ui.travel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.Trip
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.TripRepository
import com.example.jamuione.domain.repository.UserRepository
import com.example.jamuione.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TravelViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("travel_prefs", Context.MODE_PRIVATE)

    private val _fromKey = MutableStateFlow("jamui")
    val fromKey: StateFlow<String> = _fromKey

    private val _toKey = MutableStateFlow("patna")
    val toKey: StateFlow<String> = _toKey

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate

    private val _selectedTab = MutableStateFlow(0) // 0: Find a ride, 1: Requests
    val selectedTab: StateFlow<Int> = _selectedTab

    private val _hideFullTrips = MutableStateFlow(false)
    val hideFullTrips: StateFlow<Boolean> = _hideFullTrips

    private val _bannerDismissed = MutableStateFlow(prefs.getBoolean("banner_dismissed", false))
    val bannerDismissed: StateFlow<Boolean> = _bannerDismissed

    private val _userProfile = MutableStateFlow<Resource<com.example.jamuione.domain.model.User?>>(Resource.Idle())
    val userProfile: StateFlow<Resource<com.example.jamuione.domain.model.User?>> = _userProfile

    val currentUserId: String?
        get() = authRepository.getCurrentUser()?.uid

    val isOperator: StateFlow<Boolean> = _userProfile.map { res ->
        (res as? Resource.Success)?.data?.operatorVerified == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            userRepository.getUserProfile(uid).collectLatest { resource ->
                _userProfile.value = resource
                if (resource is Resource.Success && resource.data != null) {
                    val districtTown = resource.data?.district?.lowercase()?.replace(" ", "_") ?: "jamui"
                    if (districtTown.isNotBlank()) {
                        _fromKey.value = districtTown
                    }
                }
            }
        }
    }

    fun setTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setFrom(key: String) {
        _fromKey.value = key
    }

    fun setTo(key: String) {
        _toKey.value = key
    }

    fun swapRoutes() {
        val temp = _fromKey.value
        _fromKey.value = _toKey.value
        _toKey.value = temp
    }

    fun setDate(timestamp: Long) {
        _selectedDate.value = timestamp
    }

    fun setHideFullTrips(hide: Boolean) {
        _hideFullTrips.value = hide
    }

    fun dismissBanner() {
        _bannerDismissed.value = true
        prefs.edit().putBoolean("banner_dismissed", true).apply()
    }

    fun markFull(tripId: String) {
        viewModelScope.launch {
            tripRepository.updateTripStatus(tripId, "full").collectLatest { }
        }
    }

    fun cancelRequest(tripId: String) {
        viewModelScope.launch {
            tripRepository.deleteTrip(tripId).collectLatest { }
        }
    }

    private fun getDayRange(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis - 1
        return Pair(start, end)
    }

    val requestCount: StateFlow<Resource<Long>> = combine(
        _fromKey,
        _toKey,
        _selectedDate
    ) { from, to, date ->
        Triple(from, to, date)
    }.flatMapLatest { (from, to, date) ->
        val (start, end) = getDayRange(date)
        tripRepository.countRequests(from, to, start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Resource.Loading()
    )

    val myRequests: StateFlow<Resource<List<Trip>>> = currentUserId?.let { uid ->
        tripRepository.observeMyTrips(uid).map { res ->
            if (res is Resource.Success) {
                Resource.Success(res.data?.filter { it.type == "request" } ?: emptyList())
            } else {
                res
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading()
        )
    } ?: flowOf<Resource<List<Trip>>>(Resource.Success(emptyList())).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Resource.Success(emptyList())
    )

    val operatorDemand: StateFlow<Resource<List<Trip>>> = flow {
        val now = System.currentTimeMillis()
        val next7Days = now + (7 * 24 * 3600 * 1000L)
        tripRepository.observeRequests("", "", now, next7Days).collect { emit(it) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Resource.Loading()
    )

    val trips: StateFlow<Resource<List<Trip>>> = combine(
        _fromKey,
        _toKey,
        _selectedDate,
        _hideFullTrips
    ) { from, to, date, hideFull ->
        Triple(Triple(from, to, date), hideFull, Unit)
    }.flatMapLatest { (params, hideFull, _) ->
        val (from, to, date) = params
        val (dayStart, dayEnd) = getDayRange(date)

        tripRepository.observeTrips(from, to, dayStart, dayEnd).map { resource ->
            if (resource is Resource.Success) {
                val now = System.currentTimeMillis()
                val cutoff = now - (30 * 60 * 1000L)
                val filtered = resource.data?.filter { trip ->
                    if (trip.departureAt < cutoff) return@filter false
                    if (hideFull && trip.seatsLeft <= 0) return@filter false
                    true
                } ?: emptyList()
                Resource.Success(filtered)
            } else {
                resource
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Resource.Loading()
    )
}
