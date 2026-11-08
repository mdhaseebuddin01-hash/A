package com.auris.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** One doctor-confirmed case. No images or patient details are kept. */
data class CaseRecord(
    val id: String,
    val ts: Long,
    val ai: String,
    val prob: Float,
    val confident: Boolean,
    val actual: String,
    val ok: Boolean,
    val top3: Boolean
)

/** Stores the verdict log as a small JSON file in the app's private storage. */
class CaseLog(context: Context) {
    private val file = File(context.filesDir, "auris_cases.json")

    fun load(): List<CaseRecord> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                CaseRecord(
                    id = o.optString("id"),
                    ts = o.optLong("ts"),
                    ai = o.optString("ai"),
                    prob = o.optDouble("prob", 0.0).toFloat(),
                    confident = o.optBoolean("confident"),
                    actual = o.optString("actual"),
                    ok = o.optBoolean("ok"),
                    top3 = o.optBoolean("top3")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(cases: List<CaseRecord>) {
        val arr = JSONArray()
        cases.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("ts", c.ts)
                put("ai", c.ai)
                put("prob", c.prob.toDouble())
                put("confident", c.confident)
                put("actual", c.actual)
                put("ok", c.ok)
                put("top3", c.top3)
            })
        }
        val text = arr.toString()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }
}

object Stats {

    data class Summary(val n: Int, val k: Int, val top3: Int, val ciLow: Double, val ciHigh: Double) {
        val accuracy: Double get() = if (n == 0) 0.0 else k.toDouble() / n
        val top3Rate: Double get() = if (n == 0) 0.0 else top3.toDouble() / n
    }

    fun summarise(cases: List<CaseRecord>): Summary {
        val n = cases.size
        val k = cases.count { it.ok }
        val (low, high) = wilson(k, n)
        return Summary(n, k, cases.count { it.top3 }, low, high)
    }

    /** 95% Wilson score interval for k successes out of n. */
    fun wilson(k: Int, n: Int, z: Double = 1.96): Pair<Double, Double> {
        if (n == 0) return 0.0 to 0.0
        val p = k.toDouble() / n
        val d = 1 + z * z / n
        val centre = (p + z * z / (2.0 * n)) / d
        val half = z * sqrt(p * (1 - p) / n + z * z / (4.0 * n * n)) / d
        return max(0.0, centre - half) to min(1.0, centre + half)
    }

    fun csv(cases: List<CaseRecord>): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder("date,auris_diagnosis,auris_probability,confident,doctor_diagnosis,match,in_top_3\n")
        cases.sortedBy { it.ts }.forEach { c ->
            sb.append(fmt.format(Date(c.ts))).append(',')
                .append(quote(Catalogue.name(c.ai))).append(',')
                .append(String.format(Locale.US, "%.3f", c.prob)).append(',')
                .append(if (c.confident) "yes" else "no").append(',')
                .append(quote(Catalogue.name(c.actual))).append(',')
                .append(if (c.ok) "yes" else "no").append(',')
                .append(if (c.top3) "yes" else "no").append('\n')
        }
        return sb.toString()
    }

    private fun quote(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
}
