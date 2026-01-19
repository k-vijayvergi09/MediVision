package com.samsung.android.medivision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.samsung.android.medivision.di.AppModule
import com.samsung.android.medivision.presentation.scanmedicine.ScanMedicineScreen
import com.samsung.android.medivision.presentation.prescriptionupload.PrescriptionUploadScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize dependency injection once for the whole app
        AppModule.initialize(
            applicationContext,
            BuildConfig.OPENROUTER_API_KEY,
            BuildConfig.MOONDREAM_API_KEY
        )

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TabbedScreen()
                }
            }
        }
    }

    @Composable
    private fun TabbedScreen() {
        val pagerState = rememberPagerState(pageCount = { 2 })
        val coroutineScope = rememberCoroutineScope()
        val tabs = listOf("Upload Prescription", "Identify Medicine")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        val viewModel = AppModule.providePrescriptionUploadViewModel()
                        PrescriptionUploadScreen(
                            viewModel = viewModel
                        )
                    }
                    1 -> {
                        val viewModel = AppModule.provideScanMedicineViewModel()
                        ScanMedicineScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

