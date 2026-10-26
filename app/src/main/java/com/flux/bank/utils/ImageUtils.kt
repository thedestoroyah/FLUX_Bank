package com.flux.bank.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Resizes a Bitmap preserving aspect ratio so neither width nor height exceeds maxDim.
     */
    fun resizeBitmap(bitmap: Bitmap, maxDim: Int = 480): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (ratio > 1) {
            targetWidth = maxDim
            targetHeight = (maxDim / ratio).toInt()
        } else {
            targetHeight = maxDim
            targetWidth = (maxDim * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    /**
     * Compresses and converts a Bitmap into a compact Base64 String for Firestore persistence.
     * Keeps typical image document footprint between 30 KB and 60 KB.
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 75, maxDim: Int = 480): String {
        val resized = resizeBitmap(bitmap, maxDim)
        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Decodes a Base64 string back into a displayable Android Bitmap.
     */
    fun base64ToBitmap(base64Str: String?): Bitmap? {
        if (base64Str.isNullOrEmpty()) return null
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Đọc ảnh từ [uri] với downsampling để cạnh dài không quá [maxDim], và xoay đúng chiều theo EXIF
     * (ảnh từ app camera thường lưu nằm ngang kèm cờ xoay). Trả về null nếu không đọc được.
     */
    fun decodeSampled(context: Context, uri: Uri, maxDim: Int): Bitmap? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val rotated = if (rotation == 0f) decoded else Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true
        )
        resizeBitmap(rotated, maxDim)
    }.getOrNull()

    /**
     * Reads a Bitmap from an Android content Uri.
     */
    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }
}

