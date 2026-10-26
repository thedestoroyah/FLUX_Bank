package com.flux.bank.ui.ekyc

import android.content.Context
import android.graphics.Bitmap
import com.flux.bank.face.FaceDetectorHelper
import com.flux.bank.face.FaceEmbedder
import com.flux.bank.face.cropFace
import com.flux.bank.utils.ImageUtils

/** Ảnh chụp thật (Bitmap) cho màn eKYC. */
class BitmapPhoto(val bitmap: Bitmap, private val context: Context) : CapturedPhoto {

    override fun toBase64(): String = ImageUtils.bitmapToBase64(bitmap, quality = JPEG_QUALITY, maxDim = MAX_DIM)

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
    }
}
