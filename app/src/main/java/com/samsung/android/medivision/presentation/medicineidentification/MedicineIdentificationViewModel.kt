package com.samsung.android.medivision.presentation.medicineidentification

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.medivision.domain.usecase.IdentifyMedicineUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MedicineIdentificationViewModel(
    private val identifyMedicineUseCase: IdentifyMedicineUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MedicineIdentificationState())
    val state: StateFlow<MedicineIdentificationState> = _state.asStateFlow()

    fun onImageSelected(bitmap: Bitmap) {
        _state.update { currentState ->
            currentState.copy(
                selectedBitmap = bitmap,
                identificationResult = "",
                error = null
            )
        }
    }

    fun identifyMedicine() {
        val bitmap = _state.value.selectedBitmap ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            identifyMedicineUseCase(bitmap)
                .onSuccess { identification ->
                    _state.update {
                        it.copy(
                            identificationResult = identification.description,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to identify medicine."
                        )
                    }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
