package com.flux.bank.ui.customer.branch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.bank.FluxApp
import com.flux.bank.data.location.LocationResult
import com.flux.bank.data.location.LocationSource
import com.flux.bank.data.routing.WalkingRoute
import com.flux.bank.domain.branch.BranchRecommender
import com.flux.bank.domain.branch.Branches
import com.flux.bank.domain.branch.GeoPoint
import com.flux.bank.domain.branch.RankedBranch
import com.flux.bank.domain.branch.Recommendation
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** Lý do chưa có vị trí; mỗi lý do có một nút sửa riêng trên sheet (mockup F6 màn 5). */
enum class LocationIssue { NO_PERMISSION, PERMISSION_BLOCKED, DISABLED, TIMEOUT }

/** Đường vẽ trên bản đồ: đường đi bộ thật, hoặc đường thẳng dự phòng khi không tải được. */
sealed interface RouteUi {
    data class Walking(val route: WalkingRoute) : RouteUi
    data class Straight(val meters: Double) : RouteUi
}

/**
 * Trạng thái màn chi nhánh. Khi chưa có vị trí, [branches] là mọi chi nhánh xếp theo tên (khoảng cách = 0, không dùng).
 * [loading] = đang lấy vị trí / đang tính đường (sheet hiện khung giữ chỗ).
 */
data class BranchMapState(
    val user: GeoPoint? = null,
    val issue: LocationIssue? = null,
    val loading: Boolean = false,
    val branches: List<RankedBranch> = emptyList(),
    val recommendation: Recommendation? = null,
    val selectedId: String? = null,
    val route: RouteUi? = null,
    val routeFailed: Boolean = false
)

/** Màn bản đồ chi nhánh (spec F6 §5, §7): lấy vị trí, gợi ý, chọn chi nhánh khác, lưu đường đã tính trong phiên. */
class BranchMapViewModel(
    private val location: LocationSource = FluxApp.instance.locationSource,
    private val recommender: BranchRecommender = FluxApp.instance.branchRecommender
) : ViewModel() {

    private val _state = MutableLiveData(BranchMapState(branches = byName()))
    val state: LiveData<BranchMapState> = _state

    private var started = false
    // Số thứ tự để bỏ kết quả về muộn: lần định vị mới huỷ mọi kết quả cũ, lần chọn mới huỷ kết quả chọn cũ
    private var locateGen = 0
    private var routeGen = 0
    private val cache = mutableMapOf<String, WalkingRoute>()

    /** Gọi mỗi lần màn tạo view; chỉ chạy lần đầu nên xoay màn không xin vị trí lại. */
    fun start(hasPermission: Boolean) {
        if (started) return
        started = true
        if (hasPermission) locate() else showIssue(LocationIssue.NO_PERMISSION)
    }

    /** Kết quả hộp thoại quyền (hoặc kiểm lại khi quay về từ cài đặt). */
    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        started = true
        when {
            granted -> locate()
            canAskAgain -> showIssue(LocationIssue.NO_PERMISSION)
            else -> showIssue(LocationIssue.PERMISSION_BLOCKED)
        }
    }

    /** Lấy vị trí rồi tính gợi ý; đường đã lưu theo vị trí cũ bị xoá. */
    fun locate() {
        val gen = ++locateGen
        routeGen++
        cache.clear()
        _state.value = current().copy(loading = true, issue = null)
        viewModelScope.launch {
            val result = location.current()
            if (gen != locateGen) return@launch
            when (result) {
                LocationResult.Disabled -> showIssue(LocationIssue.DISABLED)
                LocationResult.Timeout -> showIssue(LocationIssue.TIMEOUT)
                is LocationResult.Found -> {
                    val rec = recommender.recommend(result.point)
                    if (gen != locateGen) return@launch
                    rec.ranked.forEach { r -> r.walk?.let { cache[r.branch.id] = it } }
                    _state.value = BranchMapState(
                        user = result.point,
                        branches = rec.ranked,
                        recommendation = rec,
                        selectedId = rec.pick.branch.id,
                        route = routeOf(rec.pick),
                        routeFailed = !rec.routed
                    )
                }
            }
        }
    }

    /** Chọn một chi nhánh (chạm ghim hoặc dòng danh sách) và vẽ đường tới đó; đã tính rồi thì dùng lại. */
    fun select(branchId: String) {
        val s = current()
        val target = s.branches.firstOrNull { it.branch.id == branchId } ?: return
        val gen = ++routeGen
        val user = s.user ?: return run { _state.value = s.copy(selectedId = branchId) }
        cache[branchId]?.let {
            _state.value = s.copy(selectedId = branchId, route = RouteUi.Walking(it), routeFailed = false, loading = false)
            return
        }
        _state.value = s.copy(selectedId = branchId, route = null, routeFailed = false, loading = true)
        val locGen = locateGen
        viewModelScope.launch {
            val route = try {
                recommender.routeTo(user, target.branch)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null // không tải được thì vẽ đường thẳng và cho thử lại
            }
            if (gen != routeGen || locGen != locateGen) return@launch
            route?.let { cache[branchId] = it }
            _state.value = current().copy(
                loading = false,
                route = route?.let { RouteUi.Walking(it) } ?: RouteUi.Straight(target.straightMeters),
                routeFailed = route == null
            )
        }
    }

    /** Về lại chi nhánh được gợi ý. */
    fun backToRecommendation() {
        val rec = current().recommendation ?: return
        routeGen++
        _state.value = current().copy(
            selectedId = rec.pick.branch.id, route = routeOf(rec.pick), routeFailed = !rec.routed, loading = false
        )
    }

    /** Nút "Thử lại" khi không tải được đường: gợi ý lỗi thì tính lại từ đầu, chi nhánh tự chọn thì gọi lại đường. */
    fun retryRoute() {
        val s = current()
        val rec = s.recommendation
        val selected = s.selectedId
        if (rec == null || selected == null || selected == rec.pick.branch.id) locate() else select(selected)
    }

    private fun showIssue(issue: LocationIssue) {
        _state.value = BranchMapState(issue = issue, branches = byName())
    }

    private fun routeOf(r: RankedBranch): RouteUi = r.walk?.let { RouteUi.Walking(it) } ?: RouteUi.Straight(r.straightMeters)

    private fun current() = _state.value!!

    private fun byName() = Branches.ALL.sortedBy { it.name }.map { RankedBranch(it, straightMeters = 0.0, walk = null) }
}
