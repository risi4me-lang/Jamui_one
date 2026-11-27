package com.example.jamuione.ui.travel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jamuione.domain.model.OperatorApplication
import com.example.jamuione.domain.repository.AuthRepository
import com.example.jamuione.domain.repository.TripRepository
import com.example.jamuione.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplyOperatorViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val uid = authRepository.getCurrentUser()?.uid ?: ""

    val application: StateFlow<Resource<OperatorApplication?>> = tripRepository.observeOperatorApplication(uid)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading()
        )

    private val _businessName = MutableStateFlow("")
    val businessName: StateFlow<String> = _businessName

    private val _vehicleType = MutableStateFlow("Sumo")
    val vehicleType: StateFlow<String> = _vehicleType

    private val _vehicleNumber = MutableStateFlow("")
    val vehicleNumber: StateFlow<String> = _vehicleNumber

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone

    private val _routes = MutableStateFlow("")
    val routes: StateFlow<String> = _routes

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes

    private val _submitResult = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val submitResult: StateFlow<Resource<Boolean>> = _submitResult

    fun setBusinessName(v: String) { _businessName.value = v }
    fun setVehicleType(v: String) { _vehicleType.value = v }
    fun setVehicleNumber(v: String) { _vehicleNumber.value = v }
    fun setPhone(v: String) { _phone.value = v }
    fun setRoutes(v: String) { _routes.value = v }
    fun setNotes(v: String) { _notes.value = v }

    fun normalizeIndianPhone(input: String): String? {
        val digits = input.filter { it.isDigit() }
        val normalized = when {
            digits.length == 12 && digits.startsWith("91") -> digits.substring(2)
            digits.length == 13 && digits.startsWith("+91") -> digits.substring(3)
            digits.length == 10 -> digits
            else -> null
        }
        return if (normalized != null && normalized.matches(Regex("^[6-9]\\d{9}$"))) {
            normalized
        } else {
            null
        }
    }

    fun submitApplication() {
        if (uid.isBlank()) return
        val validPhone = normalizeIndianPhone(_phone.value)
        if (validPhone == null) {
            _submitResult.value = Resource.Error("Enter a valid 10-digit Indian mobile number")
            return
        }

        viewModelScope.launch {
            _submitResult.value = Resource.Loading()
            val app = OperatorApplication(
                userId = uid,
                businessName = _businessName.value,
                vehicleType = _vehicleType.value,
                vehicleNumber = _vehicleNumber.value,
                phone = validPhone,
                routes = _routes.value,
                notes = _notes.value,
                status = "pending",
                createdAt = System.currentTimeMillis()
            )
            tripRepository.submitOperatorApplication(app).collectLatest { res ->
                _submitResult.value = res
            }
        }
    }

    fun withdrawApplication() {
        if (uid.isBlank()) return
        viewModelScope.launch {
            tripRepository.withdrawOperatorApplication(uid).collectLatest { }
        }
    }
}
