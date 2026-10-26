package com.flux.bank.face

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import java.io.Closeable

/** Bọc ML Kit Face Detection (model đóng gói sẵn, chạy offline) cho ảnh tĩnh và luồng CameraX. */
class FaceDetectorHelper : Closeable {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    /** Tìm các khuôn mặt trong ảnh tĩnh [bitmap]. */
    suspend fun detect(bitmap: Bitmap): List<Face> = detector.process(InputImage.fromBitmap(bitmap, 0)).await()

    /**
     * Analyzer cho CameraX: mỗi khung hình gọi [onFrame] với khuôn mặt duy nhất (null nếu 0 hoặc >1 mặt)
     * và bitmap đã xoay đúng chiều để cắt mặt khi cần.
     */
    @OptIn(ExperimentalGetImage::class)
    fun analyzer(onFrame: (Face?, Bitmap) -> Unit): ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { proxy: ImageProxy ->
        val media = proxy.image
        if (media == null) {
            proxy.close()
            return@Analyzer
        }
        val rotation = proxy.imageInfo.rotationDegrees
        val bitmap = proxy.toBitmap().rotate(rotation)
        detector.process(InputImage.fromMediaImage(media, rotation))
            .addOnSuccessListener { faces -> onFrame(faces.singleOrNull(), bitmap) }
            .addOnCompleteListener { proxy.close() }
    }

    override fun close() = detector.close()

    private fun Bitmap.rotate(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}
