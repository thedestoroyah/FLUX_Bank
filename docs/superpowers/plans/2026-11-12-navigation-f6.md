# Feature 6 — Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ô "Chi nhánh" trên trang chủ mở màn bản đồ Google Maps: tự lấy vị trí, hiện các chi nhánh FLUX, gợi ý chi nhánh có **quãng đi bộ ngắn nhất** (tính bằng OSRM) và vẽ đường đi; có trạng thái chờ và lỗi rõ ràng.

**Architecture:** Phần tính toán (khoảng cách, giờ mở cửa, xếp hạng, parse đường đi) là Kotlin thuần ở `domain/branch/` và `data/routing/`, có unit test. `BranchMapViewModel` nhận `LocationSource` và `BranchRecommender` qua constructor (mặc định lấy từ `FluxApp`), test bằng fake. `BranchMapFragment` chỉ vẽ state: `SupportMapFragment` + bottom sheet cố định. Không đụng Firestore, không đổi rules.

**Tech Stack:** Kotlin, XML + ViewBinding, `play-services-maps` 19.2.0, `play-services-location` 21.3.0, `HttpURLConnection` + `org.json` (test: `org.json:json:20240303`), coroutines.

> **Ghi chú sau khi thực hiện (2026-11-12):** Google Cloud từ chối tạo tài khoản thanh toán (`OR_BACR2_59`) nên bản đồ đã chuyển từ Google Maps SDK sang **OSMDroid / OpenStreetMap** (spec §4). Các bước về key Maps, `SupportMapFragment`, `MapStyleOptions` ở Task 4 và Task 6 dưới đây là lịch sử; code thật dùng `org.osmdroid.views.MapView`.

**Spec:** `docs/superpowers/specs/2026-11-12-navigation-f6-design.md` (đã duyệt 2026-11-12). Mockup: `docs/superpowers/specs/mockups/2026-11-12-f6-navigation.html` (đã duyệt). Design system: `DESIGN.md`.

## Global Constraints

- Làm trên nhánh `feature/f6-navigation` (đã tạo từ `main`, đã có commit spec + mockup).
- Kotlin + XML/ViewBinding, không Compose. MVVM + service locator `FluxApp`, không DI library.
- Chỉ giao diện sáng; chỉ token màu, `dimens.xml`, `TextAppearance.Flux.*`, `Widget.Flux.*`; chữ giao diện tiếng Việt trong `strings.xml`. Tên/địa chỉ chi nhánh là dữ liệu Kotlin (như danh mục F5).
- Ngoại lệ màu chỉ trong màn bản đồ (spec §7): chấm người dùng xanh mặc định của Google, ghim obsidian, ghim đang chọn viền emerald, đường đi emerald viền trắng, đường thẳng dự phòng obsidian nét đứt.
- Key Maps: `local.properties` → `maps.api.key` → `manifestPlaceholders["MAPS_API_KEY"]`; không commit `local.properties`; không dùng key trong `google-services.json`. Thiếu key thì vẫn build được.
- Server đi bộ: `https://routing.openstreetmap.de/routed-foot/route/v1/foot/{lng1},{lat1};{lng2},{lat2}?overview=full&geometries=geojson`, timeout 8 s, header `User-Agent: com.flux.bank`.
- Hằng số: 3 ứng viên; không gọi router khi đường thẳng > 20.000 m; chờ vị trí tối đa 15 s; giờ mở cửa T2–T6 8:00–17:00, T7 8:00–12:00, CN nghỉ, theo `TransferPolicy.VN_ZONE`.
- KDoc/comment tiếng Việt, tên code tiếng Anh. TDD: test fail trước. Commit `feat:`/`fix:`/`test:`/`docs:`/`chore:`.
- `git add` theo đường dẫn cụ thể (`app docs README.md HANDOFF.md PROJECT_CONTEXT.md`), không `git add -A`, không commit `.idea/*`.
- Lệnh kiểm tra (từ `FLUX_Bank/`): `./gradlew testDebugUnitTest`, `./gradlew assembleDebug lintDebug`.

## Review Focus

