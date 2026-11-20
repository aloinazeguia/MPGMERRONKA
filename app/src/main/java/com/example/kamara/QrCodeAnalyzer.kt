package com.example.kamara

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * QR/kode barrak irakurri eta testua itzultzen duen analizatzailea.
 * Behin bakarrik deitzen du QR bera ikusten den bitartean (behin eta berriz ez pasatzeko).
 */
class QrCodeAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var azkenQr: String? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        //rawValue -> QR-aren benetako edukia (lehen URL finko bat zegoen)
                        barcode.rawValue?.let { qrValue ->
                            if (qrValue != azkenQr) {
                                azkenQr = qrValue
                                onQrDetected(qrValue)
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close() // IMPRESCINDIBLE cerrar el fotograma
                }
        } else {
            imageProxy.close()
        }
    }
}
