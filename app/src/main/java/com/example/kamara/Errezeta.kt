package com.example.kamara

/**
 * Errezeta baten datu-eredua (Cloud Firestore-eko "errezetak" bildumako dokumentua).
 *
 * Balio lehenetsiak behar dira Firestore-ek dokumentua deserializatu ahal izateko
 * (parametro gabeko konstruktorea behar baitu).
 */
data class Errezeta(
    val id: String = "",
    val izena: String = "",
    val argazkiaUrl: String? = null,   // null -> ez dago argazkirik
    val osagaiak: List<String> = emptyList(),
    val pausoak: List<String> = emptyList(),
    val sortuta: Long = System.currentTimeMillis()
)
