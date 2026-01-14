package com.samsung.android.medivision

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.samsung.android.medivision.di.AppModule
import com.samsung.android.medivision.presentation.medicineidentification.MedicineIdentificationScreen

class MedicineIdentificationActivity : ComponentActivity() {

    private val apiKey = "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"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize dependency injection
        AppModule.initialize(apiKey)
        
        // Create ViewModel
        val viewModel = AppModule.provideMedicineIdentificationViewModel()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MedicineIdentificationScreen(
                        viewModel = viewModel,
                        onNavigateToProcessor = {
                            startActivity(Intent(this, DocumentProcessorActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}
