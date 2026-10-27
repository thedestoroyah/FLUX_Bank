package com.flux.bank.data.model

/** Ngân hàng nhận cho chuyển liên ngân hàng (giả lập): mã viết tắt, tên ngắn và tên đầy đủ. */
data class Bank(val code: String, val shortName: String, val fullName: String)

/** Danh sách ngân hàng cố định cho màn chọn ngân hàng; FLUX không nằm trong danh sách. */
object Banks {
    /** Mã ngân hàng ghi vào `counterpartyBank` khi chuyển trong FLUX. */
    const val FLUX = "FLUX"

    val ALL = listOf(
        Bank("VCB", "Vietcombank", "Ngoại thương Việt Nam"),
        Bank("TCB", "Techcombank", "Kỹ thương Việt Nam"),
        Bank("BIDV", "BIDV", "Đầu tư và Phát triển Việt Nam"),
        Bank("CTG", "VietinBank", "Công thương Việt Nam"),
        Bank("MB", "MB Bank", "Quân đội"),
        Bank("ACB", "ACB", "Á Châu"),
        Bank("VPB", "VPBank", "Việt Nam Thịnh Vượng"),
        Bank("TPB", "TPBank", "Tiên Phong"),
        Bank("STB", "Sacombank", "Sài Gòn Thương Tín"),
        Bank("VIB", "VIB", "Quốc tế Việt Nam")
    )

    fun byCode(code: String): Bank? = ALL.firstOrNull { it.code == code }
}
