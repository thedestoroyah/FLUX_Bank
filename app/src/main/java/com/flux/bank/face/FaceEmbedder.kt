package com.flux.bank.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Chạy MobileFaceNet (TFLite, on-device) để lấy embedding 192 chiều đã chuẩn hoá L2 từ ảnh khuôn mặt đã cắt.
 * Model gốc có input batch cố định (thường = 2) nên ảnh được lặp lại để lấp đầy batch và lấy dòng đầu.
 */
class FaceEmbedder(context: Context) : Closeable {

    private val interpreter: Interpreter = Interpreter(loadModel(context), Interpreter.Options().setNumThreads(4))
    private val batch: Int = interpreter.getInputTensor(0).shape()[0]
    private val embeddingSize: Int = interpreter.getOutputTensor(0).shape()[1]

    /** Trả về embedding đã chuẩn hoá L2 của [face] (ảnh khuôn mặt đã cắt, kích thước bất kỳ). */
    fun embed(face: Bitmap): FloatArray {
        val scaled = Bitmap.createScaledBitmap(face, INPUT_SIZE, INPUT_SIZE, true)
        val input = ByteBuffer.allocateDirect(batch * INPUT_SIZE * INPUT_SIZE * 3 * 4).order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        repeat(batch) {
            for (p in pixels) {
                input.putFloat(((p shr 16 and 0xFF) - MEAN) / STD)
                input.putFloat(((p shr 8 and 0xFF) - MEAN) / STD)
                input.putFloat(((p and 0xFF) - MEAN) / STD)
            }
        }
        input.rewind()
        val output = Array(batch) { FloatArray(embeddingSize) }
        interpreter.run(input, output)
        return l2Normalize(output[0])
    }

    override fun close() = interpreter.close()

    private fun loadModel(context: Context): ByteBuffer {
        val bytes = context.assets.open(MODEL_FILE).use { it.readBytes() }
        return ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).apply { rewind() }
    }

    private fun l2Normalize(v: FloatArray): FloatArray {
        val norm = sqrt(v.fold(0f) { acc, x -> acc + x * x }).coerceAtLeast(1e-10f)
        return FloatArray(v.size) { v[it] / norm }
    }

    companion object {
        private const val MODEL_FILE = "mobilefacenet.tflite"
        private const val INPUT_SIZE = 112
        private const val MEAN = 127.5f
        private const val STD = 128f
    }
}

/** Cắt vùng khuôn mặt [box] ra khỏi [bitmap], nới thêm 10% mỗi phía và kẹp trong biên ảnh. */
fun cropFace(bitmap: Bitmap, box: Rect): Bitmap {
    val marginX = (box.width() * 0.1f).toInt()
    val marginY = (box.height() * 0.1f).toInt()
    val left = (box.left - marginX).coerceIn(0, bitmap.width - 1)
    val top = (box.top - marginY).coerceIn(0, bitmap.height - 1)
    val right = (box.right + marginX).coerceIn(left + 1, bitmap.width)
    val bottom = (box.bottom + marginY).coerceIn(top + 1, bitmap.height)
    return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
}
