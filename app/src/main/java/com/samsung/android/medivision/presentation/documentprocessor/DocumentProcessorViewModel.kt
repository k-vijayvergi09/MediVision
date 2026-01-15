package com.samsung.android.medivision.presentation.documentprocessor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.medivision.domain.usecase.ProcessPrescriptionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DocumentProcessorViewModel(
    private val processPrescriptionUseCase: ProcessPrescriptionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentProcessorState())
    val state: StateFlow<DocumentProcessorState> = _state.asStateFlow()

    fun onDocumentSelected(bitmap: Bitmap, fileName: String) {
        _state.update { currentState ->
            currentState.copy(
                selectedBitmap = bitmap,
                fileName = fileName,
                extractedText = "",
                error = null
            )
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
