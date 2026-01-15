package com.samsung.android.medivision.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.samsung.android.medivision.domain.model.PrescriptionContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class PrescriptionContextManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "prescription_context",
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun savePrescriptionContext(
        fileName: String,
        extractedText: String,
        isPdf: Boolean
    ): PrescriptionContext {
        val context = PrescriptionContext(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            extractedText = extractedText,
            timestamp = System.currentTimeMillis(),
            isPdf = isPdf
        )

        val existingContexts = getAllPrescriptionContexts().toMutableList()
        existingContexts.add(0, context) // Add to beginning for most recent first

        // Keep only last 50 prescriptions to avoid data bloat
        val contextsToSave = existingContexts.take(50)

        val jsonString = json.encodeToString(contextsToSave)
        sharedPreferences.edit()
            .putString(KEY_PRESCRIPTION_CONTEXTS, jsonString)
            .apply()

        return context
    }

    fun getAllPrescriptionContexts(): List<PrescriptionContext> {
        val jsonString = sharedPreferences.getString(KEY_PRESCRIPTION_CONTEXTS, null)
            ?: return emptyList()

        return try {
            json.decodeFromString<List<PrescriptionContext>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getMostRecentContext(): PrescriptionContext? {
        return getAllPrescriptionContexts().firstOrNull()
    }

    fun deletePrescription(prescriptionId: String) {
        val existingContexts = getAllPrescriptionContexts().toMutableList()
        val updatedContexts = existingContexts.filter { it.id != prescriptionId }

        if (updatedContexts.isEmpty()) {
            clearAllContexts()
        } else {
            val jsonString = json.encodeToString(updatedContexts)
            sharedPreferences.edit()
                .putString(KEY_PRESCRIPTION_CONTEXTS, jsonString)
                .apply()
        }
    }

    fun clearAllContexts() {
        sharedPreferences.edit()
            .remove(KEY_PRESCRIPTION_CONTEXTS)
            .apply()
    }

    companion object {
        private const val KEY_PRESCRIPTION_CONTEXTS = "prescription_contexts"
    }
}
