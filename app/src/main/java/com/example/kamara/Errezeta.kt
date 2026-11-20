package com.example.kamara

/**
 * Errezeta baten datu-eredua (Cloud Firestore-eko "errezetak" bildumako dokumentua).
 *
 * Bi modu onartzen dira:
 *  - **Klasikoa** (Errezetak pantailako + botoia): `izena`, `osagaiak`, `pausoak` eremu bereiziak.
 *  - **OCR** (Kamera): `testua` eremu bakarra, irakurritako lerro guztiekin.
 *
 * Firestore-ek dokumentuan ez dauden eremuak default balioekin uzten ditu,
 * beraz bi moduetako dokumentuak deserializatu daitezke.
 */
data class Errezeta(
    val id: String = "",
    // --- Modu klasikoa ---
    val izena: String = "",
    val osagaiak: List<String> = emptyList(),
    val pausoak: List<String> = emptyList(),
    // --- Modu OCR ---
    val testua: String = "",
    val sortuta: Long = System.currentTimeMillis()
)
