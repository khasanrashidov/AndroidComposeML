package com.example.mlkit

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions


@ExperimentalGetImage
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}


@Composable
@ExperimentalGetImage
fun MyApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderState = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            cameraProviderState.value = cameraProviderFuture.get()
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {}
    }

    val onObjectDetected = { detectedObjects: List<DetectedObject> ->
        // Handle the detected objects here.
        for (obj in detectedObjects) {
            val label = obj.labels.firstOrNull()?.text ?: "unknown"
            val confidence = obj.labels.firstOrNull()?.confidence ?: 0f
            Log.d("ObjectDetection", "Detected object: $label, Confidence: $confidence")
        }
    }

    cameraProviderState.value?.let { cameraProvider ->
        CameraView(cameraProvider, lifecycleOwner, onObjectDetected)
    }
}


@Composable
@ExperimentalGetImage
fun CameraView(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    onObjectDetected: (List<DetectedObject>) -> Unit
) {
    val context = LocalContext.current

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            bindToLifecycle(lifecycleOwner)
        }
    }

    val imageAnalysis =
        ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

    val objectDetector = getObjectDetector()

    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy: ImageProxy ->
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            objectDetector.process(inputImage).addOnSuccessListener { detectedObjects ->
                    onObjectDetected(detectedObjects)
                }.addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
    cameraController.cameraSelector = cameraSelector

    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }, modifier = Modifier.fillMaxSize()
    ) { previewView ->
        previewView.controller = cameraController
    }
}


fun getObjectDetector(): ObjectDetector {
    val options = ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableClassification().build()

    return ObjectDetection.getClient(options)
}