1. Chạm nhanh chi nhánh A rồi B khi đường tới A chưa về: chỉ đường tới B được vẽ, kết quả A về muộn bị bỏ → test `latestSelectionWins` ở Task 5.
2. Bấm "Vị trí của tôi" sau khi đã đi chỗ khác: đường đã lưu theo vị trí cũ không được dùng lại → test `relocateClearsRouteCache` ở Task 5.
3. Xoay màn / quay lại màn khi ViewModel còn sống: không xin vị trí và gọi server lại → test `startTwiceLocatesOnce` ở Task 5 (Fragment gọi `start()` mỗi lần `onViewCreated`, kể cả khi có `savedInstanceState`, tránh lỗi #11/#21 của F3/F5).
4. Từ chối quyền, vào cài đặt cấp quyền rồi quay lại app: màn tự chuyển sang tìm vị trí → test `permissionGrantedLaterLocates` ở Task 5 (Fragment kiểm lại quyền ở `onResume`).
5. Đứng sát chi nhánh (vài chục mét): hiện "20 m · 1 phút", không "0 phút" → test `durationRoundsUpToAtLeastOneMinute` ở Task 1.

---

### Task 1: Dữ liệu chi nhánh, khoảng cách, giờ mở cửa

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/branch/Branch.kt` (`GeoPoint`, `Branch`)
- Create: `app/src/main/java/com/flux/bank/domain/branch/GeoMath.kt`
- Create: `app/src/main/java/com/flux/bank/domain/branch/OpeningHours.kt`
- Create: `app/src/main/java/com/flux/bank/domain/branch/Branches.kt`
- Create: `app/src/main/java/com/flux/bank/core/format/DistanceFormatter.kt`
- Test: `app/src/test/java/com/flux/bank/domain/branch/GeoMathTest.kt`, `OpeningHoursTest.kt`, `BranchesTest.kt`, `app/src/test/java/com/flux/bank/core/DistanceFormatterTest.kt`

**Interfaces:**
- Produces:
  - `data class GeoPoint(val lat: Double, val lng: Double)`
  - `data class Branch(val id: String, val name: String, val address: String, val location: GeoPoint, val phone: String)`
  - `object GeoMath { fun distanceMeters(a: GeoPoint, b: GeoPoint): Double }` (haversine, R = 6.371.000 m)
  - `sealed interface OpenStatus { data class Open(val closesAt: LocalTime); data class Closed(val opensAt: LocalDateTime) }`, `object OpeningHours { fun status(nowMillis: Long): OpenStatus }`
  - `object Branches { val ALL: List<Branch>; val DEMO_ANCHOR: GeoPoint }` (`DEMO_ANCHOR` = cổng TDTU, 19 Nguyễn Hữu Thọ)
  - `object DistanceFormatter { fun distance(meters: Double): String; fun minutes(seconds: Double): Int }`

- [ ] **Step 1: Viết test fail**
  - `GeoMathTest`: `samePointIsZero` (= 0.0); `knownDistance`: Bến Thành (10.7725, 106.6980) → Nhà thờ Đức Bà (10.7798, 106.6990) ≈ 818 m (sai số ±10 m).
  - `OpeningHoursTest` (dựng `nowMillis` từ `ZonedDateTime` ở `VN_ZONE`): `weekdayMorningOpenUntil17` (T3 9:00 → `Open(17:00)`); `weekdayEveningClosedUntilTomorrow8` (T3 18:00 → `Closed(T4 8:00)`); `saturdayAfternoonClosedUntilMonday` (T7 13:00 → `Closed(T2 8:00)`); `sundayClosed` (CN 10:00 → `Closed(T2 8:00)`); `beforeOpeningSameDay` (T2 7:00 → `Closed(T2 8:00)`); `usesVietnamTimeWhateverDeviceZone` (đặt `TimeZone.setDefault(UTC)` trong test, 02:00 UTC T3 = 9:00 VN → `Open`; khôi phục zone sau test).
  - `DistanceFormatterTest`: `distance(850.4) == "850 m"`, `distance(999.0) == "999 m"`, `distance(1000.0) == "1,0 km"`, `distance(2149.0) == "2,1 km"`; `minutes(777.0) == 13`, `durationRoundsUpToAtLeastOneMinute`: `minutes(5.0) == 1`, `minutes(0.0) == 1`.
  - `BranchesTest`: `idsUnique`; `allInsideHoChiMinhCity` (lat 10.6–11.0, lng 106.5–106.9); `atLeastTwelve`; `demoClusterNearTdtu` (≥ 3 chi nhánh cách `DEMO_ANCHOR` ≤ 2.000 m).

- [ ] **Step 2: Chạy, thấy fail** — `./gradlew testDebugUnitTest --tests '*GeoMathTest' --tests '*OpeningHoursTest' --tests '*DistanceFormatterTest' --tests '*BranchesTest'` → FAIL (chưa có class).

- [ ] **Step 3: Viết code**
  - `DistanceFormatter.distance`: dưới 1.000 m làm tròn mét `"850 m"`; từ 1.000 m `"%.1f km"` dấu phẩy thập phân (`Locale("vi","VN")`). `minutes` = `ceil(seconds / 60)`, tối thiểu 1.
  - `Branches.ALL`: ~12 chi nhánh tên "FLUX …", địa chỉ đường thật ở TP.HCM, SĐT dạng `028 …`. **Cụm TDTU**: 3–4 chi nhánh trong 2 km, có ít nhất một ca "gần theo đường thẳng nhưng đi bộ xa hơn" (spec §5).

- [ ] **Step 4: Kiểm cụm demo bằng server thật** — với `DEMO_ANCHOR` và từng chi nhánh trong cụm, gọi URL server đi bộ (Global Constraints) bằng `curl`; ghi bảng (đường thẳng m, đi bộ m) vào KDoc của `Branches`. Đạt khi chi nhánh gần nhất theo đường thẳng **không** phải chi nhánh đi bộ ngắn nhất trong 3 ứng viên. Chưa đạt thì dời toạ độ và gọi lại.

- [ ] **Step 5: Chạy lại test** — lệnh ở Step 2 → PASS.

- [ ] **Step 6: Commit** — `git add app/src/main/java/com/flux/bank/domain/branch app/src/main/java/com/flux/bank/core/format/DistanceFormatter.kt app/src/test/java/com/flux/bank/domain/branch app/src/test/java/com/flux/bank/core/DistanceFormatterTest.kt && git commit -m "feat: branch data, distance and opening hours (F6)"`

---

### Task 2: Đường đi bộ qua OSRM

**Files:**
- Create: `app/src/main/java/com/flux/bank/data/routing/WalkingRouter.kt` (`WalkingRoute`, `WalkingRouter`, `RoutingException`)
- Create: `app/src/main/java/com/flux/bank/data/routing/OsrmWalkingRouter.kt`
- Modify: `app/build.gradle.kts` (`testImplementation("org.json:json:20240303")`)
- Test: `app/src/test/java/com/flux/bank/data/routing/OsrmWalkingRouterTest.kt`

**Interfaces:**
- Consumes: `GeoPoint` (Task 1)
- Produces:
  - `data class WalkingRoute(val distanceMeters: Double, val durationSeconds: Double, val path: List<GeoPoint>)`
  - `interface WalkingRouter { suspend fun route(from: GeoPoint, to: GeoPoint): WalkingRoute }` — ném `RoutingException` (hoặc `IOException`) khi lỗi
  - `class OsrmWalkingRouter(private val fetch: (url: String) -> String = ::httpGet) : WalkingRouter` với `companion fun url(from, to): String` và `companion fun parse(json: String): WalkingRoute`; `route` chạy trên `Dispatchers.IO`

- [ ] **Step 1: Viết test fail**
  - `parsesRealResponse`: chuỗi JSON rút gọn từ phản hồi thật (`"code":"Ok"`, `distance 968.6`, `duration 776.9`, 3 toạ độ `[lng,lat]`) → `distanceMeters == 968.6`, `durationSeconds == 776.9`, `path.first() == GeoPoint(10.732705, 106.697335)` (đảo thứ tự lng/lat).
  - `errorCodeThrows`: `{"code":"NoRoute","routes":[]}` → `RoutingException`.
  - `missingRoutesThrows`: `{"code":"Ok"}` → `RoutingException`.
  - `urlUsesLngLatOrderAndFootProfile`: `url(GeoPoint(10.1, 106.2), GeoPoint(10.3, 106.4))` == `".../routed-foot/route/v1/foot/106.2,10.1;106.4,10.3?overview=full&geometries=geojson"`.
  - `routeUsesFetcher`: `OsrmWalkingRouter { REAL_JSON }.route(a, b)` trả đúng `distanceMeters` (chạy trong `runTest`).

- [ ] **Step 2: Chạy, thấy fail** — `./gradlew testDebugUnitTest --tests '*OsrmWalkingRouterTest'` → FAIL.

- [ ] **Step 3: Viết code** — `httpGet`: `HttpURLConnection`, connect/read timeout 8.000 ms, `User-Agent: com.flux.bank`, mã ≠ 200 thì ném `RoutingException`. `url` định dạng số bằng `Locale.US` (dấu chấm).

- [ ] **Step 4: Chạy lại test** → PASS.

- [ ] **Step 5: Commit** — `git commit -m "feat: OSRM walking router (F6)"` (add các file trên).

---

### Task 3: `BranchRecommender` — gợi ý theo quãng đi bộ

**Files:**
- Create: `app/src/main/java/com/flux/bank/domain/branch/BranchRecommender.kt`
- Test: `app/src/test/java/com/flux/bank/domain/branch/BranchRecommenderTest.kt`

**Interfaces:**
- Consumes: `Branch`, `GeoPoint`, `GeoMath` (Task 1); `WalkingRouter`, `WalkingRoute` (Task 2)
- Produces:
  - `data class RankedBranch(val branch: Branch, val straightMeters: Double, val walk: WalkingRoute?)`
  - `data class Recommendation(val ranked: List<RankedBranch>, val pick: RankedBranch, val straightNearest: RankedBranch, val routed: Boolean) { val showsDetour: Boolean }` — `showsDetour` = `routed && pick.branch.id != straightNearest.branch.id`
  - `class BranchRecommender(private val router: WalkingRouter, private val branches: List<Branch> = Branches.ALL) { suspend fun recommend(user: GeoPoint): Recommendation; suspend fun routeTo(user: GeoPoint, branch: Branch): WalkingRoute }`
  - Hằng số `CANDIDATES = 3`, `MAX_ROUTED_STRAIGHT_METERS = 20_000.0`

- [ ] **Step 1: Viết test fail** (fake router: map `branchId → WalkingRoute` hoặc ném lỗi; đếm số lần gọi)
  - `picksShortestWalkNotStraightLine`: A thẳng 450 m / đi bộ 2.100 m, B thẳng 700 m / đi bộ 850 m, C thẳng 900 m / đi bộ 1.400 m → `pick == B`, `straightNearest == A`, `showsDetour == true`, `routed == true`.
  - `noDetourWhenSameBranch`: chi nhánh gần nhất cũng đi bộ ngắn nhất → `showsDetour == false`.
  - `routesOnlyThreeNearest`: 5 chi nhánh → router gọi đúng 3 lần, với 3 chi nhánh gần nhất.
  - `skipsFailedCandidate`: B lỗi, A 2.100 m, C 1.400 m → `pick == C`.
  - `allFailFallsBackToStraight`: cả 3 lỗi → `routed == false`, `pick == straightNearest`, mọi `walk == null`, `showsDetour == false`.
  - `farAwayDoesNotCallRouter`: người dùng ở Hà Nội → router gọi 0 lần, `routed == false`.
  - `rankedOrder`: ứng viên có `walk` xếp theo quãng đi bộ tăng dần ở đầu, phần còn lại theo đường thẳng tăng dần.

- [ ] **Step 2: Chạy, thấy fail** — `./gradlew testDebugUnitTest --tests '*BranchRecommenderTest'` → FAIL.

- [ ] **Step 3: Viết code** — gọi router song song cho 3 ứng viên (`coroutineScope { map { async { runCatching { router.route(...) } } } }`); chỉ bắt lỗi của router, không nuốt `CancellationException`. `routeTo` gọi router thẳng (ViewModel lo cache và lỗi).

- [ ] **Step 4: Chạy lại test** → PASS.

- [ ] **Step 5: Commit** — `git commit -m "feat: recommend branch by walking distance (F6)"`.

---

### Task 4: Cấu hình Maps, vị trí thật, nối `FluxApp`

**Files:**
- Modify: `app/build.gradle.kts` (đọc `maps.api.key` từ `localProps` như VNPay; `manifestPlaceholders["MAPS_API_KEY"]`; `implementation("com.google.android.gms:play-services-maps:19.2.0")`, `implementation("com.google.android.gms:play-services-location:21.3.0")`)
- Modify: `app/src/main/AndroidManifest.xml` (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`; `<meta-data android:name="com.google.android.geo.API_KEY" android:value="${MAPS_API_KEY}"/>` trong `<application>`)
- Create: `app/src/main/java/com/flux/bank/data/location/LocationSource.kt` (interface + `LocationResult`)
- Create: `app/src/main/java/com/flux/bank/data/location/FusedLocationSource.kt`
- Modify: `app/src/main/java/com/flux/bank/FluxApp.kt` (`walkingRouter`, `branchRecommender`, `locationSource` lazy)

