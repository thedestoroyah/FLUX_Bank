package com.flux.bank.face

import kotlin.math.sqrt

/** So khớp hai embedding khuôn mặt bằng cosine similarity. */
object FaceMatcher {

    /**
     * Ngưỡng cosine để coi là cùng một người.
     *
     * Suy từ repo gốc của model (syaringan357, MIT): họ chấp nhận khi bình phương khoảng cách L2 giữa hai
     * embedding đã chuẩn hoá ≤ 0.8; với vector đơn vị cos = 1 - d²/2 nên tương đương cos ≥ 0.6.
     * Đo ngày 2026-10-27 trên Xiaomi 2511FPC34G (so với selfie eKYC): cùng người 0.716–0.733 (n=2 còn trong log,
     * mọi lần thử khác đều qua); người khác -0.191–0.007 (n=6). Hai nhóm cách xa nhau nên giữ 0.6.
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
