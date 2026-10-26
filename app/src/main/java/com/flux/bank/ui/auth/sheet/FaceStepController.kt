package com.flux.bank.ui.auth.sheet

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.flux.bank.face.BlinkDetector
import com.flux.bank.face.FaceDetectorHelper
import com.flux.bank.face.FaceEmbedder
import com.flux.bank.face.cropFace
import java.util.concurrent.Executors

/**
 * Chạy camera trước trong sheet: mỗi khung hình đưa qua ML Kit; khi phát hiện chớp mắt thì báo [onBlink],
 * khung hình kế tiếp có đúng một khuôn mặt sẽ được cắt + tính embedding và trả qua [onEmbedding].
 * Callback được gọi trên main thread.
 */
class FaceStepController(
    private val context: Context,
    private val owner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onBlink: () -> Unit,
    private val onEmbedding: (FloatArray) -> Unit
) {

    private val executor = Executors.newSingleThreadExecutor()
    private val detector = FaceDetectorHelper()
    private val embedder by lazy { FaceEmbedder(context) }
    private val blink = BlinkDetector()
    private val main = ContextCompat.getMainExecutor(context)
    private var provider: ProcessCameraProvider? = null

    @Volatile private var wantEmbedding = false

    fun start() {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val cameraProvider = future.get().also { provider = it }
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor, detector.analyzer { face, bitmap ->
                if (face == null) return@analyzer
                if (wantEmbedding) {
                    wantEmbedding = false
                    val embedding = embedder.embed(cropFace(bitmap, face.boundingBox))
                    main.execute { onEmbedding(embedding) }
                } else if (blink.onFrame(face.leftEyeOpenProbability, face.rightEyeOpenProbability)) {
                    wantEmbedding = true
                    main.execute(onBlink)
                }
            })
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
        }, main)
    }

    /** Sau một lần không khớp: bắt đầu lại từ bước chớp mắt. */
    fun retry() {
        blink.reset()
        wantEmbedding = false
    }

    fun stop() {
        provider?.unbindAll()
        executor.execute {
            detector.close()
            runCatching { embedder.close() }
        }
        executor.shutdown()
    }
}