**Interfaces:**
- Consumes: `OsrmWalkingRouter` (Task 2); `BranchRecommender` (Task 3)
- Produces: `sealed interface LocationResult { data class Found(val point: GeoPoint); data object Disabled; data object Timeout }`, `interface LocationSource { suspend fun current(): LocationResult }` (impl tự chờ tối đa 15 s); `FluxApp.instance.walkingRouter: WalkingRouter`, `.branchRecommender: BranchRecommender`, `.locationSource: LocationSource`; `FusedLocationSource(context: Context) : LocationSource`

- [ ] **Step 1: Viết code** — `FusedLocationSource.current()`: `LocationManagerCompat.isLocationEnabled` false → `Disabled`; `getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token).await()` trong `withTimeoutOrNull(15_000)`, null/hết giờ → `Timeout`. Quyền do Fragment kiểm trước khi gọi (`@SuppressLint("MissingPermission")` kèm comment lý do).
- [ ] **Step 2: Build** — `./gradlew assembleDebug` → BUILD SUCCESSFUL, cả khi `local.properties` chưa có `maps.api.key` (placeholder rỗng).
- [ ] **Step 3: Commit** — `git commit -m "chore: Maps SDK, fused location and F6 wiring"`.

---

### Task 5: `BranchMapViewModel`

