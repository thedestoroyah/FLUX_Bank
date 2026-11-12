package com.flux.bank.ui.customer.branch

import android.Manifest
import android.annotation.SuppressLint
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Màn Chi nhánh (spec F6 §7, mockup 2026-11-12-f6-navigation): bản đồ Google Maps + bottom sheet.
 * Mở bằng `screenHost.openScreen(BranchMapFragment())`; ViewModel giữ trạng thái, Fragment chỉ vẽ.
 */
class BranchMapFragment : Fragment() {

    private var _binding: FragmentBranchMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BranchMapViewModel by viewModels()
    private val adapter = BranchAdapter { id -> viewModel.select(id); sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
    private lateinit var sheetBehavior: BottomSheetBehavior<View>
    private var map: GoogleMap? = null
    private var markers: BranchMarkers? = null
    private var cameraKey: Any? = null
    private var bottomInset = 0
    private var topInset = 0

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result.values.any { it }
        if (granted) enableMyLocation()
        viewModel.onPermissionResult(granted, canAskAgain = shouldShowRationale())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        (childFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment).getMapAsync(::onMapReady)
        viewModel.state.observe(viewLifecycleOwner, ::render)
        // Gọi cả khi có savedInstanceState: ViewModel tự bỏ qua nếu đã chạy (xoay màn không xin vị trí lại)
        val granted = hasPermission()
        viewModel.start(granted)
        if (!granted && savedInstanceState == null) requestPermission()
    }

    override fun onResume() {
        super.onResume()
        // Quay về từ màn cài đặt: đã cấp quyền / đã bật định vị thì tự tìm lại
        val issue = viewModel.state.value?.issue
        if (!hasPermission()) return
        when (issue) {
            LocationIssue.NO_PERMISSION, LocationIssue.PERMISSION_BLOCKED -> {
                enableMyLocation()
                viewModel.onPermissionResult(granted = true, canAskAgain = true)
            }
            LocationIssue.DISABLED -> viewModel.locate()
            else -> Unit
        }
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

    /** Sheet thu gọn = phần trên cùng (card/thông báo); bản đồ chừa đúng phần đó để logo Google và camera không bị che. */
    private fun updatePeek(topHeight: Int) {
        if (_binding == null || topHeight == 0) return
        val peek = topHeight + bottomInset
        if (sheetBehavior.peekHeight != peek) sheetBehavior.peekHeight = peek
        map?.setPadding(0, binding.btnBack.bottom, 0, peek)
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

    private fun onMapReady(googleMap: GoogleMap) {
        if (_binding == null) return
        map = googleMap
        markers = BranchMarkers(requireContext())
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.setOnMarkerClickListener { m -> (m.tag as? String)?.let(viewModel::select); true }
        enableMyLocation()
        updatePeek(binding.sheetTop.height)
        viewModel.state.value?.let { renderMap(it) }
    }

    @SuppressLint("MissingPermission") // đã kiểm hasPermission() ngay trước
    private fun enableMyLocation() {
        if (hasPermission()) map?.isMyLocationEnabled = true
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
                isPick && s.routeFailed -> R.string.branch_straight_nearest
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

    private fun renderMap(s: BranchMapState) {
        val googleMap = map ?: return
        val icons = markers ?: return
        googleMap.clear()
        s.branches.forEach { r ->
            val isSelected = r.branch.id == s.selectedId
            googleMap.addMarker(
                MarkerOptions()
                    .position(r.branch.location.toLatLng())
                    .title(r.branch.name)
                    .icon(if (isSelected) icons.selected else icons.normal)
                    .anchor(0.5f, 0.5f)
                    .zIndex(if (isSelected) 1f else 0f)
            )?.tag = r.branch.id
        }
        val user = s.user
        val target = s.branches.firstOrNull { it.branch.id == s.selectedId }
        if (user != null && target != null) drawRoute(googleMap, s.route, user, target)
        moveCamera(googleMap, user, target, s)
    }

    private fun drawRoute(googleMap: GoogleMap, route: RouteUi?, user: GeoPoint, target: RankedBranch) {
        val density = resources.displayMetrics.density
        when (route) {
            is RouteUi.Walking -> {
                val points = route.route.path.map { it.toLatLng() }
                googleMap.addPolyline(PolylineOptions().addAll(points).width(ROUTE_CASING_DP * density).color(color(R.color.flux_surface)))
                googleMap.addPolyline(PolylineOptions().addAll(points).width(ROUTE_DP * density).color(color(R.color.flux_emerald)).zIndex(1f))
            }
            is RouteUi.Straight -> googleMap.addPolyline(
                PolylineOptions().add(user.toLatLng(), target.branch.location.toLatLng())
                    .width(STRAIGHT_DP * density).color(color(R.color.flux_primary))
                    .pattern(listOf(Dash(DASH_DP * density), Gap(DASH_DP * density)))
            )
            null -> Unit
        }
    }

    /** Chỉ dời camera khi vị trí hoặc chi nhánh đang chọn đổi, không phải mỗi lần state phát lại. */
    private fun moveCamera(googleMap: GoogleMap, user: GeoPoint?, target: RankedBranch?, s: BranchMapState) {
        val key = Triple(user, target?.branch?.id, s.issue)
        if (key == cameraKey) return
        cameraKey = key
        val points = when {
            user != null && target != null -> listOf(user, target.branch.location)
            user != null -> listOf(user)
            else -> Branches.ALL.map { it.location }
        }
        val padding = resources.getDimensionPixelSize(R.dimen.space_32) * 2
        if (points.size == 1) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points[0].toLatLng(), SINGLE_POINT_ZOOM))
            return
        }
        val bounds = LatLngBounds.builder().apply { points.forEach { include(it.toLatLng()) } }.build()
        binding.root.post { if (_binding != null) googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding)) }
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

    private fun GeoPoint.toLatLng() = LatLng(lat, lng)

    override fun onDestroyView() {
        super.onDestroyView()
        map = null
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
        const val SINGLE_POINT_ZOOM = 15f
    }
}
