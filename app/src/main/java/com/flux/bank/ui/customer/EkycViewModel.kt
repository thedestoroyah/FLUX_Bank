package com.flux.bank.ui.customer

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.data.repository.AuthRepository
import com.flux.bank.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class EkycUiState {
    object Idle : EkycUiState()
    object Loading : EkycUiState()
    data class Success(val message: String) : EkycUiState()
    data class Error(val message: String) : EkycUiState()
}

class EkycViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<EkycUiState>(EkycUiState.Idle)
    val uiState: LiveData<EkycUiState> = _uiState

    fun submitEkyc(uid: String, idCardBitmap: Bitmap?, faceBitmap: Bitmap?) {
        if (uid.isEmpty()) {
            _uiState.value = EkycUiState.Error("User identification is missing. Please re-login.")
            return
        }

        if (idCardBitmap == null) {
            _uiState.value = EkycUiState.Error("Please capture or upload your Citizen ID / Passport photo.")
            return
        }

        if (faceBitmap == null) {
            _uiState.value = EkycUiState.Error("Please capture your live face verification selfie.")
            return
        }

        _uiState.value = EkycUiState.Loading

        viewModelScope.launch {
            // Compress and convert to Base64 off the main thread
            val (idCardBase64, faceBase64) = withContext(Dispatchers.Default) {
                val idBase64 = ImageUtils.bitmapToBase64(idCardBitmap, quality = 75, maxDim = 480)
                val fBase64 = ImageUtils.bitmapToBase64(faceBitmap, quality = 75, maxDim = 480)
                Pair(idBase64, fBase64)
            }

            val result = authRepository.submitEkyc(uid, idCardBase64, faceBase64)
            result.onSuccess {
                _uiState.value = EkycUiState.Success("eKYC documents submitted successfully! Awaiting bank officer verification.")
            }.onFailure { err ->
                _uiState.value = EkycUiState.Error(err.localizedMessage ?: "Failed to submit eKYC documents.")
            }
        }
    }

    fun resetState() {
        _uiState.value = EkycUiState.Idle
    }
}

