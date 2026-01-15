package com.samsung.android.medivision.presentation.prescriptionupload

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.medivision.data.storage.PrescriptionContextManager
import com.samsung.android.medivision.domain.usecase.IdentifyMedicineUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PrescriptionUploadViewModel(
    private val identifyMedicineUseCase: IdentifyMedicineUseCase,
    private val prescriptionContextManager: PrescriptionContextManager
) : ViewModel() {

    private val _state = MutableStateFlow(PrescriptionUploadState())
    val state: StateFlow<PrescriptionUploadState> = _state.asStateFlow()

    init {
        loadSavedPrescriptions()
    }

    fun onImageSelected(bitmap: Bitmap, fileName: String = "") {
        _state.update { currentState ->
            currentState.copy(
                selectedBitmap = bitmap,
                pdfBytes = null,
                fileName = fileName,
                isPdf = false,
                uploadResult = "",
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
                uploadResult = "",
                error = null
            )
        }
    }

    fun uploadPrescription() {
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
                    // Save the context to SharedPreferences
                    val savedContext = prescriptionContextManager.savePrescriptionContext(
                        fileName = fileName,
                        extractedText = identification.description,
                        medicines = identification.medicines,
                        isPdf = isPdf
                    )

                    val totalContexts = prescriptionContextManager.getAllPrescriptionContexts().size

                    _state.update {
                        it.copy(
                            uploadResult = "Successfully saved prescription context!\n\n" +
                                    "Total saved prescriptions: $totalContexts\n\n" +
                                    "Extracted information:\n${identification.description}",
                            isLoading = false,
                            error = null
                        )
                    }

                    // Reload the list
                    loadSavedPrescriptions()
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to upload prescription."
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

    fun loadSavedPrescriptions() {
        val prescriptions = prescriptionContextManager.getAllPrescriptionContexts()
        _state.update { it.copy(savedPrescriptions = prescriptions) }
    }

    fun deletePrescription(prescriptionId: String) {
        prescriptionContextManager.deletePrescription(prescriptionId)
        loadSavedPrescriptions()
    }

    fun clearAllPrescriptions() {
        prescriptionContextManager.clearAllContexts()
        loadSavedPrescriptions()
    }
}
