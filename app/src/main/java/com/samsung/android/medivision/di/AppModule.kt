package com.samsung.android.medivision.di

import com.samsung.android.medivision.data.repository.DocumentRepositoryImpl
import com.samsung.android.medivision.data.repository.MedicineRepositoryImpl
import com.samsung.android.medivision.domain.repository.DocumentRepository
import com.samsung.android.medivision.domain.repository.MedicineRepository
import com.samsung.android.medivision.domain.usecase.IdentifyMedicineUseCase
import com.samsung.android.medivision.domain.usecase.ProcessPrescriptionUseCase
import com.samsung.android.medivision.presentation.documentprocessor.DocumentProcessorViewModel
import com.samsung.android.medivision.presentation.medicineidentification.MedicineIdentificationViewModel
import com.samsung.android.medivision_sdk.OpenRouterClient

object AppModule {
    
    private var apiKey: String = ""
    
    fun initialize(apiKey: String) {
        this.apiKey = apiKey
    }
    
    private fun provideOpenRouterClient(): OpenRouterClient {
        return OpenRouterClient(apiKey = apiKey)
    }
    
    // Medicine Identification Dependencies
    private fun provideMedicineRepository(): MedicineRepository {
        return MedicineRepositoryImpl(provideOpenRouterClient())
    }
    
    private fun provideIdentifyMedicineUseCase(): IdentifyMedicineUseCase {
        return IdentifyMedicineUseCase(provideMedicineRepository())
    }
    
    fun provideMedicineIdentificationViewModel(): MedicineIdentificationViewModel {
        return MedicineIdentificationViewModel(provideIdentifyMedicineUseCase())
    }
    
    // Document Processor Dependencies
    private fun provideDocumentRepository(): DocumentRepository {
        return DocumentRepositoryImpl(provideOpenRouterClient())
    }
    
    private fun provideProcessPrescriptionUseCase(): ProcessPrescriptionUseCase {
        return ProcessPrescriptionUseCase(provideDocumentRepository())
    }
    
    fun provideDocumentProcessorViewModel(): DocumentProcessorViewModel {
        return DocumentProcessorViewModel(provideProcessPrescriptionUseCase())
    }
}
