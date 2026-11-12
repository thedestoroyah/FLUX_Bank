package com.flux.bank.ui.customer.branch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.flux.bank.R
import com.flux.bank.core.format.DistanceFormatter
import com.flux.bank.databinding.FragmentBranchMapBinding
import com.flux.bank.domain.TransferPolicy
import com.flux.bank.domain.branch.Branches
import com.flux.bank.domain.branch.GeoPoint
import com.flux.bank.domain.branch.OpenStatus
import com.flux.bank.domain.branch.OpeningHours
import com.flux.bank.domain.branch.RankedBranch
import com.flux.bank.BuildConfig
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Màn Chi nhánh (spec F6 §7, mockup 2026-11-12-f6-navigation): bản đồ OpenStreetMap (OSMDroid) + bottom sheet.
 * Mở bằng `screenHost.openScreen(BranchMapFragment())`; ViewModel giữ trạng thái, Fragment chỉ vẽ.
 */
class BranchMapFragment : Fragment() {

    private var _binding: FragmentBranchMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BranchMapViewModel by viewModels()
    private val adapter = BranchAdapter { id -> viewModel.select(id); sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
    private lateinit var sheetBehavior: BottomSheetBehavior<View>
    private var markers: BranchMarkers? = null
    private var cameraKey: Any? = null
    private var bottomInset = 0
    private var topInset = 0

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result.values.any { it }
        viewModel.onPermissionResult(granted, canAskAgain = shouldShowRationale())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        configureOsmdroid()
        _binding = FragmentBranchMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sheetBehavior = BottomSheetBehavior.from(binding.sheet)
        binding.rvBranches.adapter = adapter
        applyInsets()
        bindActions()
        // Đổi peekHeight ngay trong lượt layout thì requestLayout() của BottomSheetBehavior bị bỏ qua, nên dời sang frame sau
        binding.sheetTop.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ -> v.post { updatePeek(v.height) } }
        binding.root.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ -> v.post { updateMaxHeight() } }
        setUpMap()
        viewModel.state.observe(viewLifecycleOwner, ::render)
        // Gọi cả khi có savedInstanceState: ViewModel tự bỏ qua nếu đã chạy (xoay màn không xin vị trí lại)
        val granted = hasPermission()
        viewModel.start(granted)
        if (!granted && savedInstanceState == null) requestPermission()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        // Quay về từ màn cài đặt: đã cấp quyền / đã bật định vị thì tự tìm lại
        val issue = viewModel.state.value?.issue
        if (!hasPermission()) return
        when (issue) {
            LocationIssue.NO_PERMISSION, LocationIssue.PERMISSION_BLOCKED ->
                viewModel.onPermissionResult(granted = true, canAskAgain = true)
            LocationIssue.DISABLED -> viewModel.locate()
            else -> Unit
        }
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    private fun bindActions() = with(binding) {
        btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        btnLocate.setOnClickListener { if (hasPermission()) viewModel.locate() else requestPermission() }
        btnRetry.setOnClickListener { viewModel.retryRoute() }
        tvOsm.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OSM_COPYRIGHT_URL))) }
        btnPrompt.setOnClickListener {
            when (viewModel.state.value?.issue) {
                LocationIssue.NO_PERMISSION -> requestPermission()
                LocationIssue.PERMISSION_BLOCKED -> startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireContext().packageName, null))
                )
                LocationIssue.DISABLED -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                LocationIssue.TIMEOUT -> viewModel.locate()
                null -> Unit
            }
        }
    }

    private fun applyInsets() {
        val baseTop = resources.getDimensionPixelSize(R.dimen.space_16)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.btnBack.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = baseTop + bars.top }
            topInset = bars.top
            updateMaxHeight()
            bottomInset = bars.bottom
            binding.rvBranches.updatePadding(bottom = bars.bottom)
            updatePeek(binding.sheetTop.height)
            insets
        }
    }

    /** Sheet thu gọn = phần trên cùng (card/thông báo); camera tính phần bị che này khi khung các điểm (xem [frame]). */
    private fun updatePeek(topHeight: Int) {
        if (_binding == null || topHeight == 0) return
        val peek = topHeight + bottomInset
        if (sheetBehavior.peekHeight != peek) sheetBehavior.peekHeight = peek
    }

    /**
     * Sheet kéo lên hết thì chừa khoảng trên cho nút back. Dùng maxHeight thay cho topMargin vì BottomSheetBehavior
     * cộng dồn margin trên vào vị trí thu gọn khi layout lại (sheet bị đẩy xuống gần hết màn).
     */
    private fun updateMaxHeight() {
        if (_binding == null || binding.root.height == 0) return
        val max = binding.root.height - resources.getDimensionPixelSize(R.dimen.map_sheet_top_gap) - topInset
        if (sheetBehavior.maxHeight != max) sheetBehavior.maxHeight = max
    }

    /** OSM chặn user-agent mặc định; cache ô bản đồ để trong cacheDir của app nên không cần quyền bộ nhớ. */
    private fun configureOsmdroid() {
        val ctx = requireContext().applicationContext
        Configuration.getInstance().apply {
            load(ctx, ctx.getSharedPreferences(OSMDROID_PREFS, android.content.Context.MODE_PRIVATE))
            userAgentValue = BuildConfig.APPLICATION_ID
            osmdroidBasePath = File(ctx.cacheDir, OSMDROID_DIR)
            osmdroidTileCache = File(osmdroidBasePath, OSMDROID_TILES)
        }
    }

    private fun setUpMap() = with(binding.mapView) {
        markers = BranchMarkers(requireContext())
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        isTilesScaledToDpi = true
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        minZoomLevel = MIN_ZOOM
        maxZoomLevel = MAX_ZOOM
        // Nền gần xám (spec §7) để đường emerald nổi lên
        overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(MAP_SATURATION) }))
        controller.setZoom(CITY_ZOOM)
        controller.setCenter(Branches.DEMO_ANCHOR.toOsm())
    }

    private fun render(state: BranchMapState) {
        renderSheet(state)
        renderList(state)
        renderMap(state)
    }

    private fun renderSheet(s: BranchMapState) = with(binding) {
        val rec = s.recommendation
        val selected = s.branches.firstOrNull { it.branch.id == s.selectedId }
        val showPrompt = s.issue != null
        val showSkeleton = !showPrompt && s.loading && rec == null
        val showCard = !showPrompt && !showSkeleton && selected != null && s.user != null
        prompt.isVisible = showPrompt
        skeleton.isVisible = showSkeleton
        card.isVisible = showCard
        tvSection.isVisible = !showPrompt
        if (showPrompt) renderPrompt(s.issue!!)
        if (showSkeleton) tvSection.setText(R.string.branch_finding)
        if (!showCard || selected == null || rec == null) return@with

        val isPick = selected.branch.id == rec.pick.branch.id
        tvSection.setText(
            when {
                isPick && (s.routeFailed || s.tooFar) -> R.string.branch_straight_nearest
                isPick -> R.string.branch_recommended
                else -> R.string.branch_selected
            }
        )
        tvName.text = selected.branch.name
        tvAddress.text = selected.branch.address
        renderOpen()
        when (val route = s.route) {
            is RouteUi.Walking -> {
                tvDistance.text = DistanceFormatter.distance(route.route.distanceMeters)
                tvWalkTime.text = getString(R.string.branch_walk_minutes, DistanceFormatter.minutes(route.route.durationSeconds))
            }
            is RouteUi.Straight -> {
                tvDistance.text = DistanceFormatter.distance(route.meters)
                tvWalkTime.setText(R.string.branch_straight_line)
            }
            null -> {
                tvDistance.text = ""
                tvWalkTime.setText(R.string.branch_finding)
            }
        }
        ivWalk.isVisible = s.route is RouteUi.Walking
        renderNote(s, isPick)
        btnCall.isVisible = !isPick
        btnCall.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + selected.branch.phone.replace(" ", ""))))
        }
        btnRetry.isVisible = s.routeFailed
        tvOsm.isVisible = s.route is RouteUi.Walking
        btnTextAction.setText(if (isPick) R.string.branch_see_all else R.string.branch_back_to_pick)
        btnTextAction.setIconResource(if (isPick) R.drawable.ic_list else R.drawable.ic_caret_left)
        btnTextAction.setOnClickListener {
            if (isPick) sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED else viewModel.backToRecommendation()
        }
    }

    private fun renderNote(s: BranchMapState, isPick: Boolean) = with(binding) {
        val rec = s.recommendation!!
        val nearestWalk = rec.ranked.firstOrNull { it.branch.id == rec.straightNearest.branch.id }?.walk
        val note: Pair<String, Int>? = when {
            s.routeFailed -> getString(R.string.branch_route_failed) to R.drawable.ic_wifi_slash
            isPick && rec.showsDetour && nearestWalk != null -> getString(
                R.string.branch_detour,
                rec.straightNearest.branch.name,
                DistanceFormatter.distance(rec.straightNearest.straightMeters),
                DistanceFormatter.distance(nearestWalk.distanceMeters)
            ) to R.drawable.ic_map_pin
            else -> null
        }
        tvNote.isVisible = note != null
        note?.let { (text, icon) ->
            tvNote.text = text
            tvNote.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        }
    }

    private fun renderOpen() = with(binding) {
        val dot = R.drawable.bg_status_dot
        when (val status = OpeningHours.status(System.currentTimeMillis())) {
            is OpenStatus.Open -> {
                tvOpen.text = getString(R.string.branch_open_until, status.closesAt.format(TIME))
                tvOpen.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, 0, 0, 0)
                tvOpen.compoundDrawablesRelative[0]?.setTint(ContextCompat.getColor(requireContext(), R.color.flux_emerald))
            }
            is OpenStatus.Closed -> {
                val today = Instant.now().atZone(TransferPolicy.VN_ZONE).toLocalDate()
                val time = status.opensAt.toLocalTime().format(TIME)
                val day = status.opensAt.toLocalDate()
                val whenText = when (day) {
                    today -> time
                    today.plusDays(1) -> getString(R.string.branch_opens_tomorrow, time)
                    else -> getString(R.string.branch_opens_on, time, resources.getStringArray(R.array.branch_weekdays)[day.dayOfWeek.value - 1])
                }
                tvOpen.text = getString(R.string.branch_closed_until, whenText)
                tvOpen.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, 0, 0, 0)
                tvOpen.compoundDrawablesRelative[0]?.setTint(ContextCompat.getColor(requireContext(), R.color.flux_text_tertiary))
            }
        }
    }

    private fun renderPrompt(issue: LocationIssue) = with(binding) {
        val (title, body, action) = when (issue) {
            LocationIssue.NO_PERMISSION -> Triple(R.string.branch_no_permission_title, R.string.branch_no_permission_body, R.string.branch_no_permission_action)
            LocationIssue.PERMISSION_BLOCKED -> Triple(R.string.branch_no_permission_title, R.string.branch_blocked_body, R.string.branch_blocked_action)
            LocationIssue.DISABLED -> Triple(R.string.branch_disabled_title, R.string.branch_disabled_body, R.string.branch_disabled_action)
            LocationIssue.TIMEOUT -> Triple(R.string.branch_timeout_title, R.string.branch_timeout_body, R.string.branch_retry)
        }
        tvPromptTitle.setText(title)
        tvPromptBody.setText(body)
        btnPrompt.setText(action)
    }

    private fun renderList(s: BranchMapState) {
        val hasLocation = s.user != null
        val pickId = s.recommendation?.takeIf { it.routed }?.pick?.branch?.id
        binding.tvListHeader.text = getString(
            if (hasLocation) R.string.branch_list_header_near else R.string.branch_list_header, s.branches.size
        )
        val rows = s.branches.map { BranchRow(it, showDistance = hasLocation, isPick = it.branch.id == pickId) }
        val firstChanged = adapter.currentList.firstOrNull()?.item?.branch?.id != rows.firstOrNull()?.item?.branch?.id
        // Đổi thứ tự (theo tên → theo khoảng cách) thì RecyclerView giữ vị trí theo dòng cũ, nên cuộn về đầu
        adapter.submitList(rows) { if (firstChanged && _binding != null) binding.rvBranches.scrollToPosition(0) }
    }

    /** Vẽ lại toàn bộ overlay mỗi lần state đổi (12 ghim + 1–2 đường, rẻ). */
    private fun renderMap(s: BranchMapState) {
        val icons = markers ?: return
        val mapView = binding.mapView
        mapView.overlays.clear()
        val user = s.user
        val target = s.branches.firstOrNull { it.branch.id == s.selectedId }
        // Ở quá xa (> 20 km) thì không vẽ đường: một nét đứt xuyên tỉnh/xuyên biển chỉ gây rối
        if (user != null && target != null && !s.tooFar) drawRoute(mapView, s.route, user, target)
        s.branches.sortedBy { it.branch.id == s.selectedId }.forEach { r ->
            val isSelected = r.branch.id == s.selectedId
            mapView.overlays.add(Marker(mapView).apply {
                position = r.branch.location.toOsm()
                icon = if (isSelected) icons.selected else icons.normal
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = r.branch.name
                infoWindow = null
                setOnMarkerClickListener { _, _ -> viewModel.select(r.branch.id); true }
            })
        }
        if (user != null) mapView.overlays.add(Marker(mapView).apply {
            position = user.toOsm()
            icon = icons.user
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            infoWindow = null
            setOnMarkerClickListener { _, _ -> true }
        })
        mapView.overlays.add(CopyrightOverlay(requireContext()).apply {
            setAlignBottom(false)
            setAlignRight(true)
            setOffset(resources.getDimensionPixelSize(R.dimen.space_16), topInset + resources.getDimensionPixelSize(R.dimen.space_16))
            setTextSize(resources.getDimensionPixelSize(R.dimen.map_copyright_text))
        })
        mapView.invalidate()
        moveCamera(user, target, s)
    }

    private fun drawRoute(mapView: MapView, route: RouteUi?, user: GeoPoint, target: RankedBranch) {
        val density = resources.displayMetrics.density
        fun line(points: List<GeoPoint>, colorRes: Int, widthDp: Float, dashed: Boolean = false) = Polyline(mapView).apply {
            setPoints(points.map { it.toOsm() })
            outlinePaint.color = color(colorRes)
            outlinePaint.strokeWidth = widthDp * density
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            if (dashed) outlinePaint.pathEffect = DashPathEffect(floatArrayOf(DASH_DP * density, DASH_DP * density), 0f)
            infoWindow = null
            setOnClickListener { _, _, _ -> false }
        }
        when (route) {
            is RouteUi.Walking -> {
                mapView.overlays.add(line(route.route.path, R.color.flux_surface, ROUTE_CASING_DP))
                mapView.overlays.add(line(route.route.path, R.color.flux_emerald, ROUTE_DP))
            }
            is RouteUi.Straight -> mapView.overlays.add(
                line(listOf(user, target.branch.location), R.color.flux_primary, STRAIGHT_DP, dashed = true)
            )
            null -> Unit
        }
    }

    /** Chỉ dời camera khi vị trí hoặc chi nhánh đang chọn đổi, không phải mỗi lần state phát lại. */
    private fun moveCamera(user: GeoPoint?, target: RankedBranch?, s: BranchMapState) {
        val key = listOf(user, target?.branch?.id, s.issue, s.tooFar)
        if (key == cameraKey) return
        cameraKey = key
        val points = when {
            s.tooFar -> Branches.ALL.map { it.location }
            user != null && target != null -> listOf(user, target.branch.location)
            user != null -> listOf(user)
            else -> Branches.ALL.map { it.location }
        }
        // Đợi layout xong (lần đầu map chưa có kích thước, sheet chưa có peek)
        binding.mapView.post { if (_binding != null) frame(points) }
    }

    /**
     * Khung các điểm vào phần bản đồ còn thấy: giữa nút back ở trên và sheet thu gọn ở dưới.
     * OSMDroid không có padding cho camera nên nới hộp lên/xuống theo tỉ lệ phần bị che.
     */
    private fun frame(points: List<GeoPoint>) {
        val mapView = binding.mapView
        if (points.size == 1 || mapView.height == 0) {
            mapView.controller.animateTo(points.first().toOsm(), SINGLE_POINT_ZOOM, CAMERA_ANIM_MS)
            return
        }
        val box = BoundingBox.fromGeoPointsSafe(points.map { it.toOsm() })
        val top = binding.btnBack.bottom
        val bottom = sheetBehavior.peekHeight
        val visible = mapView.height - top - bottom
        if (visible <= 0) {
            mapView.controller.animateTo(box.centerWithDateLine)
            return
        }
        val degPerPx = (box.latNorth - box.latSouth) / visible
        val padded = BoundingBox(box.latNorth + degPerPx * top, box.lonEast, box.latSouth - degPerPx * bottom, box.lonWest)
        mapView.zoomToBoundingBox(padded, true, resources.getDimensionPixelSize(R.dimen.space_32) * 2)
    }

    private fun hasPermission(): Boolean = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }

    private fun requestPermission() = permissionLauncher.launch(
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    // Sau một lần từ chối, rationale = true nghĩa là còn hỏi lại được; false = Android không cho hỏi nữa
    private fun shouldShowRationale() = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun color(res: Int) = ContextCompat.getColor(requireContext(), res)

    private fun GeoPoint.toOsm() = org.osmdroid.util.GeoPoint(lat, lng)

    override fun onDestroyView() {
        binding.mapView.onDetach()
        super.onDestroyView()
        markers = null
        cameraKey = null
        _binding = null
    }

    private companion object {
        const val OSM_COPYRIGHT_URL = "https://www.openstreetmap.org/copyright"
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
        const val ROUTE_DP = 5f
        const val ROUTE_CASING_DP = 9f
        const val STRAIGHT_DP = 2.5f
        const val DASH_DP = 6f
        const val SINGLE_POINT_ZOOM = 16.0
        const val CITY_ZOOM = 12.0
        const val MIN_ZOOM = 4.0
        const val MAX_ZOOM = 19.0
        const val MAP_SATURATION = 0.3f
        const val CAMERA_ANIM_MS = 600L
        const val OSMDROID_PREFS = "osmdroid"
        const val OSMDROID_DIR = "osmdroid"
        const val OSMDROID_TILES = "tiles"
    }
}
