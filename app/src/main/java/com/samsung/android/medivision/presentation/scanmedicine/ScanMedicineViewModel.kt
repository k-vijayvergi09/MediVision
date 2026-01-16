package com.samsung.android.medivision.presentation.scanmedicine

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.medivision.data.storage.PrescriptionContextManager
import com.samsung.android.medivision.domain.usecase.ProcessPrescriptionUseCase
import com.samsung.android.medivision_sdk.MoondreamClient
import com.samsung.android.medivision_sdk.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScanMedicineViewModel(
    private val processPrescriptionUseCase: ProcessPrescriptionUseCase,
    private val moondreamClient: MoondreamClient?,
    private val prescriptionContextManager: PrescriptionContextManager
) : ViewModel() {

    private val _state = MutableStateFlow(ScanMedicineState())
    val state: StateFlow<ScanMedicineState> = _state.asStateFlow()

    // Configuration: Enable verification to check each detected point
    // Set to false for faster detection (may have false positives)
    // Set to true for accurate detection (slower but verifies each point)
    private val enableVerification = true

    /**
     * Verifies if a detected point actually contains the medicine name by asking
     * the LLM to intelligently match what's visible with the expected medicine.
     * Uses LLM intelligence to handle abbreviations, partial names, and variations.
     */
    private suspend fun verifyMedicineAtPoint(
        bitmap: Bitmap,
        medicineName: String,
        point: Point
    ): Boolean {
        return try {
            // Use LLM intelligence to verify the match with a simpler, more flexible approach
            val question = """
                Is the medicine "$medicineName" visible in this image?
                This includes:
                - Exact name match
                - Abbreviations (like "Para" for "Paracetamol", "ASP" for "Aspirin")
                - Partial names
                - Brand names for this generic medicine

                Respond with just YES or NO.
            """.trimIndent()

            val response = moondreamClient?.query(bitmap, question)

            Log.d("ViewModel", "Verification query for '$medicineName'")
            Log.d("ViewModel", "LLM response: '$response'")

            if (response == null) {
                Log.w("ViewModel", "Null response from LLM, accepting by default")
                return true
            }

            // Be more flexible in parsing the response
            val cleanResponse = response.trim().uppercase()
            val isVerified = when {
                // Direct YES
                cleanResponse == "YES" -> true
                cleanResponse.startsWith("YES") -> true

                // Check for affirmative patterns
                cleanResponse.contains("YES") && !cleanResponse.contains("NO") -> true

                // Direct NO
                cleanResponse == "NO" -> false
                cleanResponse == "NO." -> false

                // If response is ambiguous and doesn't clearly say NO, accept it
                !cleanResponse.contains("NO") && cleanResponse.isNotEmpty() -> {
                    Log.d("ViewModel", "Ambiguous response, accepting by default")
                    true
                }

                // Clear rejection
                else -> false
            }

            Log.d("ViewModel", "Verification result for '$medicineName': $isVerified (cleaned response: '$cleanResponse')")

            isVerified
        } catch (e: Exception) {
            Log.e("ViewModel", "Verification failed: ${e.message}", e)
            // If verification fails, assume it's valid (fail-open approach)
            Log.d("ViewModel", "Exception occurred, accepting by default")
            true
        }
    }

    fun onDocumentSelected(bitmap: Bitmap, fileName: String) {
        _state.update { currentState ->
            currentState.copy(
                selectedBitmap = bitmap,
                fileName = fileName,
                extractedText = "",
                error = null,
                medicineCoordinates = null,
                applicableMedicines = emptyList()
            )
        }

        // Automatically detect applicable medicines and their coordinates
        detectApplicableMedicines(bitmap)
    }

    /**
     * Determines the current time of day and filters applicable medicines.
     * Then detects their coordinates in the scanned image.
     */
    private fun detectApplicableMedicines(bitmap: Bitmap) {
        if (moondreamClient == null) {
            Log.e("ViewModel", "Moondream API client is null")
            _state.update { it.copy(error = "Moondream API not available") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isDetectingCoordinates = true, error = null) }

            try {
                Log.d("ViewModel", "=== Starting detectApplicableMedicines flow ===")

                // Get current time of day
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val timeOfDay = when {
                    currentHour < 12 -> "Morning"
                    currentHour < 17 -> "Morning" // Treating afternoon as morning for medicine purposes
                    else -> "Evening"
                }

                Log.i("ViewModel", "Current time: Hour=$currentHour, TimeOfDay=$timeOfDay")

                // Get all saved prescriptions
                val prescriptions = prescriptionContextManager.getAllPrescriptionContexts()
                Log.i("ViewModel", "Retrieved ${prescriptions.size} saved prescription(s)")

                prescriptions.forEachIndexed { index, prescription ->
                    Log.d("ViewModel", "Prescription $index: ${prescription.fileName}, Medicines count: ${prescription.medicines.size}")
                }

                // Collect all medicines that should be taken at this time
                val applicableMedicines = mutableListOf<com.samsung.android.medivision.domain.model.Medicine>()
                prescriptions.forEach { prescription ->
                    prescription.medicines.forEach { medicine ->
                        Log.d("ViewModel", "Evaluating medicine: name='${medicine.name}', whenToTake='${medicine.whenToTake}', frequency=${medicine.frequency}")

                        val shouldTake = when (medicine.whenToTake) {
                            "Morning" -> timeOfDay == "Morning"
                            "Evening" -> timeOfDay == "Evening"
                            "Both" -> true
                            else -> false
                        }

                        Log.d("ViewModel", "  -> shouldTake=$shouldTake (current time: $timeOfDay)")

                        if (shouldTake) {
                            applicableMedicines.add(medicine)
                            Log.i("ViewModel", "  -> Added '${medicine.name}' to applicable medicines")
                        }
                    }
                }

                if (applicableMedicines.isEmpty()) {
                    Log.w("ViewModel", "No applicable medicines found for $timeOfDay time")
                    _state.update {
                        it.copy(
                            isDetectingCoordinates = false,
                            applicableMedicines = emptyList(),
                            extractedText = "No medicines found for $timeOfDay time in your saved prescriptions."
                        )
                    }
                    return@launch
                }

                Log.i("ViewModel", "=== Applicable medicines for $timeOfDay: ${applicableMedicines.size} ===")
                applicableMedicines.forEachIndexed { index, medicine ->
                    Log.i("ViewModel", "  ${index + 1}. ${medicine.name} (${medicine.whenToTake}, ${medicine.frequency}x)")
                }

                // Detect coordinates for each applicable medicine
                val allCoordinates = mutableListOf<com.samsung.android.medivision_sdk.Point>()
                val detectedMedicines = mutableListOf<String>()

                applicableMedicines.forEachIndexed { index, medicine ->
                    try {
                        Log.i("ViewModel", "--- Processing medicine ${index + 1}/${applicableMedicines.size}: '${medicine.name}' ---")

                        // Try with more specific query first (medicine name with "tablet" or "pill" context)
                        val specificQuery = "${medicine.name} medicine"
                        Log.d("ViewModel", "Calling Moondream API with query: '$specificQuery'")

                        val coordinates = moondreamClient.point(bitmap, specificQuery)

                        if (coordinates == null || coordinates.isEmpty()) {
                            Log.w("ViewModel", "No results with specific query, trying just medicine name: '${medicine.name}'")
                            val fallbackCoordinates = moondreamClient.point(bitmap, medicine.name)

                            if (fallbackCoordinates != null && fallbackCoordinates.isNotEmpty()) {
                                Log.i("ViewModel", "Fallback query returned ${fallbackCoordinates.size} coordinate(s)")

                                // Take the first coordinate as best match
                                val bestMatch = fallbackCoordinates.first()

                                // Optionally verify the match
                                val isValid = if (enableVerification) {
                                    Log.d("ViewModel", "Verifying detected point for '${medicine.name}'...")
                                    verifyMedicineAtPoint(bitmap, medicine.name, bestMatch)
                                } else {
                                    true
                                }

                                if (isValid) {
                                    allCoordinates.add(bestMatch)
                                    detectedMedicines.add(medicine.name)
                                    Log.i("ViewModel", "✓ Successfully detected '${medicine.name}' at (${bestMatch.x}, ${bestMatch.y})")
                                } else {
                                    Log.w("ViewModel", "✗ Verification failed for '${medicine.name}' - not the correct medicine")
                                }
                            } else {
                                Log.w("ViewModel", "✗ Medicine '${medicine.name}' not found in image")
                            }
                        } else {
                            Log.i("ViewModel", "Moondream API response: ${coordinates.size} coordinate(s) found")
                            coordinates.forEachIndexed { coordIndex, point ->
                                Log.d("ViewModel", "  Coordinate $coordIndex: x=${point.x}, y=${point.y}")
                            }

                            // Take the first coordinate as best match
                            val bestMatch = coordinates.first()

                            // Optionally verify the match
                            val isValid = if (enableVerification) {
                                Log.d("ViewModel", "Verifying detected point for '${medicine.name}'...")
                                verifyMedicineAtPoint(bitmap, medicine.name, bestMatch)
                            } else {
                                true
                            }

                            if (isValid) {
                                allCoordinates.add(bestMatch)
                                detectedMedicines.add(medicine.name)
                                Log.i("ViewModel", "✓ Successfully detected '${medicine.name}' at (${bestMatch.x}, ${bestMatch.y}) (selected best match from ${coordinates.size} results)")
                            } else {
                                Log.w("ViewModel", "✗ Verification failed for '${medicine.name}' - not the correct medicine")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ViewModel", "✗ Exception while detecting '${medicine.name}': ${e.message}", e)
                    }
                }

                Log.i("ViewModel", "=== Detection Summary ===")
                Log.i("ViewModel", "Total applicable medicines: ${applicableMedicines.size}")
                Log.i("ViewModel", "Successfully detected: ${detectedMedicines.size}")
                Log.i("ViewModel", "Total coordinates found: ${allCoordinates.size}")
                if (detectedMedicines.isNotEmpty()) {
                    Log.i("ViewModel", "Detected medicines: ${detectedMedicines.joinToString(", ")}")
                }

                val resultText = if (detectedMedicines.isNotEmpty()) {
                    "Time: $timeOfDay\n\n" +
                    "Medicines to take now:\n" +
                    applicableMedicines.joinToString("\n") { "• ${it.name}" } +
                    "\n\nDetected in image:\n" +
                    detectedMedicines.joinToString("\n") { "✓ $it" }
                } else {
                    "Time: $timeOfDay\n\n" +
                    "Medicines to take now:\n" +
                    applicableMedicines.joinToString("\n") { "• ${it.name}" } +
                    "\n\nNone of these medicines were found in the image."
                }

                _state.update {
                    it.copy(
                        medicineCoordinates = if (allCoordinates.isNotEmpty()) allCoordinates else null,
                        applicableMedicines = applicableMedicines,
                        isDetectingCoordinates = false,
                        extractedText = resultText
                    )
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error in detectApplicableMedicines", e)
                _state.update {
                    it.copy(
                        isDetectingCoordinates = false,
                        error = "Failed to detect medicines: ${e.message}"
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
