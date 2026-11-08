package com.auris.app

import android.graphics.Bitmap

/** Fast, rough image checks. They only produce hints and never block an analysis. */
object ImageQuality {

    data class Report(val tooDark: Boolean, val glare: Boolean, val blurred: Boolean) {
        val hints: List<String>
            get() = buildList {
                if (tooDark) add("The image looks dark. More light usually gives a clearer reading.")
                if (glare) add("Strong glare may hide detail. Angle the speculum slightly and retake.")
                if (blurred) add("The image may be out of focus. Hold steady and retake if you can.")
            }
    }

    fun check(src: Bitmap): Report {
        val n = 192
        val small = ImageLoader.resizeSquare(src, n)
        val px = IntArray(n * n)
        small.getPixels(px, 0, n, 0, 0, n, n)
        if (small !== src) small.recycle()

        val gray = FloatArray(px.size)
        var lit = 0
        var litSum = 0.0
        var blown = 0
        for (i in px.indices) {
            val c = px[i]
            val g = 0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)
            gray[i] = g
            if (g > 25f) {
                lit++
                litSum += g
            }
            if (g > 245f) blown++
        }
        val litFraction = lit.toDouble() / px.size
        val litMean = if (lit > 0) litSum / lit else 0.0

        // Variance of the Laplacian over the lit area: low values suggest blur.
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until n - 1) {
            for (x in 1 until n - 1) {
                val i = y * n + x
                if (gray[i] <= 25f) continue
                val lap = (gray[i - 1] + gray[i + 1] + gray[i - n] + gray[i + n] - 4f * gray[i]).toDouble()
                sum += lap
                sumSq += lap * lap
                count++
            }
        }
        val variance = if (count > 0) sumSq / count - (sum / count) * (sum / count) else 0.0

        return Report(
            tooDark = litFraction < 0.15 || litMean < 60.0,
            glare = lit > 0 && blown.toDouble() / lit > 0.12,
            blurred = count > 2000 && variance < 6.0
        )
    }
}
