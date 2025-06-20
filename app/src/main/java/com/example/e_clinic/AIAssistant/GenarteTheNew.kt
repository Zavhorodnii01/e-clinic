package com.example.e_clinic.AIAssistant

import android.util.Log
import com.example.e_clinic.BuildConfig

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlin.random.Random
import java.util.UUID

private const val TAG = "HealthTipGenerator"
private var lastTips = mutableSetOf<String>()

suspend fun getDailyHealthTip(): String {
    // Create unique prompt each time
    val promptId = UUID.randomUUID().toString()
    val currentTime = System.currentTimeMillis()
    val randomSeed = Random.nextInt(1, 1000)

    val prompt = """
        You are an AI medical assistant providing UNIQUE daily health tips.
        Generate exactly ONE health tip (12-15 words) with these requirements:
        
        MUST:
        - Be fundamentally different from: ${lastTips.joinToString(", ").take(200)}
        - Focus on: ${getRotatingCategory()}
        - Avoid sleep-related advice
        - Format: Plain text only, no quotes/bullets
        
        CONTEXT:
        - Prompt ID: $promptId
        - Timestamp: $currentTime
        - Random seed: $randomSeed
        
        EXAMPLES (DO NOT REPEAT THESE):
        - "Take stairs instead of elevator for movement"
        - "Add colorful vegetables to every meal"
        - "Wash hands for 20 seconds before eating"
        
        Your unique tip:
    """.trimIndent()

    return try {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                //temperature = 0.9  // More creative outputs
                topK = 30          // Wider variety selection
            }
        )

        val response = model.generateContent(prompt)
        val rawTip = response.text?.trim() ?: throw Exception("Empty API response")

        val cleanTip = rawTip
            .replace(Regex("^[\"']|[\"']$"), "")
            .replace(Regex("^[-•]\\s*"), "")
            .take(100)  // Safety limit

        if (cleanTip.isBlank()) {
            throw Exception("Blank tip generated")
        }

        // Ensure tip is truly new
        if (lastTips.size > 10) lastTips.clear()
        lastTips.add(cleanTip)

        Log.d(TAG, "New tip generated: $cleanTip")
        cleanTip

    } catch (e: Exception) {
        Log.e(TAG, "Tip generation failed: ${e.message}")
        getFallbackTip()
    }
}

private fun getRotatingCategory(): String {
    val categories = listOf(
        "physical activity",
        "nutrition",
        "mental health",
        "hygiene",
        "preventive care",
        "chronic condition management"
    )
    return categories.random()
}

private fun getFallbackTip(): String {
    val fallbackTips = listOf(
        "Take a 5-minute walk every hour",
        "Choose whole grains over refined carbs",
        "Practice deep breathing for stress relief",
        "Clean your phone screen daily",
        "Get regular health check-ups",
        "Manage diabetes with balanced meals",
        "Stretch your neck and shoulders",
        "Limit processed food intake",
        "Wear sunscreen when outdoors",
        "Stay socially connected for mental health"
    )
    return fallbackTips.random()
}