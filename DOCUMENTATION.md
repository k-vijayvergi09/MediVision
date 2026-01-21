# MediVision - Application Documentation

## Overview

MediVision is a healthcare Android application designed to help users manage their prescriptions and identify medicines. The app leverages AI/ML technologies to automatically extract medicine information from prescription images and PDFs, and provides real-time medicine identification using camera capture.

---

## Features

### 1. Prescription Upload & Management

**Location:** `presentation/prescriptionupload/`

This feature allows users to upload and manage their medical prescriptions digitally.

#### How It Works:

1. **Upload Prescription** - Users can select an image or PDF of their prescription from their device
2. **AI Processing** - The app sends the document to OpenRouter API (using GPT-4o model) which analyzes the prescription
3. **Medicine Extraction** - The AI extracts structured data including:
   - Medicine name
   - Dosage timing (Morning, Evening, or Both)
   - Frequency (Daily, Every other day, Weekly, etc.)
4. **Local Storage** - Extracted data is saved locally for offline access
5. **History View** - Users can view all saved prescriptions in an expandable list format
6. **Management** - Individual prescriptions can be deleted, or all history can be cleared

#### User Flow:
```
Select Image/PDF → AI Processing → Extract Medicines → Save Locally → Display Results
```

#### Key Components:
- `PrescriptionUploadScreen.kt` - UI composable with image picker and history display
- `PrescriptionUploadViewModel.kt` - Manages state and coordinates business logic
- `PrescriptionUploadState.kt` - Holds UI state (loading, medicines, prescriptions)

---

### 2. Medicine Identifier / Scanner

**Location:** `presentation/scanmedicine/`

This feature enables real-time medicine identification from images using AI vision and on-device OCR.

#### How It Works:

1. **Capture/Upload** - Users can take a photo using the camera or upload an existing image
2. **Time-Aware Detection** - The app determines current time of day (Morning/Evening) to filter relevant medicines
3. **Context Loading** - Retrieves saved prescriptions and filters medicines applicable for the current time
4. **OCR Processing** - ML Kit performs on-device text recognition to find medicine names in the image
5. **Bounding Box Detection** - Locates the position of each identified medicine in the image
6. **Visual Overlay** - Draws bounding boxes around detected medicines on the image
7. **Results Display** - Shows a list of identified medicines with their detection status

#### User Flow:
```
Capture/Upload Image → Load Prescription Context → ML Kit OCR → Match Medicines → Draw Bounding Boxes → Display Results
```

#### Key Components:
- `ScanMedicineScreen.kt` - UI with camera integration and bounding box overlay
- `ScanMedicineViewModel.kt` - Orchestrates detection pipeline
- `ScanMedicineState.kt` - Holds detected medicines and bounding box data

---

## Architecture

### Pattern: MVVM + Clean Architecture

The app follows a layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │   Screens   │  │  ViewModels │  │   State Classes │  │
│  │  (Compose)  │  │             │  │                 │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                     DOMAIN LAYER                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │   Models    │  │  Use Cases  │  │   Repository    │  │
│  │             │  │             │  │   Interfaces    │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                      DATA LAYER                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │ Repository  │  │ API Clients │  │  Local Storage  │  │
│  │   Impls     │  │             │  │                 │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   MEDIVISION SDK                         │
│  ┌─────────────────────┐  ┌─────────────────────────┐   │
│  │  OpenRouter Client  │  │    Moondream Client     │   │
│  └─────────────────────┘  └─────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

---

## Package Structure

| Package | Description |
|---------|-------------|
| `com.samsung.android.medivision` | Main application entry point |
| `presentation/prescriptionupload` | Prescription upload feature (Screen, ViewModel, State) |
| `presentation/scanmedicine` | Medicine scanner feature (Screen, ViewModel, State) |
| `domain/model` | Business entities (Medicine, PrescriptionContext, etc.) |
| `domain/repository` | Repository interface definitions |
| `domain/usecase` | Business logic (IdentifyMedicine, ProcessPrescription) |
| `domain/ocr` | OCR provider abstraction |
| `data/repository` | Repository implementations |
| `data/ocr` | ML Kit OCR implementation |
| `data/storage` | SharedPreferences storage manager |
| `data/util` | Utilities and AI prompts |
| `di` | Dependency injection (AppModule) |
| `medivision-sdk` | External API clients module |

---

## Data Flow

### Prescription Upload Flow

