package com.example.kamara

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Datu-base ez-erlazionalerako (Cloud Firestore) sarbidea.
 * Ez da Firebase Storage erabiltzen (argazkiak ez dira gordetzen, ez dohainik delako).
 */
class ErrezetaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val bilduma = db.collection("errezetak")

    /** Errezeta testua soilik gorde (OCR-ek irakurritako testu osoa). */
    suspend fun gorde(errezeta: Errezeta) {
        val dokumentua = bilduma.document() // auto-id
        dokumentua.set(errezeta.copy(id = dokumentua.id)).await()
    }

    /** Errezeta existentea eguneratu (dokumentu bera mantenduta). */
    suspend fun eguneratu(errezeta: Errezeta) {
        bilduma.document(errezeta.id).set(errezeta).await()
    }

    /** Errezeta guztiak, berrienak lehenengo. */
    suspend fun lortuGuztiak(): List<Errezeta> =
        bilduma.orderBy("sortuta", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Errezeta::class.java)

    /** Errezeta ezabatu (Firestore-eko dokumentua soilik). */
    suspend fun ezabatu(errezeta: Errezeta) {
        bilduma.document(errezeta.id).delete().await()
    }
}
