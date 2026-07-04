package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Request Models ---

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

// --- Gemini API Response Models ---

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generatePrediction(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }
}

data class AIPrediction(
    val occupancyPercent: Int,
    val demandLevel: String, // Low, Moderate, High
    val advice: String,
    val pricingSurgeText: String
)

object AIParkingPredictor {
    suspend fun predictParkingDemand(
        lotName: String,
        location: String,
        dayOfWeek: String,
        timeOfDay: String,
        basePrice: Double
    ): AIPrediction = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Secure, highly-realistic local fallback if Gemini key is not yet configured in UI secrets
            return@withContext getLocalSimulation(lotName, dayOfWeek, timeOfDay, basePrice)
        }

        val prompt = """
            Generate a parking demand prediction for the following parking spot:
            Lot Name: $lotName
            Location: $location
            Day: $dayOfWeek
            Time: $timeOfDay
            Base Price: ₹$basePrice/hr
            
            Return exactly a single JSON object (and nothing else, no markdown fences, no formatting) with these exact keys:
            - occupancyPercent (integer between 10 and 100)
            - demandLevel (string: either "Low", "Moderate", or "High")
            - advice (string: a direct, helpful, friendly sentence advising the user whether to book now, wait, or seek alternatives based on peak hours)
            - pricingSurgeText (string: e.g. "Normal rate ₹30/hr applies" or "Surge active: +₹15/hr due to high demand")
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are a specialized parking demand prediction AI. You output ONLY valid, raw, unformatted JSON. Do not include markdown code block syntax (like ```json).")))
        )

        try {
            val response = GeminiClient.api.generatePrediction(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: throw Exception("Empty response from model")
            
            // Clean markdown blocks if any returned despite instruction
            val cleanedJson = rawText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            
            // Parse using Moshi or manual search for safety
            val occupancy = extractIntJsonField(cleanedJson, "occupancyPercent") ?: 75
            val demand = extractStringJsonField(cleanedJson, "demandLevel") ?: "High"
            val advice = extractStringJsonField(cleanedJson, "advice") ?: "High demand expected during evening hours. Book now to secure your slot."
            val surge = extractStringJsonField(cleanedJson, "pricingSurgeText") ?: "Normal pricing applies."
            
            AIPrediction(occupancy, demand, advice, surge)
        } catch (e: Exception) {
            e.printStackTrace()
            getLocalSimulation(lotName, dayOfWeek, timeOfDay, basePrice)
        }
    }

    private fun extractIntJsonField(json: String, field: String): Int? {
        val regex = "\"$field\"\\s*:\\s*(\\d+)".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractStringJsonField(json: String, field: String): String? {
        val regex = "\"$field\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun getLocalSimulation(
        lotName: String,
        dayOfWeek: String,
        timeOfDay: String,
        basePrice: Double
    ): AIPrediction {
        val isWeekend = dayOfWeek.contains("Sat", true) || dayOfWeek.contains("Sun", true)
        val isPeakHour = timeOfDay.contains("PM", true) && (timeOfDay.contains("6") || timeOfDay.contains("7") || timeOfDay.contains("8") || timeOfDay.contains("5") || timeOfDay.contains("9"))

        val occupancy: Int
        val demandLevel: String
        val advice: String
        val surge: String

        if (isWeekend && isPeakHour) {
            occupancy = 92
            demandLevel = "High"
            surge = "Surge Pricing active: +₹15/hr"
            advice = "High weekend rush at $lotName. Only a few spots left. We highly recommend booking now!"
        } else if (isPeakHour) {
            occupancy = 82
            demandLevel = "High"
            surge = "Peak hour rate applies: +₹10/hr"
            advice = "Evening commuter rush at $lotName. Booking in advance is recommended to bypass entry queues."
        } else if (isWeekend) {
            occupancy = 65
            demandLevel = "Moderate"
            surge = "Weekend standard rate: +₹5/hr"
            advice = "Moderate parking demand. Booking is secure and quick."
        } else {
            occupancy = 38
            demandLevel = "Low"
            surge = "Normal rate of ₹${basePrice.toInt()}/hr applies"
            advice = "Low demand currently. Plenty of slots are available at $lotName."
        }

        return AIPrediction(occupancy, demandLevel, advice, surge)
    }
}
