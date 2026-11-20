package com.example.kamara

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kamara.ui.theme.KamaraTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KamaraTheme {
                // Pantailen arteko nabigazio sinplea
                var pantaila by remember { mutableStateOf("hasiera") }
                var formularioaIturria by remember { mutableStateOf("errezetak") }
                // QR-tik irakurritako datuak formulariora eramateko
                var eskaneatutakoErrezeta by remember { mutableStateOf<Errezeta?>(null) }
                val vm: ErrezetaViewModel = viewModel()

                when (pantaila) {
                    "hasiera" -> HasieraPantaila(
                        onKamera = { pantaila = "kamera" },
                        onErrezetak = { pantaila = "errezetak"; vm.kargatu() }
                    )

                    "kamera" -> ComposableCameraScreen(
                        onAtzera = { pantaila = "hasiera" },
                        onErrezetaEskaneatuta = { testua ->
                            eskaneatutakoErrezeta = ErrezetaParser.parse(testua)
                            formularioaIturria = "kamera"
                            pantaila = "formularioa"
                        }
                    )

                    "errezetak" -> ErrezetakPantaila(
                        vm = vm,
                        onAtzera = { pantaila = "hasiera" },
                        onGehitu = {
                            eskaneatutakoErrezeta = null
                            formularioaIturria = "errezetak"
                            pantaila = "formularioa"
                        }
                    )

                    "formularioa" -> ErrezetaFormularioa(
                        vm = vm,
                        hasierakoErrezeta = eskaneatutakoErrezeta,
                        onAtzera = { pantaila = formularioaIturria },
                        onAmaituta = { pantaila = "errezetak" }
                    )
                }
            }
        }
    }
}

/** Pantaila nagusia: bi botoi soilik -> Kamera eta Errezetak. */
@Composable
fun HasieraPantaila(onKamera: () -> Unit, onErrezetak: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Errezetak", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onKamera, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Kamera")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onErrezetak, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Errezetak")
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ComposableCameraScreen(
    onAtzera: () -> Unit,
    onErrezetaEskaneatuta: (String) -> Unit
) {
    // Orain bakarrik kamera baimena behar da (ez GPS, ez argazkia)
    val permissionsState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.CAMERA)
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    val cameraGranted = permissionsState.permissions.find {
        it.permission == Manifest.permission.CAMERA
    }?.status?.isGranted ?: false

    if (cameraGranted) {
        CameraPreviewContent(
            onAtzera = onAtzera,
            onQrIrakurrita = onErrezetaEskaneatuta
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = onAtzera,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(48.dp)
                    .zIndex(10f)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .clip(CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atzera", tint = Color.White)
            }
            Text("Se requiere permiso de cámara para continuar.")
        }
    }
}

@Composable
fun CameraPreviewContent(
    onAtzera: () -> Unit,
    onQrIrakurrita: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    val previewView = remember { PreviewView(context) }

    // QR bakarrik: ez dago ImageCapture, ez argazkia, ez GPS
    LaunchedEffect(cameraSelector) {
        val cameraProviderFuture =
            androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        QrCodeAnalyzer { qrTexto ->
                            onQrIrakurrita(qrTexto)
                        }
                    )
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Vista previa de la cámara
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // 2. Atzera botoia
        IconButton(
            onClick = onAtzera,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .size(48.dp)
                .zIndex(10f)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .clip(CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Atzera",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // 3. Argibide testua
        Text(
            "Ekarri errezetaren QR kodea kamerara",
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 24.dp, start = 32.dp, end = 32.dp)
                .zIndex(10f)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )

        // 4. Kamera alderantzikatzeko botoia
        IconButton(
            onClick = {
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .size(56.dp)
                .zIndex(10f)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                Icons.Default.Refresh,
                "Invertir cámara",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
