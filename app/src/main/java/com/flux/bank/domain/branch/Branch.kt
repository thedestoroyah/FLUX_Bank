package com.flux.bank.domain.branch

/** Toạ độ địa lý (độ thập phân, WGS84). */
data class GeoPoint(val lat: Double, val lng: Double)

/** Một chi nhánh FLUX (dữ liệu giả lập, xem [Branches]). */
data class Branch(
    val id: String,
    val name: String,
    val address: String,
    val location: GeoPoint,
    val phone: String
)
