package com.example.kamara

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.IOException

/** Moztu beharreko eremua, 0..1 balio normalizatuetan. */
private data class MozketaGunea(val l: Float, val t: Float, val r: Float, val b: Float) {
    companion object {
        val OSOA = MozketaGunea(0f, 0f, 1f, 1f)
        val HASIERAKOA = MozketaGunea(0.08f, 0.08f, 0.92f, 0.92f)
    }
}

/**
 * Argazkia OCRra pasatu aurretik mozteko pantaila.
 *
 * - **Crop ikonoa** → argazkia pantalla osoan ireki; han **X** ikonoak mozketa ezabatzen
 *   du (irudi osoa hautatzen du) eta **OK** ikonoak mozketa onartzen du.
 * - **OK ikonoa** → argazkia aukeratu eta OCRra pasatu.
 *
 * Laukia **mugitu** (barruan arrastatuz) edo **tamainaz aldatu** (izkinak/ertzak
 * arrastatuz) daiteke.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoztuPantaila(
    uri: Uri,
    onMoztuta: (Bitmap) -> Unit,
    onUtzi: () -> Unit
) {
    val context = LocalContext.current
    var irudia by remember { mutableStateOf<Bitmap?>(null) }
    var errorea by remember { mutableStateOf<String?>(null) }
    // MutableState egonkorra: pointerInput-eko lambda-k beti balio eguneratua irakurtzen du
    val guneaState = remember { mutableStateOf(MozketaGunea.HASIERAKOA) }
    var pantallaOsokoModua by remember { mutableStateOf(false) }

    // Irudia kargatu (tamaina mugatuta) eta EXIF biraketa aplikatu
    LaunchedEffect(uri) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val lagin = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 2048)

            var decoded: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use {
                decoded = BitmapFactory.decodeStream(
                    it,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = lagin }
                )
            }

            val bmp = decoded ?: throw IOException("Ezin izan da irudia ireki")

            val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
                @Suppress("DEPRECATION")
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            val gradu = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            irudia = if (gradu != 0) {
                val matrix = Matrix().apply { postRotate(gradu.toFloat()) }
                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            } else {
                bmp
            }
        } catch (e: Exception) {
            errorea = e.message ?: "Errorea irudia kargatzean"
        }
    }

    /** Hautatutako eremua moztu eta OCRri pasatu. */
    fun aukeratuArgazkia() {
        val bmp = irudia ?: return
        val g = guneaState.value
        val x = (g.l * bmp.width).toInt().coerceIn(0, bmp.width - 1)
        val y = (g.t * bmp.height).toInt().coerceIn(0, bmp.height - 1)
        val w = ((g.r - g.l) * bmp.width).toInt()
            .coerceAtLeast(1).coerceAtMost(bmp.width - x)
        val h = ((g.b - g.t) * bmp.height).toInt()
            .coerceAtLeast(1).coerceAtMost(bmp.height - y)
        onMoztuta(Bitmap.createBitmap(bmp, x, y, w, h))
    }

    val bmp = irudia

    if (pantallaOsokoModua) {
        // ---- Argazkia pantalla osoan (mozketa doitzeko modua) ----
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (bmp != null) {
                MozketaCanvas(bmp, guneaState)
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // X: mozketa ezabatu -> irudi osoa hautatuta geratu
                IconButton(
                    onClick = { guneaState.value = MozketaGunea.OSOA },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        "Mozketa ezabatu",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // OK: mozketa onartu -> pantaila arruntera itzuli
                IconButton(
                    onClick = { pantallaOsokoModua = false },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Check,
                        "Mozketa onartu",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    } else {
        // ---- Pantaila arrunta ----
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Moztu argazkia") },
                    navigationIcon = {
                        IconButton(onClick = onUtzi) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atzera")
                        }
                    }
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Crop ikonoa: argazkia pantalla osoan ireki
                    IconButton(
                        onClick = { pantallaOsokoModua = true },
                        enabled = irudia != null,
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Crop,
                            "Moztu pantalla osoan",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(Modifier.width(48.dp))

                    // OK ikonoa: argazkia aukeratu eta OCRra pasatu
                    IconButton(
                        onClick = { aukeratuArgazkia() },
                        enabled = irudia != null,
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            "Argazkia aukeratu",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black)
            ) {
                when {
                    errorea != null -> {
                        Text(
                            "Errorea: ${errorea}",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    bmp == null -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        MozketaCanvas(bmp, guneaState)
                    }
                }
            }
        }
    }
}

/** Irudia marraztu eta laukia arrastatzeko canvas-a (modu bietan erabiltzen da). */
@Composable
private fun MozketaCanvas(
    bmp: Bitmap,
    guneaState: MutableState<MozketaGunea>
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // BoxWithConstraints-ek Dp ematen du; drawImage/drawRect/pointerInput px-etan aritzen dira
        val cw = with(density) { maxWidth.toPx() }
        val ch = with(density) { maxHeight.toPx() }
        val scale = minOf(cw / bmp.width, ch / bmp.height)
        val dw = bmp.width * scale
        val dh = bmp.height * scale
        val ox = (cw - dw) / 2f
        val oy = (ch - dh) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bmp, cw, ch) {
                    var modua = "bat ere ez"
                    var heldua = -1

                    detectDragGestures(
                        onDragStart = { start ->
                            val g = guneaState.value
                            val l = ox + g.l * dw
                            val t = oy + g.t * dh
                            val r = ox + g.r * dw
                            val b = oy + g.b * dh
                            val mx = (l + r) / 2f
                            val my = (t + b) / 2f

                            // 8 helduleku: 4 izkina + 4 ertz
                            val heldulekuak = listOf(
                                Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b),
                                Offset(mx, t), Offset(r, my), Offset(mx, b), Offset(l, my)
                            )
                            heldua = heldulekuak.indexOfFirst {
                                (it - start).getDistance() < 36.dp.toPx()
                            }
                            modua = when {
                                heldua >= 0 -> "handitu"
                                start.x in l..r && start.y in t..b -> "mugitu"
                                else -> "bat ere ez"
                            }
                        },
                        onDrag = { _, delta ->
                            val g = guneaState.value
                            when (modua) {
                                "mugitu" -> guneaState.value = mugitu(
                                    g, delta.x / dw, delta.y / dh
                                )
                                "handitu" -> if (heldua >= 0) {
                                    guneaState.value = handitu(
                                        g, heldua, delta.x / dw, delta.y / dh
                                    )
                                }
                            }
                        }
                    )
                }
        ) {
            drawImage(
                image = bmp.asImageBitmap(),
                dstOffset = IntOffset(ox.toInt(), oy.toInt()),
                dstSize = IntSize(dw.toInt(), dh.toInt())
            )

            val g = guneaState.value
            val l = ox + g.l * dw
            val t = oy + g.t * dh
            val r = ox + g.r * dw
            val b = oy + g.b * dh
            val mx = (l + r) / 2f
            val my = (t + b) / 2f
            val iluna = Color.Black.copy(alpha = 0.55f)

            // Kanpoaldea ilundu (lau laukizuzen)
            drawRect(iluna, topLeft = Offset(0f, 0f), size = Size(size.width, t))
            drawRect(iluna, topLeft = Offset(0f, b), size = Size(size.width, size.height - b))
            drawRect(iluna, topLeft = Offset(0f, t), size = Size(l, b - t))
            drawRect(iluna, topLeft = Offset(r, t), size = Size(size.width - r, b - t))

            // Hautatutako eremua eta ertza
            drawRect(
                Color.White.copy(alpha = 0.12f),
                topLeft = Offset(l, t),
                size = Size(r - l, b - t)
            )
            drawRect(
                Color.White,
                topLeft = Offset(l, t),
                size = Size(r - l, b - t),
                style = Stroke(3.dp.toPx())
            )

            // 8 helduleku ikusgarri
            val erdia = 10.dp.toPx()
            listOf(
                Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b),
                Offset(mx, t), Offset(r, my), Offset(mx, b), Offset(l, my)
            ).forEach { p ->
                drawRect(
                    Color.White,
                    topLeft = p - Offset(erdia, erdia),
                    size = Size(erdia * 2, erdia * 2)
                )
            }
        }
    }
}

