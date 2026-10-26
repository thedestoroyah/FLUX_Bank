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
    private val onEmbedding: (FloatArray) -> Unit,
    private val onError: () -> Unit
) {

    private val executor = Executors.newSingleThreadExecutor()

    // Callback ML Kit có thể tới sau stop(); bỏ qua thay vì ném RejectedExecutionException
    private val safeExecutor = java.util.concurrent.Executor { task ->
        if (!executor.isShutdown) runCatching { executor.execute(task) }
    }
    private val detector = FaceDetectorHelper()
    private val embedder by lazy { FaceEmbedder(context) }
    private val blink = BlinkDetector()
    private val main = ContextCompat.getMainExecutor(context)
    private var provider: ProcessCameraProvider? = null

    @Volatile private var wantEmbedding = false
    @Volatile private var stopped = false

    fun start() {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (stopped) return@addListener
            runCatching { bind(future.get()) }.onFailure { onError() }
        }, main)
    }

    private fun bind(cameraProvider: ProcessCameraProvider) {
        provider = cameraProvider
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        // Callback ML Kit chạy trên executor riêng (một luồng) nên embed() và close() không chạy chồng nhau
        analysis.setAnalyzer(executor, detector.analyzer(safeExecutor) { face, bitmap ->
            if (face == null || stopped) return@analyzer
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
    }

    /** Sau một lần không khớp: bắt đầu lại từ bước chớp mắt. */
    fun retry() {
        blink.reset()
        wantEmbedding = false
    }

    fun stop() {
        stopped = true
        provider?.unbindAll()
        executor.execute {
            detector.close()
            runCatching { embedder.close() }
        }
        executor.shutdown()
    }
}
