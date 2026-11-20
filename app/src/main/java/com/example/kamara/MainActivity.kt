package com.example.kamara

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kamara.ui.theme.KamaraTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KamaraTheme {
                val context = LocalContext.current
                var pantaila by remember { mutableStateOf("hasiera") }
                var formularioaIturria by remember { mutableStateOf("errezetak") }
                // Argazkitik OCR bidez irakurritako testu osoa formulariora eramateko
                var irakurritakoTestua by remember { mutableStateOf<String?>(null) }
                // Moztu pantailan dagoen argazkia
                var moztukoUri by remember { mutableStateOf<Uri?>(null) }
                // Errezetak pantailatik editatzen ari den errezeta
                var editatzenAriDenErrezeta by remember { mutableStateOf<Errezeta?>(null) }
                var irakurtzen by remember { mutableStateOf(false) }
                val vm: ErrezetaViewModel = viewModel()

                // OCR: aukeratutako (moztutako) irudia irakurri
                fun ocrEgin(bitmap: Bitmap) {
                    irakurtzen = true
                    val onAmaituta: (String) -> Unit = { testua ->
                        irakurtzen = false
                        if (testua.isNotBlank()) {
                            irakurritakoTestua = testua
                            editatzenAriDenErrezeta = null
                            formularioaIturria = "kamera"
                            pantaila = "formularioa"
                        } else {
                            Toast.makeText(
                                context,
                                "Ez da testurik aurkitu argazkian",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    val onErrorea: (String) -> Unit = { msg ->
                        irakurtzen = false
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    TestuIrakurlea.irakurri(bitmap, onAmaituta, onErrorea)
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (pantaila) {
                        "hasiera" -> HasieraPantaila(
                            onKamera = { pantaila = "kamera" },
                            onErrezetak = { pantaila = "errezetak"; vm.kargatu() }
                        )

                        "kamera" -> ComposableCameraScreen(
                            onAtzera = { pantaila = "hasiera" },
                            onArgazkiaAtera = { uri ->
                                moztukoUri = uri
                                pantaila = "moztu"
                            }
                        )

                        "moztu" -> {
                            val uri = moztukoUri
                            if (uri != null) {
                                MoztuPantaila(
                                    uri = uri,
                                    onMoztuta = { bitmap -> ocrEgin(bitmap) },
                                    onUtzi = { pantaila = "kamera" }
                                )
                            } else {
                                Text(
                                    "Ez dago argazkirik mozteko.",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        "errezetak" -> ErrezetakPantaila(
                            vm = vm,
                            onAtzera = { pantaila = "hasiera" },
                            onGehitu = {
                                irakurritakoTestua = null
                                editatzenAriDenErrezeta = null
                                formularioaIturria = "errezetak"
                                pantaila = "formularioa"
                            },
                            onEditatu = { e ->
                                editatzenAriDenErrezeta = e
                                irakurritakoTestua = null
                                formularioaIturria = "errezetak"
                                pantaila = "formularioa"
                            }
                        )

                        "formularioa" -> ErrezetaFormularioa(
                            vm = vm,
                            hasierakoTestua = irakurritakoTestua,
                            editatzenAriDenErrezeta = editatzenAriDenErrezeta,
                            onAtzera = { pantaila = formularioaIturria },
                            onAmaituta = { pantaila = "errezetak" }
                        )
                    }

                    // OCR bitartean kargatzen ari dela adierazi
                    if (irakurtzen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(Modifier.height(12.dp))
                                Text("Testua irakurtzen...", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Pantaila nagusia: bi botoi soilik -> Kamera eta Errezetak. */
@Composable
fun HasieraPantaila(onKamera: () -> Unit, onErrezetak: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
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
    onArgazkiaAtera: (Uri) -> Unit
) {
    // Kamera baimena soilik (ez GPS, ez Storage)
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
            onArgazkiaAtera = onArgazkiaAtera
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
    onArgazkiaAtera: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var argazkiaHartzen by remember { mutableStateOf(false) }

    // Zoomaren kontrolerako: kamera lotuta dagoen objektua eta uneko balioak
    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var zoomMin by remember { mutableStateOf(1f) }
    var zoomMax by remember { mutableStateOf(1f) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    // Zoom maila aldatu (gailuaren mugen barruan mantenduta)
    val aldatuZoom: (Float) -> Unit = { faktorea ->
        camera?.let { cam ->
            cam.cameraInfo.zoomState.value?.let { z ->
                val berria = (z.zoomRatio * faktorea).coerceIn(z.minZoomRatio, z.maxZoomRatio)
                cam.cameraControl.setZoomRatio(berria)
            }
        }
    }

    // Kamera konfiguratu (vista previa + argazkia)
    LaunchedEffect(cameraSelector) {
        val cameraProviderFuture =
            androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            try {
                // Kamera aldatzean, kendu aurreko zoom-behatzailea
                camera?.cameraInfo?.zoomState?.removeObservers(lifecycleOwner)
                cameraProvider.unbindAll()
                val cam = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                camera = cam
                cam.cameraInfo.zoomState.observe(lifecycleOwner) { z ->
                    zoomRatio = z.zoomRatio
                    zoomMin = z.minZoomRatio
                    zoomMax = z.maxZoomRatio
                }
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

        // 4. Miniatura (azken argazkia)
        lastPhotoUri?.let { uri ->
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(32.dp)
                    .size(56.dp)
                    .zIndex(10f)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Última foto tomada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 5. Obturadorea -> argazkia atera, lokalean gorde eta mozteko pantailara
        Button(
            onClick = {
                if (!argazkiaHartzen) {
                    argazkiaHartzen = true
                    takePhotoAndSaveLocal(context, imageCapture) { uri ->
                        argazkiaHartzen = false
                        if (uri != null) {
                            lastPhotoUri = uri
                            onArgazkiaAtera(uri)
                        }
                    }
                }
            },
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            enabled = !argazkiaHartzen,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .size(70.dp)
                .zIndex(10f)
        ) {
            // Sin texto
        }

        // 6. Zoom kontrolak (+ / -) eta uneko maila
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .zIndex(10f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = { aldatuZoom(1.2f) },
                enabled = zoomRatio < zoomMax,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.ZoomIn, "Zoom handitu", tint = Color.White)
            }

            Text(
                "${"%.1f".format(zoomRatio)}x",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )

            IconButton(
                onClick = { aldatuZoom(1f / 1.2f) },
                enabled = zoomRatio > zoomMin,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.ZoomOut, "Zoom txikitu", tint = Color.White)
            }
        }
    }
}

/** Argazkia lokalean gorde (gallerian), ez Firebase Storage-n (dohainik ez delako). */
private fun takePhotoAndSaveLocal(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoCaptured: (Uri?) -> Unit
) {
    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        .format(System.currentTimeMillis())

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$name.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MiAppCompose")
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onPhotoCaptured(outputFileResults.savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(
                    context,
                    "Error al guardar la foto: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onPhotoCaptured(null)
            }
        }
    )
}
