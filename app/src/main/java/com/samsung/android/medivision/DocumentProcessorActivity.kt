package com.samsung.android.medivision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.samsung.android.medivision.di.AppModule
import com.samsung.android.medivision.presentation.documentprocessor.DocumentProcessorScreen

class DocumentProcessorActivity : ComponentActivity() {

    // IMPORTANT: Replace with your actual Open Router API Key
    private val apiKey = "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"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize dependency injection
        AppModule.initialize(apiKey)
        
        // Create ViewModel
        val viewModel = AppModule.provideDocumentProcessorViewModel()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DocumentProcessorScreen(viewModel = viewModel)
                }
            }
        }
    }
}
