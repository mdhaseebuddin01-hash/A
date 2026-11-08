package com.auris.app

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import android.graphics.Bitmap
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp

data class Prediction(val id: String, val prob: Float)

/** Facts about the installed model, written by the training notebook into model_info.json. */
data class ModelInfo(
    val name: String,
    val trainedOn: String,
    val testImages: Int,
    val testAccuracy: Double?,
    val testCiLow: Double?,
    val testCiHigh: Double?,
    val threshold: Float,
    val confidentAccuracy: Double?,
    val confidentCiLow: Double?,
    val confidentCoverage: Double?,
    val targetMet: Boolean
)

class Classifier private constructor(
    private val env: OrtEnvironment,
    private val session: OrtSession,
    private val inputName: String,
    private val size: Int,
    val labels: List<String>,
    val info: ModelInfo
) {

    /** Returns every label with its probability, highest first. Runs entirely on the phone. */
    fun classify(bitmap: Bitmap): List<Prediction> {
        val scaled = ImageLoader.resizeSquare(bitmap, size)
        val plane = size * size
        val pixels = IntArray(plane)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        if (scaled !== bitmap) scaled.recycle()

        // NCHW, RGB in 0..1. Normalisation happens inside the exported model.
        val buffer = ByteBuffer.allocateDirect(4 * 3 * plane)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        for (i in 0 until plane) {
            val c = pixels[i]
            buffer.put(i, ((c shr 16) and 0xFF) / 255f)
            buffer.put(plane + i, ((c shr 8) and 0xFF) / 255f)
            buffer.put(2 * plane + i, (c and 0xFF) / 255f)
        }

        val shape = longArrayOf(1, 3, size.toLong(), size.toLong())
        val raw: FloatArray = OnnxTensor.createTensor(env, buffer, shape).use { input ->
            session.run(mapOf(inputName to input)).use { result ->
                @Suppress("UNCHECKED_CAST")
                (result[0].value as Array<FloatArray>)[0].copyOf()
            }
        }
        val probs = if (looksLikeProbabilities(raw)) raw else softmax(raw)
        return labels.mapIndexed { i, id -> Prediction(id, probs.getOrElse(i) { 0f }) }
            .sortedByDescending { it.prob }
    }

    private fun looksLikeProbabilities(v: FloatArray): Boolean =
        v.all { it in 0f..1f } && abs(v.sum() - 1f) < 0.01f

    private fun softmax(v: FloatArray): FloatArray {
        val max = v.maxOrNull() ?: 0f
        val e = v.map { exp((it - max).toDouble()) }
        val total = e.sum()
        return FloatArray(v.size) { (e[it] / total).toFloat() }
    }

    companion object {
        const val MODEL_FILE = "auris.onnx"
        const val LABELS_FILE = "labels.txt"
        const val INFO_FILE = "model_info.json"

        fun load(context: Context): Classifier {
            val assets = context.assets
            val modelBytes = assets.open(MODEL_FILE).use { it.readBytes() }
            val labels = assets.open(LABELS_FILE).bufferedReader().use { reader ->
                reader.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            }
            require(labels.isNotEmpty()) { "labels.txt is empty" }
            val infoJson = runCatching {
                assets.open(INFO_FILE).bufferedReader().use { it.readText() }
            }.getOrNull()

            val env = OrtEnvironment.getEnvironment()
            val options = OrtSession.SessionOptions().apply { setIntraOpNumThreads(4) }
            val session = env.createSession(modelBytes, options)
            val inputName = session.inputNames.first()
            val shape = (session.inputInfo[inputName]?.info as? TensorInfo)?.shape
            val (info, sizeFromInfo) = parseInfo(infoJson)
            val size = shape?.getOrNull(2)?.toInt()?.takeIf { it > 0 } ?: sizeFromInfo
            return Classifier(env, session, inputName, size, labels, info)
        }

        private fun parseInfo(json: String?): Pair<ModelInfo, Int> {
            val o = runCatching { JSONObject(json ?: "{}") }.getOrDefault(JSONObject())
            fun num(key: String): Double? = o.optDouble(key).takeUnless { it.isNaN() }
            val info = ModelInfo(
                name = o.optString("name", "Auris model"),
                trainedOn = o.optString("trained_on", ""),
                testImages = o.optInt("test_images", 0),
                testAccuracy = num("test_accuracy"),
                testCiLow = num("test_ci_low"),
                testCiHigh = num("test_ci_high"),
                threshold = (num("threshold") ?: 0.9).toFloat(),
                confidentAccuracy = num("confident_accuracy"),
                confidentCiLow = num("confident_ci_low"),
                confidentCoverage = num("confident_coverage"),
                targetMet = o.optBoolean("target_met", false)
            )
            return info to o.optInt("input_size", 224)
        }
    }
}
