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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
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
    val hasDocument = bitmap != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Medicine Scanner",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Scan medicines to find out insights",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Main Content
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Document Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .border(
                            width = 2.dp,
                            color = if (hasDocument) {
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        ImageWithCoordinatesOverlay(
                            bitmap = bitmap,
                            coordinates = medicineCoordinates
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No image loaded",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Upload an image or scan with camera",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // File name display
            if (fileName.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Detecting coordinates indicator
            if (isDetectingCoordinates) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Detecting medicine coordinates...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Medicine coordinates display
            if (medicineCoordinates != null && medicineCoordinates.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Medicine Locations Detected",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        medicineCoordinates.forEachIndexed { index, point ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Medicine ${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "(${String.format("%.2f", point.x)}, ${String.format("%.2f", point.y)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📍 Red dots show detected medicine positions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action Buttons
            FilledTonalButton(
                onClick = onScanMedicines,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan to Verify",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSelectFile,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload")
                }

                Button(
                    onClick = onProcessDocument,
                    enabled = hasDocument && !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing")
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extract")
                    }
                }
            }

            // Error Display
            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        TextButton(onClick = onDismissError) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Extracted Information Display
            if (extractedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Extracted Medicine Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
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
