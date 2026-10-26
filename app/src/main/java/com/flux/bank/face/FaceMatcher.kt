package com.flux.bank.face

import kotlin.math.sqrt

/** So khớp hai embedding khuôn mặt bằng cosine similarity. */
object FaceMatcher {

    /**
     * Ngưỡng cosine để coi là cùng một người.
     *
     * Suy từ repo gốc của model (syaringan357, MIT): họ chấp nhận khi bình phương khoảng cách L2 giữa hai
     * embedding đã chuẩn hoá ≤ 0.8; với vector đơn vị cos = 1 - d²/2 nên tương đương cos ≥ 0.6.
     * Cần hiệu chỉnh lại trên máy thật (Task 11 bước 4) và ghi số đo vào đây.
     */
    const val MATCH_THRESHOLD = 0.6f

    /** Cosine giữa [a] và [b]. Ném [IllegalArgumentException] nếu khác độ dài hoặc có vector 0. */
    fun cosine(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Embedding khác độ dài: ${a.size} vs ${b.size}" }
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        require(normA > 0f && normB > 0f) { "Embedding rỗng (vector 0)" }
        return dot / (sqrt(normA) * sqrt(normB))
    }

    fun isMatch(a: FloatArray, b: FloatArray): Boolean = cosine(a, b) >= MATCH_THRESHOLD
}