**Files:**
- Create: `app/src/main/java/com/flux/bank/ui/customer/branch/BranchMapViewModel.kt`
- Modify: `app/src/test/java/com/flux/bank/fakes/Fakes.kt` (`FakeLocationSource`, `FakeWalkingRouter`)
- Test: `app/src/test/java/com/flux/bank/ui/customer/branch/BranchMapViewModelTest.kt`

**Interfaces:**
- Consumes: `BranchRecommender`, `Recommendation`, `RankedBranch` (Task 3); `WalkingRoute` (Task 2); `LocationSource`, `LocationResult`, `FluxApp.instance.locationSource`, `FluxApp.instance.branchRecommender` (Task 4)
- Produces:
  - `enum class LocationIssue { NO_PERMISSION, PERMISSION_BLOCKED, DISABLED, TIMEOUT }`
  - `sealed interface RouteUi { data class Walking(val route: WalkingRoute); data class Straight(val meters: Double) }`
  - `data class BranchMapState(val user: GeoPoint? = null, val issue: LocationIssue? = null, val loading: Boolean = false, val branches: List<RankedBranch> = emptyList(), val recommendation: Recommendation? = null, val selectedId: String? = null, val route: RouteUi? = null, val routeFailed: Boolean = false)` — `branches` khi chưa có vị trí = `Branches.ALL` xếp theo tên, `straightMeters = 0`, `walk = null`
  - `class BranchMapViewModel(location: LocationSource = FluxApp.instance.locationSource, recommender: BranchRecommender = FluxApp.instance.branchRecommender) : ViewModel()` với `val state: LiveData<BranchMapState>` và các hàm `start(hasPermission: Boolean)`, `onPermissionResult(granted: Boolean, canAskAgain: Boolean)`, `locate()`, `select(branchId: String)`, `backToRecommendation()`, `retryRoute()`

