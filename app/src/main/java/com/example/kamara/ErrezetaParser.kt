package com.example.kamara

/**
 * QR kodean datorren testu egituratua parseatu eta [Errezeta] batera bihurtzen du.
 *
 * Onartutako formatu adibidea:
 * ```
 * Izena: Patata tortilla
 * Osagaiak:
 * - 5 patata
 * - 3 arrautza
 * - Tipula
 * Pausoak:
 * 1. Patatak zuritu eta moztu
 * 2. Olioarekin frijitu
 * 3. Arrautzak irabatu eta nahastu
 * ```
 *
 * Etiketak hainbat hizkuntzatan onartzen dira (eu/es/en) eta osagaiak/pausoak
 * lerroz lerro zein komaz bereizita etor daitezke.
 */
object ErrezetaParser {

    private val izenaLabels = listOf("izena", "nombre", "name", "titulo", "título")
    private val osagaiLabels = listOf("osagaiak", "ingredientes", "ingredients", "osagai")
    private val pausoLabels = listOf("pausoak", "pasos", "steps", "elaboracion", "elaboración", "preparacion", "preparación", "pauso")

    fun parse(testua: String): Errezeta {
        val izena = mutableListOf<String>()
        val osagaiak = mutableListOf<String>()
        val pausoak = mutableListOf<String>()
        var atala = Atala.IZENA

        testua.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { lerroa ->
                val (label, balioa) = banatuEtiketa(lerroa)
                when {
                    label != null && izenaLabels.any { label.startsWith(it) } -> {
                        atala = Atala.IZENA
                        if (balioa.isNotBlank()) izena.add(balioa)
                    }
                    label != null && osagaiLabels.any { label.startsWith(it) } -> {
                        atala = Atala.OSAGAIAK
                        if (balioa.isNotBlank()) osagaiak.addAll(banatuBalioak(balioa))
                    }
                    label != null && pausoLabels.any { label.startsWith(it) } -> {
                        atala = Atala.PAUSOAK
                        if (balioa.isNotBlank()) pausoak.addAll(banatuBalioak(balioa))
                    }
                    else -> when (atala) {
                        Atala.IZENA -> if (izena.isEmpty()) izena.add(garbitu(lerroa))
                        Atala.OSAGAIAK -> osagaiak.add(garbitu(lerroa))
                        Atala.PAUSOAK -> pausoak.add(garbitu(lerroa))
                    }
                }
            }

        return Errezeta(
            izena = izena.joinToString(" ").ifBlank { "Errezeta inportatua" },
            osagaiak = osagaiak.filter { it.isNotBlank() },
            pausoak = pausoak.filter { it.isNotBlank() }
        )
    }

    /** "Izena: Tortilla" -> ("izena", "Tortilla"). Etiketarik ez badago -> (null, lerroa). */
    private fun banatuEtiketa(lerroa: String): Pair<String?, String> {
        val pos = lerroa.indexOf(':')
        return if (pos > 0) {
            val label = lerroa.substring(0, pos).trim().lowercase()
            val balioa = lerroa.substring(pos + 1).trim()
            // Etiketa ezaguna bada soilik; bestela testu arrunt gisa utzi
            val ezaguna = (izenaLabels + osagaiLabels + pausoLabels)
                .any { label == it || label.startsWith("$it ") || label == "${it}s" }
            if (ezaguna) label to balioa else null to lerroa
        } else null to lerroa
    }

    /** Komaz edo ';' bidez bereizitako balioak banatu. */
    private fun banatuBalioak(balioa: String): List<String> =
        balioa.split(Regex("[,;]"))
            .map { garbitu(it) }
            .filter { it.isNotBlank() }

    /** Lerro hasierako "-", "*", "1.", "1)" etab. kendu. */
    private fun garbitu(testua: String): String =
        testua.trim().replace(Regex("^([-*•]|\\d+[.)])\\s*"), "").trim()

    private enum class Atala { IZENA, OSAGAIAK, PAUSOAK }
}
