package com.samsung.android.medivision.presentation.documentprocessor

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.android.medivision.data.util.PdfUtils

@Composable
fun DocumentProcessorScreen(
    viewModel: DocumentProcessorViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.path?.substringAfterLast("/") ?: "Selected File"
            val mimeType = context.contentResolver.getType(it)

            val bitmap = if (mimeType == "application/pdf") {
                PdfUtils.renderPdfToBitmap(context, it)
            } else {
                if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }
            }

            bitmap?.let { bmp ->
                viewModel.onDocumentSelected(bmp, fileName)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            viewModel.onDocumentSelected(it, "Scanned medicines")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            viewModel.setError("Camera permission denied. Please enable it to scan medicines.")
        }
    }

    DocumentProcessorContent(
        bitmap = state.selectedBitmap,
        fileName = state.fileName,
        extractedText = state.extractedText,
        isLoading = state.isLoading,
        error = state.error,
        medicineCoordinates = state.medicineCoordinates,
        isDetectingCoordinates = state.isDetectingCoordinates,
        onSelectFile = { filePickerLauncher.launch("*/*") },
        onScanMedicines = {
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                PackageManager.PERMISSION_GRANTED -> cameraLauncher.launch(null)
                else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onProcessDocument = { viewModel.processPrescription() },
        onDismissError = { viewModel.clearError() }
    )
}

@Composable
private fun DocumentProcessorContent(
    bitmap: android.graphics.Bitmap?,
    fileName: String,
    extractedText: String,
    isLoading: Boolean,
    error: String?,
    medicineCoordinates: List<com.samsung.android.medivision_sdk.Point>?,
    isDetectingCoordinates: Boolean,
    onSelectFile: () -> Unit,
    onScanMedicines: () -> Unit,
    onProcessDocument: () -> Unit,
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
            text = "Prescription Scanner",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Upload a prescription (Image or PDF) to extract medicine details automatically.",
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
                ImageWithCoordinatesOverlay(
                    bitmap = bitmap,
                    coordinates = medicineCoordinates
                )
            } else {
                Text(text = "No prescription selected")
            }
        }

        if (fileName.isNotEmpty()) {
            Text(
                text = "File: $fileName",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (isDetectingCoordinates) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Detecting medicine coordinates...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (medicineCoordinates != null && medicineCoordinates.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Medicine Coordinates Detected:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    medicineCoordinates.forEachIndexed { index, point ->
                        Text(
                            text = "Medicine ${index + 1}: (${String.format("%.2f", point.x)}, ${String.format("%.2f", point.y)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "Note: Coordinates are normalized (0-1)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Button(
            onClick = onScanMedicines,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Scan medicines to verify")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSelectFile,
                modifier = Modifier.weight(1f)
            ) {
                Text("Select File")
            }

            Button(
                onClick = onProcessDocument,
                enabled = bitmap != null && !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Extract Info")
                }
            }
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

        if (extractedText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Extracted Medicine Information:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = extractedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageWithCoordinatesOverlay(
    bitmap: android.graphics.Bitmap,
    coordinates: List<com.samsung.android.medivision_sdk.Point>?
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    
    // Calculate aspect ratio
    val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                containerSize = layoutCoordinates.size
            }
    ) {
        // Calculate the displayed image size maintaining aspect ratio
        val containerWidth = containerSize.width.toFloat()
        val containerHeight = containerSize.height.toFloat()
        
        // Safety check for zero sizes
        if (containerWidth <= 0f || containerHeight <= 0f || imageAspectRatio <= 0f) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Document Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            return@Box
        }
        
        val containerAspectRatio = containerWidth / containerHeight
        
        val displayedWidth: Float
        val displayedHeight: Float
        val offsetX: Float
        val offsetY: Float
        
        if (imageAspectRatio > containerAspectRatio) {
            // Image is wider - fit to width
            displayedWidth = containerWidth
            displayedHeight = containerWidth / imageAspectRatio
            offsetX = 0f
            offsetY = (containerHeight - displayedHeight) / 2f
        } else {
            // Image is taller - fit to height
            displayedWidth = containerHeight * imageAspectRatio
            displayedHeight = containerHeight
            offsetX = (containerWidth - displayedWidth) / 2f
            offsetY = 0f
        }
        
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Document Preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        
        // Draw red dots overlay if coordinates are available
        if (coordinates != null && coordinates.isNotEmpty() && containerSize != IntSize.Zero) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .matchParentSize()
            ) {
                val dotRadius = with(density) { 12.dp.toPx() } // Red dot radius (bigger as requested)
                
                coordinates.forEach { point ->
                    // Convert normalized coordinates (0-1) to pixel coordinates
                    // Account for the actual displayed image bounds with aspect ratio maintained
                    val x = offsetX + (point.x * displayedWidth)
                    val y = offsetY + (point.y * displayedHeight)
                    
                    // Draw red circle
                    drawCircle(
                        color = Color.Red,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