- [ ] **Step 1: Viết test fail** (dùng `InstantTaskExecutorRule` + `MainDispatcherRule` như `TopUpViewModelTest`; fake router có thể treo bằng `CompletableDeferred` để test thứ tự)
  - `noPermissionShowsBranchesByName`: `start(false)` → `issue == NO_PERMISSION`, `branches` theo tên, `user == null`.
  - `blockedPermission`: `onPermissionResult(false, canAskAgain = false)` → `PERMISSION_BLOCKED`.
  - `locationDisabled` / `locationTimeout`: fake trả `Disabled` / `Timeout` → `issue` tương ứng, `loading == false`.
  - `recommendsAndDrawsWalkingRoute`: vị trí OK + router OK → `recommendation.pick` đúng, `selectedId == pick.id`, `route == Walking(...)`, `issue == null`.
  - `allRoutesFailShowsStraightLine`: router luôn lỗi → `route == Straight(straightMeters của pick)`, `routeFailed == true`.
  - `selectOtherBranchRoutesIt` và `selectCachedBranchDoesNotCallRouter` (chọn ứng viên đã tính lúc gợi ý → router không được gọi thêm).
  - `backToRecommendationRestoresPick`.
  - `latestSelectionWins`: chọn A (router treo), chọn B (trả ngay), rồi A trả → `selectedId == B`, `route` là đường của B.
  - `relocateClearsRouteCache`: sau gợi ý, `locate()` với vị trí mới → router được gọi lại cho ứng viên.
  - `startTwiceLocatesOnce`: `start(true)` hai lần → `LocationSource.current()` gọi 1 lần.
  - `permissionGrantedLaterLocates`: `start(false)` rồi `onPermissionResult(true, true)` → gọi vị trí, `issue == null`.
  - `retryRouteAfterFailure`: router lỗi lần đầu, OK lần sau → `retryRoute()` cho `Walking`, `routeFailed == false`.

