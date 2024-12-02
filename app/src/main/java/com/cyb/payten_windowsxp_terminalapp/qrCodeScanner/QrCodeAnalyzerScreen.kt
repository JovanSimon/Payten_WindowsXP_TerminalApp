package com.cyb.payten_windowsxp_terminalapp.qrCodeScanner

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.BackpressureStrategy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.qrCodeAnalyzer(
    route: String,
    onClose: () -> Unit,
    navigate: (String) -> Unit
) = composable(
    route = route
) {
    val qrCodeAnalyzerViewModel: QrCodeAnalyzerViewModel = hiltViewModel<QrCodeAnalyzerViewModel>()
    val state = qrCodeAnalyzerViewModel.state.collectAsState()

    QrCodeAnalyzerScreen(
        state = state.value,
        eventPublisher = {
            qrCodeAnalyzerViewModel.setEvent(it)
        },
        onClose = onClose,
        navigate = navigate
    )
}

@Composable
fun QrCodeAnalyzerScreen(
    state: QrCodeAnalyzerContract.QrCodeAnalyzerUiState,
    eventPublisher: (uiEvent: QrCodeAnalyzerContract.QrCodeAnalyzerUiEvent) -> Unit,
    onClose: () -> Unit,
    navigate: (String) -> Unit
) {
    if (state.loadedQR) {
        navigate("payment")
    } else {
        var newCode by remember {
            mutableStateOf("")
        }
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProviderFuture = remember {
            ProcessCameraProvider.getInstance(context)
        }

        var hasCameraPromission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                hasCameraPromission = granted
            }
        )
        LaunchedEffect(key1 = true) {
            launcher.launch(Manifest.permission.CAMERA)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (hasCameraPromission) {

                AndroidView(
                    factory = { context ->
                        val previewView = PreviewView(context)
                        val preview = Preview.Builder().build()
                        val selector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        imageAnalysis.setAnalyzer(
                            ContextCompat.getMainExecutor(context),
                            QrCodeAnalyzer { result ->
                                newCode = result
                                eventPublisher(
                                    QrCodeAnalyzerContract.QrCodeAnalyzerUiEvent.SetDataStore(
                                        newCode
                                    )
                                )
                            }
                        )
                        try {
                            cameraProviderFuture.get().bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        previewView
                    },
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}