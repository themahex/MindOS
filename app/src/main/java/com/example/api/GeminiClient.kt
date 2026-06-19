package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Sends a generation request to Gemini with an optional system instruction.
     */
    suspend fun generate(
        prompt: String,
        systemInstruction: String? = null,
        responseJsonOnly: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is missing or default placeholder!")
            return@withContext "Error: Gemini API key is missing. Please configure it in the AI Studio Secrets panel."
        }

        val url = "$BASE_URL?key=$apiKey"

        try {
            val rootJson = JSONObject()

            // Contents array
            val contentsArray = JSONArray()
            val textPart = JSONObject().put("text", prompt)
            val partsArray = JSONArray().put(textPart)
            val contentObject = JSONObject().put("parts", partsArray)
            contentsArray.put(contentObject)
            rootJson.put("contents", contentsArray)

            // Generation config
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.4)
            if (responseJsonOnly) {
                generationConfig.put("responseMimeType", "application/json")
            }
            rootJson.put("generationConfig", generationConfig)

            // System Instruction
            if (systemInstruction != null) {
                val sysPart = JSONObject().put("text", systemInstruction)
                val sysPartsArray = JSONArray().put(sysPart)
                val systemContent = JSONObject().put("parts", sysPartsArray)
                rootJson.put("systemInstruction", systemContent)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call failed: Code ${response.code}, Response: $responseStr")
                    return@withContext "Error: ${response.message} (Code ${response.code})"
                }

                val jsonResponse = JSONObject(responseStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "")
                    }
                }
                "Error: No text response from Gemini."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during generation", e)
            "Error: ${e.localizedMessage ?: "Unknown exception occurred"}"
        }
    }

    /**
     * Parses raw user text input to extract and organize structured thoughts.
     */
    suspend fun parseThought(userInput: String): JSONObject {
        val systemPrompt = """
            You are MindOS, the neural software layer of a user's mind extension. 
            Analyze the following unstructured raw thought or transcribing voice clip.
            Extract the following structured fields and return a single JSON object.
            
            JSON fields required:
            - "category": Categorize this thought into exactly one of these: "Startup", "Personal", "Task", "Idea", "Reminders", "Meeting".
            - "processedText": A beautifully formatted, concise revision of the thought. Fix spelling, expand acronyms if necessary, and use a bulleted list or high-fidelity technical tone. Keep it highly readable and clean.
            - "isTask": Boolean. Is this an actionable task, reminder, action item, or milestone?
            - "dueDateStr": If there is a deadline or date mentioned (such as "July 15", "tomorrow", "next Monday"), extract it as a warm, clean string. If no date is mentioned, set to null.
            
            Return ONLY the raw JSON object, no markdown wrappers, no backticks.
        """.trimIndent()

        val jsonResponse = generate(userInput, systemPrompt, responseJsonOnly = true)
        return try {
            JSONObject(jsonResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse json: $jsonResponse", e)
            // Fallback object
            JSONObject()
                .put("category", "General")
                .put("processedText", userInput)
                .put("isTask", userInput.lowercase().contains("need to") || userInput.lowercase().contains("remember to"))
                .put("dueDateStr", null as String?)
        }
    }
}