- [ ] **Step 2: Chạy, thấy fail** — `./gradlew testDebugUnitTest --tests '*BranchMapViewModelTest'` → FAIL.

- [ ] **Step 3: Viết code** — cache `Map<String, WalkingRoute>` nạp từ `recommendation.ranked` có `walk`, xoá khi `locate()`; mỗi lần chọn/định vị tăng một số thứ tự (generation), kết quả về với số cũ thì bỏ. `start` chỉ chạy lần đầu (cờ trong VM). `routeTo` lỗi khi chọn chi nhánh khác → `Straight` + `routeFailed = true`.

- [ ] **Step 4: Chạy lại test** → PASS; chạy cả bộ `./gradlew testDebugUnitTest` → PASS (292 cũ + mới).

- [ ] **Step 5: Commit** — `git commit -m "feat: branch map view model (F6)"`.

---

### Task 6: Màn bản đồ + nối trang chủ

**Files:**
- Create: `app/src/main/res/layout/fragment_branch_map.xml`, `item_branch.xml`
- Create: `app/src/main/res/raw/map_style.json` (giảm bão hoà, ẩn POI thương mại)
- Create: drawable `ic_walk.xml`, `ic_crosshair.xml`, `ic_wifi_slash.xml`, icon "không có vị trí" (Phosphor gần nghĩa nhất), `ic_phone_call.xml`, `ic_list.xml`, `bg_map_pin.xml`/`bg_map_pin_selected.xml` (hoặc vector ghim) theo mockup
- Create: `app/src/main/java/com/flux/bank/ui/customer/branch/BranchMapFragment.kt`, `BranchAdapter.kt`
- Modify: `app/src/main/res/values/strings.xml` (mọi chữ trong mockup: "Chi nhánh", "Gợi ý cho bạn", "Chi nhánh bạn chọn", "Gần nhất theo đường thẳng", "%1$s · %2$d phút đi bộ", "Đi bộ ngắn nhất", "Mở đến %s", "Đã đóng · mở %s", "%1$s gần hơn theo đường thẳng (%2$s) nhưng phải đi vòng, đi bộ %3$s.", "Xem tất cả chi nhánh", "Quay lại gợi ý", "Gọi chi nhánh", "Thử lại", "Cho phép vị trí", "Mở cài đặt định vị", "Đang tìm đường đi bộ ngắn nhất…", "%d chi nhánh · gần nhất trước", thông báo của 4 `LocationIssue`, "© OpenStreetMap", content description cho nút nổi)
- Modify: `app/src/main/java/com/flux/bank/ui/customer/home/HomeFragment.kt:166-182` (thêm `Utility.CHI_NHANH -> BranchMapFragment()`)

