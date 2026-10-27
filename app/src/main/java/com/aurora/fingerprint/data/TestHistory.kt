package com.aurora.fingerprint.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TestRecord(val timestamp: Long, val outcome: String, val explanation: String)

/** Non-sensitive local summaries only. No biometric data, identifiers, or analytics SDK. */
class TestHistory(context: Context) {
    private val prefs = context.getSharedPreferences("local_test_history", Context.MODE_PRIVATE)

    fun read(): List<TestRecord> = try {
        val stored = JSONArray(prefs.getString("history", "[]") ?: "[]")
        (0 until stored.length()).mapNotNull { i ->
            val obj = stored.optJSONObject(i) ?: return@mapNotNull null
            TestRecord(obj.optLong("time"), obj.optString("outcome"), obj.optString("note"))
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun append(record: TestRecord): List<TestRecord> {
        val updated = (listOf(record) + read()).take(20)
        val array = JSONArray()
        updated.forEach {
            array.put(JSONObject().put("time", it.timestamp)
                .put("outcome", it.outcome).put("note", it.explanation))
        }
        prefs.edit().putString("history", array.toString()).apply()
        return updated
    }

    fun clear() = prefs.edit().remove("history").apply()
}
