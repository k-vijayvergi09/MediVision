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

    fun onImageSelected(bitmap: Bitmap, fileName: String = "") {
        _state.update { currentState ->
            currentState.copy(
                selectedBitmap = bitmap,
                pdfBytes = null,
                fileName = fileName,
                isPdf = false,
                identificationResult = "",
                error = null
            )
        }
    }

    fun onDocumentSelected(pdfBytes: ByteArray, fileName: String) {
        _state.update { currentState ->
            currentState.copy(
                selectedBitmap = null,
                pdfBytes = pdfBytes,
                fileName = fileName,
                isPdf = true,
                identificationResult = "",
                error = null
            )
        }
    }

    fun identifyMedicine() {
        val state = _state.value
        val bitmap = state.selectedBitmap
        val pdfBytes = state.pdfBytes
        val isPdf = state.isPdf
        val fileName = state.fileName

        if (bitmap == null && !isPdf) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            identifyMedicineUseCase(bitmap, pdfBytes, fileName, isPdf)
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

    fun setError(message: String) {
        _state.update { it.copy(error = message) }
    }
}
