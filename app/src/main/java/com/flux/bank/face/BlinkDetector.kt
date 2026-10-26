package com.flux.bank.face

/**
 * Phát hiện một lần chớp mắt (mở → nhắm → mở) từ xác suất mở mắt mà ML Kit trả về mỗi khung hình.
 * Dùng làm bước kiểm tra "người thật" trước khi đối chiếu khuôn mặt.
 *
 * @param open ngưỡng coi là mắt mở (cả hai mắt phải vượt ngưỡng).
 * @param closed ngưỡng coi là mắt nhắm (cả hai mắt phải dưới ngưỡng).
 */
class BlinkDetector(private val open: Float = 0.8f, private val closed: Float = 0.2f) {

    private enum class Stage { WAIT_OPEN, OPEN, CLOSED }

    private var stage = Stage.WAIT_OPEN

    /** Trả về true đúng ở khung hình hoàn tất chuỗi mở → nhắm → mở. Giá trị null (ML Kit không đo được) bị bỏ qua. */
    fun onFrame(leftOpen: Float?, rightOpen: Float?): Boolean {
        if (leftOpen == null || rightOpen == null) return false
        val bothOpen = leftOpen > open && rightOpen > open
        val bothClosed = leftOpen < closed && rightOpen < closed
        when (stage) {
            Stage.WAIT_OPEN -> if (bothOpen) stage = Stage.OPEN
            Stage.OPEN -> if (bothClosed) stage = Stage.CLOSED
            Stage.CLOSED -> if (bothOpen) {
                stage = Stage.WAIT_OPEN
                return true
            }
        }
        return false
    }

    fun reset() {
        stage = Stage.WAIT_OPEN
    }
}
