package com.samsung.android.medivision.presentation.medicineidentification

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MedicineIdentificationScreen(
    viewModel: MedicineIdentificationViewModel,
    onNavigateToProcessor: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.path?.substringAfterLast("/") ?: "Selected File"
            val mimeType = context.contentResolver.getType(it)

            if (mimeType == "application/pdf") {
                // For PDFs, read the file as bytes
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val pdfBytes = inputStream.readBytes()
                        viewModel.onDocumentSelected(pdfBytes, fileName)
                    }
                } catch (e: Exception) {
                    viewModel.setError("Failed to read PDF file: ${e.message}")
                }
            } else {
                // For images, load as bitmap
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }

                bitmap?.let { bmp ->
                    viewModel.onImageSelected(bmp, fileName)
                }
            }
        }
    }

    MedicineIdentificationContent(
        bitmap = state.selectedBitmap,
        fileName = state.fileName,
        resultText = state.identificationResult,
        isLoading = state.isLoading,
        error = state.error,
        isPdf = state.isPdf,
        onSelectFile = { filePickerLauncher.launch("*/*") },
        onIdentifyMedicine = { viewModel.identifyMedicine() },
        onNavigateToProcessor = onNavigateToProcessor,
        onDismissError = { viewModel.clearError() }
    )
}

@Composable
private fun MedicineIdentificationContent(
    bitmap: Bitmap?,
    fileName: String,
    resultText: String,
    isLoading: Boolean,
    error: String?,
    isPdf: Boolean,
    onSelectFile: () -> Unit,
    onIdentifyMedicine: () -> Unit,
    onNavigateToProcessor: () -> Unit,
    onDismissError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Medicine Identifier",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Select an image or PDF document to identify the medicine.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .height(300.dp)
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected Document",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isPdf) {
                Text(
                    text = "PDF Document: $fileName",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(text = "No document selected")
            }
        }

        if (fileName.isNotEmpty()) {
            Text(
                text = "File: $fileName",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Button(
            onClick = onSelectFile,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text("Select Image or PDF")
        }

        Button(
            onClick = onIdentifyMedicine,
            enabled = (bitmap != null || isPdf) && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Identify Medicine")
            }
        }

        OutlinedButton(
            onClick = onNavigateToProcessor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text("Go to Document Processor")
        }

        if (error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = onDismissError) {
                        Text("Dismiss")
                    }
                }
            }
        }

        if (resultText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = resultText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
