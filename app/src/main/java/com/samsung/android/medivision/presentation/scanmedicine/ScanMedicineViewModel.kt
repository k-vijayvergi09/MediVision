package com.samsung.android.medivision.presentation.scanmedicine

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.medivision.data.storage.PrescriptionContextManager
import com.samsung.android.medivision.domain.usecase.ProcessPrescriptionUseCase
import com.samsung.android.medivision_sdk.OpenRouterClient
import com.samsung.android.medivision_sdk.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ScanMedicineViewModel(
    private val processPrescriptionUseCase: ProcessPrescriptionUseCase,
    private val openRouterClient: OpenRouterClient,
    private val prescriptionContextManager: PrescriptionContextManager
) : ViewModel() {

    private val _state = MutableStateFlow(ScanMedicineState())
    val state: StateFlow<ScanMedicineState> = _state.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Data class for coordinate detection response from OpenRouter.
     */
    @Serializable
    data class CoordinateResponse(
        val coordinates: List<Coordinate>
    )

    @Serializable
    data class Coordinate(
        val x: Float,
        val y: Float
    )

    /**
     * Detects medicine coordinates using OpenRouter API with vision capabilities.
     * Returns normalized coordinates (0-1) for the medicine location in the image.
     */
    private suspend fun detectMedicineCoordinates(
        bitmap: Bitmap,
        medicineName: String
    ): List<Point>? {
        return try {
            Log.d("ViewModel", "=== detectMedicineCoordinates() API call ===")
            Log.d("ViewModel", "Request medicine name: '$medicineName'")

            val prompt = """
                Analyze this image and find the location of the medicine named "$medicineName".

                Look for:
                - Exact name match
                - Abbreviations (like "Para" for "Paracetamol", "ASP" for "Aspirin")
                - Partial names
                - Brand names for this generic medicine

                If you find the medicine, return its location as normalized coordinates (0-1 range, where 0,0 is top-left and 1,1 is bottom-right).
                Return the center point of where the medicine name is visible.

                Respond ONLY with valid JSON in this exact format:
                {
                  "coordinates": [
                    {"x": 0.5, "y": 0.3}
                  ]
                }

                If the medicine is not found, return:
                {
                  "coordinates": []
                }

                Do not include any other text, explanation, or markdown formatting. Only return the JSON object.
            """.trimIndent()

            Log.d("ViewModel", "Calling OpenRouter API with vision prompt...")

            val response = openRouterClient.generateTextFromImage(
                model = "openai/gpt-4o",
                prompt = prompt,
                bitmap = bitmap
            )

            Log.d("ViewModel", "Raw API response: '$response'")

            if (response == null) {
                Log.w("ViewModel", "Null response from OpenRouter API")
                return null
            }

            // Clean up the response - remove markdown code blocks if present
            val cleanedResponse = response
                .replace("```json", "")
                .replace("```", "")
                .trim()

            Log.d("ViewModel", "Cleaned response: '$cleanedResponse'")

            // Parse JSON response
            val coordinateResponse = try {
                json.decodeFromString<CoordinateResponse>(cleanedResponse)
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to parse coordinate response: ${e.message}", e)
                return null
            }

            if (coordinateResponse.coordinates.isEmpty()) {
                Log.i("ViewModel", "No coordinates found in response")
                return emptyList()
            }

            // Convert to Point objects
            val points = coordinateResponse.coordinates.map { coord ->
                Point(x = coord.x, y = coord.y)
            }

            Log.i("ViewModel", "Successfully parsed ${points.size} coordinate(s):")
            points.forEachIndexed { index, point ->
                Log.d("ViewModel", "  Point $index: (${point.x}, ${point.y})")
            }

            points

        } catch (e: Exception) {
            Log.e("ViewModel", "Error in detectMedicineCoordinates()", e)
            Log.e("ViewModel", "Exception message: ${e.message}")
            null
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
     * Then detects their coordinates in the scanned image using OpenRouter API.
     */
    private fun detectApplicableMedicines(bitmap: Bitmap) {
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

                        // Use OpenRouter API to detect medicine coordinates
                        Log.d("ViewModel", "Calling OpenRouter API to detect: '${medicine.name}'")

                        val coordinates = detectMedicineCoordinates(bitmap, medicine.name)

                        if (coordinates == null) {
                            Log.w("ViewModel", "✗ API call failed for '${medicine.name}'")
                        } else if (coordinates.isEmpty()) {
                            Log.w("ViewModel", "✗ Medicine '${medicine.name}' not found in image")
                        } else {
                            Log.i("ViewModel", "OpenRouter API returned ${coordinates.size} coordinate(s)")
                            coordinates.forEachIndexed { coordIndex, point ->
                                Log.d("ViewModel", "  Coordinate $coordIndex: x=${point.x}, y=${point.y}")
                            }

                            // Take the first coordinate as best match
                            val bestMatch = coordinates.first()
                            allCoordinates.add(bestMatch)
                            detectedMedicines.add(medicine.name)
                            Log.i("ViewModel", "✓ Successfully detected '${medicine.name}' at (${bestMatch.x}, ${bestMatch.y})")
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
