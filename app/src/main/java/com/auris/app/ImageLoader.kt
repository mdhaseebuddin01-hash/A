package com.auris.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

object ImageLoader {

    /** Decodes a picked or captured photo, at most [maxDim] px on its longer side, turned upright. */
    fun load(context: Context, uri: Uri, maxDim: Int = 1600): Bitmap? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val probe = resolver.openInputStream(uri) ?: return null
        probe.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim || bounds.outHeight / (sample * 2) >= maxDim) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val stream = resolver.openInputStream(uri) ?: return null
        val decoded = stream.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

        val degrees = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                when (ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrNull() ?: 0f

        if (degrees == 0f) return decoded
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }

    /**
     * Resizes to a [size] x [size] square. It halves the image first, so fine detail is
     * averaged the way the training notebook's resize does, rather than skipped.
     */
    fun resizeSquare(src: Bitmap, size: Int): Bitmap {
        var current = src
        while (current.width / 2 >= size && current.height / 2 >= size) {
            val next = Bitmap.createScaledBitmap(current, current.width / 2, current.height / 2, true)
            if (current !== src) current.recycle()
            current = next
        }
        val out = Bitmap.createScaledBitmap(current, size, size, true)
        if (current !== src && current !== out) current.recycle()
        return out
    }
}
