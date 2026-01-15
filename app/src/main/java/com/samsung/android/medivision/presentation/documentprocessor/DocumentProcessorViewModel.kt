package com.samsung.android.medivision.presentation.documentprocessor

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.medivision.domain.usecase.ProcessPrescriptionUseCase
import com.samsung.android.medivision_sdk.MoondreamClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DocumentProcessorViewModel(
    private val processPrescriptionUseCase: ProcessPrescriptionUseCase,
    private val moondreamClient: MoondreamClient?
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentProcessorState())
    val state: StateFlow<DocumentProcessorState> = _state.asStateFlow()
    
    // Extract the object type from the prescription prompt - default to "medicine"
    private val medicineObjectType = "head"

    fun onDocumentSelected(bitmap: Bitmap, fileName: String) {
        _state.update { currentState ->
            currentState.copy(
                selectedBitmap = bitmap,
                fileName = fileName,
                extractedText = "",
                error = null,
                medicineCoordinates = null
            )
        }
        
        // Automatically detect medicine coordinates when photo is taken
        detectMedicineCoordinates(bitmap)
    }
    
    /**
     * Detects medicine coordinates using Moondream API.
     * Uses the point method to get precise center coordinates for medicines.
     */
    private fun detectMedicineCoordinates(bitmap: Bitmap) {
        if (moondreamClient == null) {
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isDetectingCoordinates = true, error = null) }
            
            try {
                Log.i("ViewModel", "detecting point")
                val coordinates = moondreamClient.point(bitmap, medicineObjectType)
                _state.update { 
                    it.copy(
                        medicineCoordinates = coordinates,
                        isDetectingCoordinates = false
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isDetectingCoordinates = false,
                        error = "Failed to detect medicine coordinates: ${e.message}"
                    )
                }
            }
        }
    }

    fun processPrescription() {
        val bitmap = _state.value.selectedBitmap ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            processPrescriptionUseCase(bitmap)
                .onSuccess { prescriptionData ->
                    _state.update {
                        it.copy(
                            extractedText = prescriptionData.extractedText,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to process prescription."
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
