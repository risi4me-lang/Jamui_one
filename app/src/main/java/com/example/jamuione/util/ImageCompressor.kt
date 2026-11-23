package com.example.jamuione.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {
    suspend fun compressImage(context: Context, uri: Uri, maxSide: Int = 1280): Uri = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext uri

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0) return@withContext uri

            var sampleSize = 1
            var maxDim = maxOf(width, height)
            while (maxDim > maxSide) {
                sampleSize *= 2
                maxDim /= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val inputStream2 = context.contentResolver.openInputStream(uri) ?: return@withContext uri
            var bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2.close()

            if (bitmap == null) return@withContext uri

            val currentMax = maxOf(bitmap.width, bitmap.height)
            if (currentMax > maxSide) {
                val scale = maxSide.toFloat() / currentMax
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                if (scaled !== bitmap) {
                    bitmap.recycle()
                    bitmap = scaled
                }
            }

            val tempFile = File.createTempFile("compressed_", ".jpg", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()
            bitmap.recycle()

            Uri.fromFile(tempFile)
        } catch (e: Exception) {
            Log.w("IMAGE_COMPRESS", "Failed to compress image, falling back to original: ${e.message}")
            uri
        }
    }
}
