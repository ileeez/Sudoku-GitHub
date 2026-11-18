package com.sudokucat.app.ai

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * JSON Schema e parser per le risposte Structured Output di Gemini.
 *
 * Tenere schema e parsing nello stesso punto evita che il formato richiesto
 * al modello e quello letto dal codice divergano.
 */
object StructuredOutputs {

    /** Schema OCR: { "grid": [[int x9] x9] } con 0 per le celle vuote. */
    val gridSchema: JsonObject = buildJsonObject {
        put("type", "OBJECT")
        put("properties", buildJsonObject {
            put("grid", buildJsonObject {
                put("type", "ARRAY")
                put("items", buildJsonObject {
                    put("type", "ARRAY")
                    put("items", buildJsonObject { put("type", "INTEGER") })
                })
            })
        })
        put("required", JsonArray(listOf(JsonPrimitive("grid"))))
    }

    /**
     * Schema suggerimento: { "row": int, "col": int, "valore": int,
     * "spiegazione": string } come richiesto dalla scheda didattica.
     */
    val moveSchema: JsonObject = buildJsonObject {
        put("type", "OBJECT")
        put("properties", buildJsonObject {
            put("row", buildJsonObject { put("type", "INTEGER") })
            put("col", buildJsonObject { put("type", "INTEGER") })
            put("valore", buildJsonObject { put("type", "INTEGER") })
            put("spiegazione", buildJsonObject { put("type", "STRING") })
        })
        put(
            "required",
            JsonArray(
                listOf(
                    JsonPrimitive("row"),
                    JsonPrimitive("col"),
                    JsonPrimitive("valore"),
                    JsonPrimitive("spiegazione")
                )
            )
        )
    }

    data class Move(
        val row: Int,
        val col: Int,
        val value: Int,
        val explanation: String
    ) {
        val cell: Int get() = row * 9 + col
    }

    /** Estrae una griglia 9x9 valida, oppure null se la risposta è malformata. */
    fun parseGrid(raw: String): IntArray? {
        val root = extractObject(raw) ?: return null
        val rows = runCatching { root["grid"]?.jsonArray }.getOrNull() ?: return null
        if (rows.size != 9) return null
        val grid = IntArray(81)
        for (row in 0 until 9) {
            val cells = runCatching { rows[row].jsonArray }.getOrNull() ?: return null
            if (cells.size != 9) return null
            for (col in 0 until 9) {
                val value = runCatching {
                    cells[col].jsonPrimitive.contentOrNull?.trim()?.toIntOrNull()
                }.getOrNull() ?: return null
                if (value !in 0..9) return null
                grid[row * 9 + col] = value
            }
        }
        return grid
    }

    /** Estrae e valida un suggerimento. */
    fun parseMove(raw: String): Move? {
        val root = extractObject(raw) ?: return null
        val row = intValue(root, "row") ?: return null
        val col = intValue(root, "col") ?: return null
        val value = intValue(root, "valore") ?: intValue(root, "value") ?: return null
        if (row !in 0..8 || col !in 0..8 || value !in 1..9) return null
        val explanation = runCatching {
            (root["spiegazione"] ?: root["explanation"])?.jsonPrimitive?.contentOrNull
        }.getOrNull().orEmpty().trim()
        if (explanation.isBlank()) return null
        return Move(row, col, value, explanation)
    }

    private fun intValue(root: JsonObject, key: String): Int? =
        runCatching { root[key]?.jsonPrimitive?.contentOrNull?.trim()?.toIntOrNull() }.getOrNull()

    /**
     * Gemini a volte avvolge il JSON in ```json ... ``` oppure aggiunge
     * una frase introduttiva. Prendiamo il primo oggetto completo.
     */
    private fun extractObject(raw: String): JsonObject? {
        val cleaned = raw
            .replace("```json", "")
            .replace("```JSON", "")
            .replace("```", "")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val candidate = cleaned.substring(start, end + 1)
        return runCatching { kotlinx.serialization.json.Json.parseToJsonElement(candidate).jsonObject }.getOrNull()
    }
}
