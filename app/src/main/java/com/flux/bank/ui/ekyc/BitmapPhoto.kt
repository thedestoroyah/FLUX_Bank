package com.flux.bank.ui.ekyc

import android.content.Context
import android.graphics.Bitmap
import com.flux.bank.face.FaceDetectorHelper
import com.flux.bank.face.FaceEmbedder
import com.flux.bank.face.cropFace
import com.flux.bank.utils.ImageUtils

/** Ảnh chụp thật (Bitmap) cho màn eKYC. */
class BitmapPhoto(val bitmap: Bitmap, private val context: Context) : CapturedPhoto {

    /** Nén JPEG, giảm dần chất lượng cho tới khi mỗi ảnh không quá [MAX_BASE64_CHARS] ký tự. */
    override fun toBase64(): String {
        var quality = JPEG_QUALITY
        var encoded = ImageUtils.bitmapToBase64(bitmap, quality = quality, maxDim = MAX_DIM)
        while (encoded.length > MAX_BASE64_CHARS && quality > MIN_QUALITY) {
            quality -= QUALITY_STEP
            encoded = ImageUtils.bitmapToBase64(bitmap, quality = quality, maxDim = MAX_DIM)
        }
        return encoded
    }

    /** Tìm khuôn mặt trong selfie; đúng một mặt thì tính embedding MobileFaceNet. */
    suspend fun analyzeFace(): FaceAnalysis {
        val faces = FaceDetectorHelper().use { it.detect(bitmap) }
        return when (faces.size) {
            0 -> FaceAnalysis.NoFace
            1 -> FaceEmbedder(context.applicationContext).use { FaceAnalysis.One(it.embed(cropFace(bitmap, faces[0].boundingBox))) }
            else -> FaceAnalysis.MultipleFaces
        }
    }

    companion object {
        const val MAX_DIM = 1024
        private const val JPEG_QUALITY = 80
        private const val MIN_QUALITY = 40
        private const val QUALITY_STEP = 10
        private const val MAX_BASE64_CHARS = 420_000
    }
}
