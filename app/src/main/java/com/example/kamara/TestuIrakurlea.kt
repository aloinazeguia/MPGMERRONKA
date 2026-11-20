package com.example.kamara

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Argazki bati OCR (testu-ezagutza) aplikatu eta irakurritako testu osoa itzultzen du.
 * ML Kit-ek lokalean egiten du, ez du internetik behar eta dohainik da.
 */
object TestuIrakurlea {

    /** Argazki osoa (Uri) irakurri. */
    fun irakurri(
        context: Context,
        uri: Uri,
        onAmaituta: (String) -> Unit,
        onErrorea: (String) -> Unit = {}
    ) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            onErrorea("Ezin izan da irudia prozesatu: ${e.message}")
            return
        }
        prozesatu(image, onAmaituta, onErrorea)
    }

    /** Moztutako irudia (Bitmap) irakurri. Biratzea 0 da: bitmap-a jada zuzen dago. */
    fun irakurri(
        bitmap: Bitmap,
        onAmaituta: (String) -> Unit,
        onErrorea: (String) -> Unit = {}
    ) {
        val image = try {
            InputImage.fromBitmap(bitmap, 0)
        } catch (e: Exception) {
            onErrorea("Ezin izan da irudia prozesatu: ${e.message}")
            return
        }
        prozesatu(image, onAmaituta, onErrorea)
    }

    private fun prozesatu(
        image: InputImage,
        onAmaituta: (String) -> Unit,
        onErrorea: (String) -> Unit
    ) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Lerroak mantenduz testu osoa itzuli (ez da eremurik bereizten)
                onAmaituta(visionText.text)
            }
            .addOnFailureListener { e ->
                onErrorea("Errorea testua irakurtzean: ${e.message}")
            }
    }
}