```
┌──────────┐    ┌────────────┐    ┌─────────────┐    ┌────────────┐
│  User    │───▶│  ViewModel │───▶│   UseCase   │───▶│ Repository │
│  Action  │    │            │    │             │    │            │
└──────────┘    └────────────┘    └─────────────┘    └────────────┘
                                                            │
                                                            ▼
                                                    ┌────────────┐
                                                    │ OpenRouter │
                                                    │    API     │
                                                    └────────────┘
                                                            │
                                                            ▼
┌──────────┐    ┌────────────┐    ┌─────────────┐    ┌────────────┐
│    UI    │◀───│   State    │◀───│   Storage   │◀───│  Extracted │
│  Update  │    │   Update   │    │   Manager   │    │    Data    │
└──────────┘    └────────────┘    └─────────────┘    └────────────┘
```

### Medicine Scan Flow

```
┌──────────┐    ┌────────────┐    ┌─────────────┐
│  Camera/ │───▶│  ViewModel │───▶│   Load      │
│  Upload  │    │            │    │ Prescriptions│
└──────────┘    └────────────┘    └─────────────┘
                                         │
                      ┌──────────────────┼──────────────────┐
                      ▼                  ▼                  ▼
               ┌────────────┐    ┌─────────────┐    ┌────────────┐
               │  ML Kit    │    │   Filter    │    │  OpenRouter│
               │    OCR     │    │  by Time    │    │  (backup)  │
               └────────────┘    └─────────────┘    └────────────┘
                      │                  │                  │
                      └──────────────────┼──────────────────┘
                                         ▼
                                  ┌────────────┐
                                  │  Match &   │
                                  │  Overlay   │
                                  └────────────┘
                                         │
                                         ▼
                                  ┌────────────┐
                                  │  Display   │
                                  │  Results   │
                                  └────────────┘
```

---

## State Management

The app uses **Kotlin StateFlow** for reactive state management:

```kotlin
// ViewModel
private val _state = MutableStateFlow(InitialState())
val state: StateFlow<State> = _state.asStateFlow()

// State updates
_state.update { currentState ->
    currentState.copy(isLoading = true)
}

// UI Collection (Compose)
val state by viewModel.state.collectAsStateWithLifecycle()
```

### PrescriptionUploadState
```kotlin
data class PrescriptionUploadState(
    val isLoading: Boolean = false,
    val medicines: List<Medicine> = emptyList(),
    val prescriptions: List<PrescriptionContext> = emptyList(),
    val errorMessage: String? = null
)
```

### ScanMedicineState
```kotlin
data class ScanMedicineState(
    val isLoading: Boolean = false,
    val selectedImageUri: Uri? = null,
    val selectedImageBitmap: Bitmap? = null,
    val prescriptionContext: List<PrescriptionContext> = emptyList(),
    val detectedMedicines: List<DetectedMedicine> = emptyList(),
    val errorMessage: String? = null
)
```

---

## API Integrations

### 1. OpenRouter API

**Base URL:** `https://openrouter.ai/api/v1/`

**Purpose:** AI-powered text and image analysis for prescription processing

**Models Used:**
- `openai/gpt-4o` - Primary model for medicine extraction
- `google/gemini-3-flash-preview` - Alternative for text processing

**Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/chat/completions` | Send image/text for AI processing |

**Capabilities:**
- `generateTextFromImage()` - Extract text from images
- `generateTextFromPdf()` - Extract text from PDF documents
- `generateTextFromText()` - Text-only processing

### 2. Moondream API

**Base URL:** `https://api.moondream.ai/v1/`

**Purpose:** Visual understanding and object detection

**Capabilities:**
| Method | Description |
|--------|-------------|
| `query()` | Visual Question Answering |
| `point()` | Object pointing with coordinates |
| `caption()` | Image captioning |
| `segment()` | Image segmentation |

### 3. ML Kit Text Recognition

**Type:** On-device (no API key required)

**Purpose:** Fast offline text extraction with bounding boxes

**Returns:**
- Text content at block, line, and element levels
- Bounding box coordinates for each text element

---

## Domain Models

### Medicine
```kotlin
data class Medicine(
    val name: String,
    val timing: MedicineTiming,  // MORNING, EVENING, BOTH
    val frequency: MedicineFrequency  // DAILY, EVERY_OTHER_DAY, etc.
)
```

### PrescriptionContext
```kotlin
data class PrescriptionContext(
    val id: String,           // UUID
    val medicines: List<Medicine>,
    val createdAt: String     // ISO timestamp
)
```

### DetectedMedicine
```kotlin
data class DetectedMedicine(
    val medicine: Medicine,
    val boundingBox: NormalizedBoundingBox?,  // Detection coordinates
    val isDetected: Boolean
)
```

### NormalizedBoundingBox
```kotlin
data class NormalizedBoundingBox(
    val left: Float,    // 0.0 - 1.0
    val top: Float,     // 0.0 - 1.0
    val right: Float,   // 0.0 - 1.0
    val bottom: Float   // 0.0 - 1.0
)
```

