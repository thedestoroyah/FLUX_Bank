package com.flux.bank.domain.branch

/**
 * Danh sách chi nhánh FLUX giả lập (tên tự đặt, đường thật ở TP.HCM), không lưu Firestore.
 *
 * Cụm demo quanh [DEMO_ANCHOR] (cổng TDTU trên Nguyễn Hữu Thọ), đo bằng server OSRM đi bộ ngày 2026-11-12:
 * | Chi nhánh            | Đường thẳng | Đi bộ   |
 * |----------------------|-------------|---------|
 * | FLUX Tân Phong       | 393 m       | 1.289 m | (bên kia rạch, phải đi vòng)
 * | FLUX Nguyễn Hữu Thọ  | 600 m       | 612 m   | (được gợi ý)
 * | FLUX Phú Mỹ Hưng     | 713 m       | 936 m   |
 * | FLUX Lê Văn Lương    | 892 m       | 1.174 m |
 * Đổi chỗ quay demo thì dời cụm này và đo lại (xem spec F6 §5).
 */
object Branches {

    /** Cổng Đại học Tôn Đức Thắng, 19 Nguyễn Hữu Thọ, Quận 7. */
    val DEMO_ANCHOR = GeoPoint(10.73256, 106.69982)

    val ALL: List<Branch> = listOf(
        Branch("q7-tan-phong", "FLUX Tân Phong", "45 Đường số 7, Tân Phong, Quận 7", GeoPoint(10.73256, 106.70342), "028 3775 1201"),
        Branch("q7-nguyen-huu-tho", "FLUX Nguyễn Hữu Thọ", "125 Nguyễn Hữu Thọ, Tân Hưng, Quận 7", GeoPoint(10.72716, 106.69982), "028 3775 1202"),
        Branch("q7-phu-my-hung", "FLUX Phú Mỹ Hưng", "801 Nguyễn Văn Linh, Tân Phong, Quận 7", GeoPoint(10.72896, 106.70522), "028 3775 1203"),
        Branch("q7-le-van-luong", "FLUX Lê Văn Lương", "210 Lê Văn Lương, Tân Hưng, Quận 7", GeoPoint(10.73976, 106.70342), "028 3775 1204"),
        Branch("q1-ben-thanh", "FLUX Bến Thành", "15 Lê Lợi, Bến Nghé, Quận 1", GeoPoint(10.77250, 106.69800), "028 3822 1101"),
        Branch("q3-vo-van-tan", "FLUX Võ Văn Tần", "88 Võ Văn Tần, Phường 6, Quận 3", GeoPoint(10.77860, 106.68700), "028 3930 1301"),
        Branch("q4-khanh-hoi", "FLUX Khánh Hội", "88 Khánh Hội, Phường 3, Quận 4", GeoPoint(10.75900, 106.70400), "028 3940 1401"),
        Branch("q5-tran-hung-dao", "FLUX Chợ Lớn", "560 Trần Hưng Đạo, Phường 2, Quận 5", GeoPoint(10.75500, 106.66700), "028 3855 1501"),
        Branch("bt-xo-viet-nghe-tinh", "FLUX Bình Thạnh", "302 Xô Viết Nghệ Tĩnh, Phường 21, Bình Thạnh", GeoPoint(10.80100, 106.71100), "028 3512 1601"),
        Branch("pn-phan-dang-luu", "FLUX Phú Nhuận", "120 Phan Đăng Lưu, Phường 3, Phú Nhuận", GeoPoint(10.79900, 106.68000), "028 3995 1701"),
        Branch("tb-cong-hoa", "FLUX Tân Bình", "364 Cộng Hoà, Phường 13, Tân Bình", GeoPoint(10.80100, 106.65200), "028 3811 1801"),
        Branch("td-vo-van-ngan", "FLUX Thủ Đức", "66 Võ Văn Ngân, Bình Thọ, Thủ Đức", GeoPoint(10.85000, 106.77200), "028 3896 1901")
    )
}
