package com.example.kamara

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Datu-base ez-erlazionalerako (Cloud Firestore) eta argazkien biltegirako (Storage)
 * sarbidea biltzen duen repository-a.
 */
class ErrezetaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val bilduma = db.collection("errezetak")

    /** Argazkia Storage-ra igo eta bere download URLa itzultzen du. Ez badago, null. */
    private suspend fun igoArgazkia(uri: Uri?): String? {
        if (uri == null) return null
        val erreferentzia = storage.reference
            .child("errezetak/${UUID.randomUUID()}.jpg")
        erreferentzia.putFile(uri).await()
        return erreferentzia.downloadUrl.await().toString()
    }

    /** Errezeta berria gorde: argazkia badago, lehenik igo eta URLa Firestore-en gorde. */
    suspend fun gorde(errezeta: Errezeta, argazkiaUri: Uri?) {
        val url = igoArgazkia(argazkiaUri)
        val dokumentua = bilduma.document() // auto-id
        val gordetzeko = errezeta.copy(
            id = dokumentua.id,
            argazkiaUrl = url ?: errezeta.argazkiaUrl
        )
        dokumentua.set(gordetzeko).await()
    }

    /** Errezeta guztiak, berrienak lehenengo. */
    suspend fun lortuGuztiak(): List<Errezeta> =
        bilduma.orderBy("sortuta", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Errezeta::class.java)

    /** Errezeta ezabatu, baita bere argazkia Storage-n badago. */
    suspend fun ezabatu(errezeta: Errezeta) {
        bilduma.document(errezeta.id).delete().await()
        errezeta.argazkiaUrl?.let { url ->
            runCatching { storage.getReferenceFromUrl(url).delete().await() }
        }
    }
}