---

## Dependency Injection

The app uses a **manual Service Locator** pattern via `AppModule`:

```kotlin
object AppModule {
    private var apiKey: String = ""
    private var moondreamApiKey: String = ""
    private lateinit var applicationContext: Context

    fun initialize(context: Context, apiKey: String, moondreamApiKey: String)

    // Providers
    fun providePrescriptionUploadViewModel(): PrescriptionUploadViewModel
    fun provideScanMedicineViewModel(): ScanMedicineViewModel
    fun provideMlKitOcrClient(): MlKitOcrClient
    fun provideOcrProvider(): OcrProvider
    fun provideMedicineRepository(): MedicineRepository
    // ...
}
```

**Initialization in MainActivity:**
```kotlin
AppModule.initialize(
    applicationContext,
    BuildConfig.OPENROUTER_API_KEY,
    BuildConfig.MOONDREAM_API_KEY
)
```

---

## Local Storage

### PrescriptionContextManager

**Storage:** SharedPreferences
**Key:** `prescription_context`
**Format:** JSON serialized list

**Operations:**
| Method | Description |
|--------|-------------|
| `savePrescription()` | Add new prescription (max 50 stored) |
| `getPrescriptions()` | Retrieve all saved prescriptions |
| `deletePrescription()` | Remove specific prescription by ID |
| `clearAll()` | Delete all prescriptions |

---

## Technology Stack

### Core
| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.1.10 | Programming language |
| Android SDK | 36 (Target) / 31 (Min) | Platform |
| Jetpack Compose | 2024.12.01 | UI framework |
| Material 3 | Latest | Design system |

### Networking
| Library | Version | Purpose |
|---------|---------|---------|
| Retrofit | 2.11.0 | REST client |
| OkHttp | 4.12.0 | HTTP client |
| Kotlinx Serialization | 1.8.0 | JSON parsing |

### ML/AI
| Library | Version | Purpose |
|---------|---------|---------|
| ML Kit Text Recognition | 16.0.1 | On-device OCR |
| OpenRouter API | - | Cloud AI processing |
| Moondream API | - | Visual understanding |

### Other
| Library | Version | Purpose |
|---------|---------|---------|
| Coil | 2.7.0 | Image loading |
| Kotlinx Coroutines | 1.10.1 | Async operations |

---

## UI Components

### Main Screen Layout

The app uses a **HorizontalPager** with two tabs:

```
┌─────────────────────────────────────────┐
│              MediVision                  │
├─────────────────────────────────────────┤
│  [Upload Prescription] [Identify Medicine]│
├─────────────────────────────────────────┤
│                                         │
│           Tab Content Area              │
│                                         │
│                                         │
│                                         │
└─────────────────────────────────────────┘
```

### Key UI Elements

- **Image Picker** - Activity result contracts for gallery selection
- **Camera Capture** - Direct camera access with runtime permissions
- **Canvas Overlay** - Draws bounding boxes on detected medicines
- **Expandable Cards** - Shows prescription details
- **Loading Indicators** - CircularProgressIndicator during processing
- **Error Dialogs** - AlertDialog for error messages

---

## Permissions

| Permission | Purpose |
|------------|---------|
| `INTERNET` | API communication |
| `CAMERA` | Medicine photo capture |

---

## Configuration

### API Keys Setup

1. Create `local.properties` in project root
2. Add API keys:
   ```properties
   OPENROUTER_API_KEY=your_openrouter_key
   MOONDREAM_API_KEY=your_moondream_key
   ```
3. Keys are injected via BuildConfig at compile time

---

## Error Handling

The app implements comprehensive error handling:

1. **Result Pattern** - Use cases return `Result<T>` for success/failure
2. **Try-Catch Blocks** - Network and parsing errors are caught
3. **User Feedback** - Errors displayed via dialogs or state messages
4. **Fallbacks** - Empty lists returned on storage read failures
5. **Lenient JSON** - Parser configured with `ignoreUnknownKeys = true`

---

## Navigation

Simple two-tab navigation using HorizontalPager:

```kotlin
HorizontalPager(state = pagerState) { page ->
    when (page) {
        0 -> PrescriptionUploadScreen(viewModel)
        1 -> ScanMedicineScreen(viewModel)
    }
}
```

Tab selection syncs with pager position via coroutines.

---

## Build Configuration

```kotlin
android {
    namespace = "com.samsung.android.medivision"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.samsung.android.medivision"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

---

## Future Enhancements

Potential areas for improvement:
- Room database for more robust local storage
- Hilt for production-grade dependency injection
- Medicine reminder notifications
- Multi-language OCR support
- Offline AI processing
- Prescription sharing functionality
- Integration with pharmacy systems
