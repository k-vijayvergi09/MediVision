package com.samsung.android.medivision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.samsung.android.medivision.di.AppModule
import com.samsung.android.medivision.presentation.documentprocessor.DocumentProcessorScreen
import com.samsung.android.medivision.presentation.medicineidentification.MedicineIdentificationScreen

class MainActivity : ComponentActivity() {

    private val apiKey =
        "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"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize dependency injection once for the whole app
        AppModule.initialize(apiKey)

        setContent {
            val navController = rememberNavController()

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "medicine_identification"
                    ) {
                        composable(route = "medicine_identification") {
                            val viewModel = AppModule.provideMedicineIdentificationViewModel()
                            MedicineIdentificationScreen(
                                viewModel = viewModel,
                                onNavigateToProcessor = {
                                    navController.navigate("document_processor")
                                }
                            )
                        }

                        composable(route = "document_processor") {
                            val viewModel = AppModule.provideDocumentProcessorViewModel()
                            DocumentProcessorScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