/** Laukia mugitu, mugen barruan mantenduta. */
private fun mugitu(g: MozketaGunea, dx: Float, dy: Float): MozketaGunea {
    val w = g.r - g.l
    val h = g.b - g.t
    val nl = (g.l + dx).coerceIn(0f, 1f - w)
    val nt = (g.t + dy).coerceIn(0f, 1f - h)
    return MozketaGunea(nl, nt, nl + w, nt + h)
}

/**
 * Heldulekua arrastatu tamaina aldatzeko:
 * 0=goi-ezkerra, 1=goi-eskuina, 2=behe-ezkerra, 3=behe-eskuina,
 * 4=goi-ertza, 5=eskuin-ertza, 6=behe-ertza, 7=ezker-ertza.
 */
private fun handitu(g: MozketaGunea, heldulekua: Int, dx: Float, dy: Float): MozketaGunea {
    val min = 0.08f
    var l = g.l
    var t = g.t
    var r = g.r
    var b = g.b
    when (heldulekua) {
        0 -> {
            l = (l + dx).coerceIn(0f, r - min)
            t = (t + dy).coerceIn(0f, b - min)
        }
        1 -> {
            r = (r + dx).coerceIn(l + min, 1f)
            t = (t + dy).coerceIn(0f, b - min)
        }
        2 -> {
            l = (l + dx).coerceIn(0f, r - min)
            b = (b + dy).coerceIn(t + min, 1f)
        }
        3 -> {
            r = (r + dx).coerceIn(l + min, 1f)
            b = (b + dy).coerceIn(t + min, 1f)
        }
        4 -> t = (t + dy).coerceIn(0f, b - min)
        5 -> r = (r + dx).coerceIn(l + min, 1f)
        6 -> b = (b + dy).coerceIn(t + min, 1f)
        7 -> l = (l + dx).coerceIn(0f, r - min)
    }
    return MozketaGunea(l, t, r, b)
}
