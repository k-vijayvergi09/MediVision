package com.samsung.android.medivision.presentation.scanmedicine

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.android.medivision.data.ocr.NormalizedBoundingBox
import com.samsung.android.medivision.data.util.PdfUtils
import java.util.concurrent.Executors

@Composable
fun ScanMedicineScreen(
    viewModel: ScanMedicineViewModel
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
            viewModel.onDocumentSelected(it, "Camera capture")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (state.isRealTimeMode) {
                // Permission granted while trying to enable real-time mode — toggle is already on
            } else {
                cameraLauncher.launch(null)
            }
        } else {
            viewModel.setError("Camera permission denied. Please enable it to identify medicines.")
        }
    }

    // Permission-aware toggle handler
    val onToggleRealTime: () -> Unit = {
        if (!state.isRealTimeMode) {
            // Turning ON — check permission first
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                PackageManager.PERMISSION_GRANTED -> viewModel.toggleRealTimeMode()
                else -> {
                    viewModel.toggleRealTimeMode() // Set flag so UI is ready
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        } else {
            viewModel.toggleRealTimeMode()
        }
    }

    ScanMedicineContent(
        bitmap = state.selectedBitmap,
        fileName = state.fileName,
        extractedText = state.extractedText,
        isLoading = state.isLoading,
        error = state.error,
        medicineCoordinates = state.medicineCoordinates,
        detectedMedicines = state.detectedMedicines,
        isDetectingCoordinates = state.isDetectingCoordinates,
        isRealTimeMode = state.isRealTimeMode,
        onToggleRealTime = onToggleRealTime,
        onSelectFile = { filePickerLauncher.launch("*/*") },
        onScanMedicines = {
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                PackageManager.PERMISSION_GRANTED -> cameraLauncher.launch(null)
                else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onProcessDocument = { viewModel.processPrescription() },
        onDismissError = { viewModel.clearError() },
        onFrameForOcr = { bitmap -> viewModel.processFrameForOcr(bitmap) },
        cameraFrameWidth = state.cameraFrameWidth,
        cameraFrameHeight = state.cameraFrameHeight
    )
}

@Composable
private fun ScanMedicineContent(
    bitmap: android.graphics.Bitmap?,
    fileName: String,
    extractedText: String,
    isLoading: Boolean,
    error: String?,
    medicineCoordinates: List<com.samsung.android.medivision_sdk.Point>?,
    detectedMedicines: List<DetectedMedicine>,
    isDetectingCoordinates: Boolean,
    isRealTimeMode: Boolean,
    onToggleRealTime: () -> Unit,
    onSelectFile: () -> Unit,
    onScanMedicines: () -> Unit,
    onProcessDocument: () -> Unit,
    onDismissError: () -> Unit,
    onFrameForOcr: (android.graphics.Bitmap) -> Unit,
    cameraFrameWidth: Int,
    cameraFrameHeight: Int
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
                    text = "Medicine Identifier",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Identify medicines in real-time or from images",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Real-time toggle
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Real-time detection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isRealTimeMode,
                        onCheckedChange = { onToggleRealTime() }
                    )
                }
            }
        }

        // Main Content
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Preview Card — either live camera or static image
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
                            color = if (isRealTimeMode || hasDocument) {
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRealTimeMode) {
                        RealTimeCameraPreview(
                            detectedMedicines = detectedMedicines,
                            cameraFrameWidth = cameraFrameWidth,
                            cameraFrameHeight = cameraFrameHeight,
                            onFrameForOcr = onFrameForOcr
                        )
                    } else if (bitmap != null) {
                        ImageWithBoundingBoxOverlay(
                            bitmap = bitmap,
                            coordinates = medicineCoordinates,
                            detectedMedicines = detectedMedicines
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

            // File name display (only in capture mode)
            if (!isRealTimeMode && fileName.isNotEmpty()) {
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
                            text = "Analyzing medicines for current time...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Medicine detection display (ML Kit OCR bounding boxes)
            if (detectedMedicines.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isRealTimeMode) "Medicines Detected (Live)" else "Medicines Found (ML Kit OCR)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "Found ${detectedMedicines.size} medicine(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Green boxes on the image highlight detected medicines",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else if (!isRealTimeMode && medicineCoordinates != null && medicineCoordinates.isNotEmpty()) {
                // Fallback to coordinates display if using old API
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Medicines Found in Image",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "Found ${medicineCoordinates.size} medicine location(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Green dots on the image show where the medicines are located",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action Buttons — only shown in capture mode
            if (!isRealTimeMode) {
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
                        text = "Capture Image",
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
                            Text("Identify")
                        }
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
                            text = "Identified Medicine Information",
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
private fun RealTimeCameraPreview(
    detectedMedicines: List<DetectedMedicine>,
    cameraFrameWidth: Int,
    cameraFrameHeight: Int,
    onFrameForOcr: (android.graphics.Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { containerSize = it.size }
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                try {
                                    val rawBitmap = imageProxy.toBitmap()
                                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                                    // Rotate bitmap to match the display orientation.
                                    // The raw sensor image is typically landscape; PreviewView
                                    // auto-rotates but ImageAnalysis does not.
                                    val bitmap = if (rotationDegrees != 0) {
                                        val matrix = android.graphics.Matrix().apply {
                                            postRotate(rotationDegrees.toFloat())
                                        }
                                        android.graphics.Bitmap.createBitmap(
                                            rawBitmap, 0, 0,
                                            rawBitmap.width, rawBitmap.height,
                                            matrix, true
                                        )
                                    } else {
                                        rawBitmap
                                    }

                                    onFrameForOcr(bitmap)
                                } catch (e: Exception) {
                                    Log.e("CameraPreview", "Frame conversion error: ${e.message}")
                                } finally {
                                    imageProxy.close()
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Camera bind failed: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Bounding box overlay on camera feed
        if (detectedMedicines.isNotEmpty() && containerSize != IntSize.Zero
            && cameraFrameWidth > 0 && cameraFrameHeight > 0
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = with(density) { 3.dp.toPx() }
                val containerWidth = containerSize.width.toFloat()
                val containerHeight = containerSize.height.toFloat()

                // FILL_CENTER: the image is scaled to fill the container, then centered
                // (parts of the image may be cropped).
                val imageAspectRatio = cameraFrameWidth.toFloat() / cameraFrameHeight.toFloat()
                val containerAspectRatio = containerWidth / containerHeight

                val scaledImageWidth: Float
                val scaledImageHeight: Float
                if (imageAspectRatio > containerAspectRatio) {
                    // Image is wider — fit height, crop sides
                    scaledImageHeight = containerHeight
                    scaledImageWidth = containerHeight * imageAspectRatio
                } else {
                    // Image is taller — fit width, crop top/bottom
                    scaledImageWidth = containerWidth
                    scaledImageHeight = containerWidth / imageAspectRatio
                }

                val offsetX = (scaledImageWidth - containerWidth) / 2f
                val offsetY = (scaledImageHeight - containerHeight) / 2f

                detectedMedicines.forEach { medicine ->
                    val box = medicine.boundingBox

                    // Map normalized coords (0-1 of full image) to screen pixels,
                    // accounting for the crop offset from FILL_CENTER.
                    val left = box.left * scaledImageWidth - offsetX
                    val top = box.top * scaledImageHeight - offsetY
                    val boxWidth = box.width * scaledImageWidth
                    val boxHeight = box.height * scaledImageHeight

                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(left, top),
                        size = Size(boxWidth, boxHeight),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageWithBoundingBoxOverlay(
    bitmap: android.graphics.Bitmap,
    coordinates: List<com.samsung.android.medivision_sdk.Point>?,
    detectedMedicines: List<DetectedMedicine>
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

        // Draw bounding boxes for detected medicines (ML Kit OCR)
        if (detectedMedicines.isNotEmpty() && containerSize != IntSize.Zero) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .matchParentSize()
            ) {
                val strokeWidth = with(density) { 3.dp.toPx() }

                detectedMedicines.forEach { medicine ->
                    val box = medicine.boundingBox

                    // Convert normalized coordinates to pixel coordinates
                    val left = offsetX + (box.left * displayedWidth)
                    val top = offsetY + (box.top * displayedHeight)
                    val boxWidth = box.width * displayedWidth
                    val boxHeight = box.height * displayedHeight

                    // Draw green rectangle around the medicine
                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(left, top),
                        size = Size(boxWidth, boxHeight),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }
        // Fallback: Draw green dots overlay if only coordinates are available (old API)
        else if (coordinates != null && coordinates.isNotEmpty() && containerSize != IntSize.Zero) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .matchParentSize()
            ) {
                val dotRadius = with(density) { 8.dp.toPx() }

                coordinates.forEach { point ->
                    // Convert normalized coordinates (0-1) to pixel coordinates
                    val x = offsetX + (point.x * displayedWidth)
                    val y = offsetY + (point.y * displayedHeight)

                    // Draw green circle
                    drawCircle(
                        color = Color.Green,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
