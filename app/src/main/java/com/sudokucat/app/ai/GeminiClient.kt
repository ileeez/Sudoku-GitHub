package com.sudokucat.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class GeminiClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val lastRequestAt = AtomicLong(0)
    private val requestLock = Any()
    private var sessionRequests = 0
    private var sessionWindowStart = 0L

    suspend fun generate(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double = 0.75,
        maxOutputTokens: Int = 700,
        timeoutMs: Long = 15_000L,
        retryOnServerErrors: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("system_instruction", buildJsonObject {
                put("parts", JsonArray(listOf(buildJsonObject { put("text", systemPrompt) })))
            })
            put("contents", JsonArray(listOf(buildJsonObject {
                put("role", "user")
                put("parts", JsonArray(listOf(buildJsonObject { put("text", userPrompt) })))
            })))
            put("generationConfig", buildJsonObject {
                put("temperature", temperature)
                put("topP", 0.9)
                put("maxOutputTokens", maxOutputTokens)
            })
        }
        send(apiKey, model, payload, timeoutMs, retryOnServerErrors)
    }

    /**
     * Chiamata multimodale con Structured Outputs.
     *
     * L'immagine viene allegata come `inline_data` (base64) e la risposta è
     * vincolata da un JSON Schema tramite `responseSchema`, così il parsing
     * non dipende da testo libero o code fence.
     */
    suspend fun generateJson(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        schema: JsonObject,
        imageBytes: ByteArray? = null,
        imageMimeType: String = "image/jpeg",
        temperature: Double = 0.1,
        maxOutputTokens: Int = 1400,
        timeoutMs: Long = 10_000L,
        retryOnServerErrors: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        if (imageBytes != null && imageBytes.isEmpty()) {
            return@withContext Result.failure(GeminiException("L'immagine da analizzare è vuota."))
        }
        val parts = mutableListOf<JsonObject>()
        if (imageBytes != null) {
            val encoded = Base64.getEncoder().encodeToString(imageBytes)
            parts.add(buildJsonObject {
                put("inline_data", buildJsonObject {
                    put("mime_type", imageMimeType)
                    put("data", encoded)
                })
            })
        }
        parts.add(buildJsonObject { put("text", userPrompt) })
        val payload = buildJsonObject {
            put("system_instruction", buildJsonObject {
                put("parts", JsonArray(listOf(buildJsonObject { put("text", systemPrompt) })))
            })
            put("contents", JsonArray(listOf(buildJsonObject {
                put("role", "user")
                put("parts", JsonArray(parts))
            })))
            put("generationConfig", buildJsonObject {
                put("temperature", temperature)
                put("topP", 0.9)
                put("maxOutputTokens", maxOutputTokens)
                put("responseMimeType", "application/json")
                put("responseSchema", schema)
            })
        }
        send(apiKey, model, payload, timeoutMs, retryOnServerErrors)
    }

    suspend fun test(apiKey: String, model: String): Result<String> = generate(
        apiKey = apiKey,
        model = model,
        systemPrompt = "Sei un assistente conciso. Rispondi in italiano.",
        userPrompt = "Rispondi soltanto con: Connessione riuscita.",
        temperature = 0.1,
        maxOutputTokens = 80
    )

    private suspend fun send(
        apiKey: String,
        model: String,
        payload: JsonObject,
        timeoutMs: Long,
        retryOnServerErrors: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(GeminiException("Nessuna chiave Gemini configurata."))
        }
        if (!throttleAllowed()) {
            return@withContext Result.failure(GeminiException("Troppe richieste ravvicinate. Attendi qualche secondo."))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.trim()}:generateContent"
        // Le chiamate che bloccano la UI (OCR e suggerimenti) non fanno retry:
        // meglio un fallback locale immediato che attendere un secondo tentativo.
        val attempts = if (retryOnServerErrors) 2 else 1
        var attempt = 0
        var lastError: Throwable? = null
        while (attempt < attempts) {
            attempt++
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", apiKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                val call = client.newCall(request)
                call.timeout().timeout(timeoutMs, TimeUnit.MILLISECONDS)
                call.execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val message = when (response.code) {
                            400 -> "Richiesta Gemini non valida. Controlla il modello selezionato."
                            401, 403 -> "Chiave Gemini non valida o non autorizzata."
                            404 -> "Modello Gemini non disponibile. Prova un altro nome modello."
                            413 -> "L'immagine è troppo grande per Gemini. Prova con una foto più leggera."
                            429 -> "Limite gratuito Gemini raggiunto. Riprova più tardi."
                            in 500..599 -> "Gemini è temporaneamente non disponibile."
                            else -> "Errore Gemini (${response.code})."
                        }
                        val error = GeminiException("$message ${extractApiMessage(body)}".trim())
                        if (retryOnServerErrors && attempt < attempts &&
                            (response.code == 429 || response.code >= 500)
                        ) {
                            lastError = error
                            delay(900L * attempt)
                            continue
                        }
                        return@withContext Result.failure(error)
                    }
                    val text = extractText(body)
                    if (text.isBlank()) {
                        return@withContext Result.failure(GeminiException("Gemini ha restituito una risposta vuota."))
                    }
                    return@withContext Result.success(text.trim())
                }
            } catch (e: IOException) {
                lastError = e
                if (retryOnServerErrors && attempt < attempts) delay(700L * attempt)
            } catch (e: Exception) {
                return@withContext Result.failure(GeminiException("Connessione a Gemini non riuscita.", e))
            }
        }
        Result.failure(
            GeminiException(
                "Nessuna connessione a Gemini. Puoi continuare a giocare: i suggerimenti locali sono sempre disponibili.",
                lastError
            )
        )
    }

    private fun throttleAllowed(): Boolean = synchronized(requestLock) {
        val now = System.currentTimeMillis()
        if (now - lastRequestAt.get() < 2500) return false
        if (now - sessionWindowStart > 60 * 60 * 1000L) {
            sessionWindowStart = now
            sessionRequests = 0
        }
        if (sessionRequests >= 30) return false
        lastRequestAt.set(now)
        sessionRequests++
        true
    }

    private fun extractText(body: String): String {
        return runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            val candidates = root["candidates"]?.jsonArray ?: return ""
            val builder = StringBuilder()
            for (candidate in candidates) {
                val parts = candidate.jsonObject["content"]?.jsonObject?.get("parts")?.jsonArray ?: continue
                for (part in parts) {
                    val obj = part.jsonObject
                    if (obj["thought"]?.jsonPrimitive?.contentOrNull == "true") continue
                    obj["text"]?.jsonPrimitive?.contentOrNull?.let { builder.append(it) }
                }
            }
            builder.toString()
        }.getOrDefault("")
    }

    private fun extractApiMessage(body: String): String = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull.orEmpty()
    }.getOrDefault("")

    private fun JsonObject?.stringOrNull(key: String): String? =
        this?.get(key)?.jsonPrimitive?.contentOrNull
}

class GeminiException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class AiSource(val label: String)

object AiSources {
    val LOCAL = AiSource("Spiegazione locale")
    val AI = AiSource("Gemini")
    val OCR = AiSource("Gemini OCR")
    val CACHE = AiSource("Risposta in cache")
}