**Interfaces:**
- Consumes: `BranchMapViewModel`, `BranchMapState`, `LocationIssue`, `RouteUi` (Task 5); `OpeningHours`, `DistanceFormatter` (Task 1); `screenHost` (`ui/common/ScreenHost.kt`)
- Produces: `class BranchMapFragment : Fragment()` (không tham số; F7 mở bằng `screenHost.openScreen(BranchMapFragment())`)

- [ ] **Step 1: Layout theo mockup** — `SupportMapFragment` (`FragmentContainerView`) toàn màn; nút back nổi kèm chữ "Chi nhánh" và nút "Vị trí của tôi" (nền trắng, bo 14dp, viền hairline, cao 48dp); bottom sheet `BottomSheetBehavior` không scrim, nấc thu gọn chứa card gợi ý / khối thông báo / skeleton (`bg_skeleton`), nấc mở rộng là `RecyclerView` danh sách.
- [ ] **Step 2: Fragment** — xin quyền bằng `RequestMultiplePermissions` (fine + coarse; coarse đủ chạy); `canAskAgain` = `shouldShowRequestPermissionRationale` sau lần từ chối; `onResume` kiểm lại quyền, có quyền mà state đang `NO_PERMISSION`/`PERMISSION_BLOCKED` thì gọi `onPermissionResult(true, true)`. `PERMISSION_BLOCKED` → mở `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`; `DISABLED` → `Settings.ACTION_LOCATION_SOURCE_SETTINGS`; `TIMEOUT` → `locate()`. Bản đồ: `MapStyleOptions` từ `R.raw.map_style`, `isMyLocationEnabled` khi có quyền, tắt nút mặc định và toolbar, `setPadding` đáy = chiều cao sheet thu gọn; vẽ lại marker + polyline mỗi khi state đổi; camera bao người dùng + chi nhánh đang chọn (không có vị trí thì bao mọi chi nhánh). Chạm marker → `select(id)`. "Gọi chi nhánh" → `ACTION_DIAL`. "© OpenStreetMap" chỉ hiện khi `route is RouteUi.Walking`, chạm mở `https://www.openstreetmap.org/copyright`. Gọi `vm.start(hasPermission)` trong `onViewCreated` kể cả khi có `savedInstanceState`.
- [ ] **Step 3: Nối trang chủ** — nhánh `Utility.CHI_NHANH` trong `HomeFragment.openUtility`.
- [ ] **Step 4: Build + lint** — `./gradlew testDebugUnitTest assembleDebug lintDebug` → PASS, lint 0 error.
- [ ] **Step 5: Chạy trên Xiaomi** (cần `maps.api.key` của chủ dự án; chưa có thì chỉ kiểm danh sách + trạng thái, bản đồ xám) — cài APK, mở Chi nhánh: hộp thoại quyền → cho phép → có chấm, ghim, đường emerald, card đúng chữ; kéo sheet → danh sách; chạm chi nhánh khác → đường vẽ lại → "Quay lại gợi ý"; bật chế độ máy bay → "Vị trí của tôi" → nét đứt + "Thử lại"; từ chối quyền → khối thông báo. Chụp màn hình so với mockup.
- [ ] **Step 6: Commit** — `git commit -m "feat: branch map screen with walking route (F6)"`.

---

### Task 7: Tài liệu

**Files:**
- Modify: `README.md` (mục mới "Key Google Maps (F6)": tạo project Google Cloud **riêng**, gắn billing vào project đó, bật Maps SDK for Android, giới hạn key theo `com.flux.bank` + SHA-1 debug/release (lệnh `./gradlew signingReport`) + SHA-1 App signing khi lên Play, thêm `maps.api.key` vào `local.properties`, kiểm Firebase vẫn Spark; thêm F6 vào "Giới hạn đã biết": OSRM công cộng không SLA, đường OSM có thể lệch nét Google, vị trí gửi tới dịch vụ chỉ đường bên thứ ba)
- Modify: `PROJECT_CONTEXT.md` (mục Feature 6, dòng giới hạn, số test mới)
- Modify: `HANDOFF.md` (trạng thái F6, việc chủ dự án cần làm, lỗi nhỏ còn lại nếu có)

- [ ] **Step 1: Viết tài liệu** theo danh sách trên.
- [ ] **Step 2: Commit** — `git add README.md PROJECT_CONTEXT.md HANDOFF.md && git commit -m "docs: F6 navigation setup and limits"`.
