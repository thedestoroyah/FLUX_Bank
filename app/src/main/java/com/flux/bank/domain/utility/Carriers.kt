package com.flux.bank.domain.utility

import com.flux.bank.core.validation.Validators

/** Nhà mạng di động hỗ trợ nạp tiền (giả lập). */
enum class Carrier(val displayName: String) {
    VIETTEL("Viettel"), VINAPHONE("Vinaphone"), MOBIFONE("Mobifone"), VIETNAMOBILE("Vietnamobile")
}

/** Nhận nhà mạng theo 3 số đầu (spec F5 §4) và các mệnh giá nạp cố định. */
object Carriers {
    val DENOMINATIONS: List<Long> = listOf(10_000, 20_000, 50_000, 100_000, 200_000, 500_000)

    private const val PREFIX_LENGTH = 3

    private val PREFIXES: Map<String, Carrier> = buildMap {
        (32..39).forEach { put("0$it", Carrier.VIETTEL) }
        listOf("086", "096", "097", "098").forEach { put(it, Carrier.VIETTEL) }
        (81..85).forEach { put("0$it", Carrier.VINAPHONE) }
        listOf("088", "091", "094").forEach { put(it, Carrier.VINAPHONE) }
        (76..79).forEach { put("0$it", Carrier.MOBIFONE) }
        listOf("070", "089", "090", "093").forEach { put(it, Carrier.MOBIFONE) }
        listOf("052", "056", "058", "092").forEach { put(it, Carrier.VIETNAMOBILE) }
    }

    /** Nhà mạng của [phone] (chấp nhận khoảng trắng); null nếu SĐT sai định dạng hoặc đầu số lạ. */
    fun detect(phone: String): Carrier? {
        if (!Validators.isValidVnPhone(phone)) return null
        return PREFIXES[Validators.normalizePhone(phone).take(PREFIX_LENGTH)]
    }
}
