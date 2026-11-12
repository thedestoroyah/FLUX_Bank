package com.flux.bank.domain.branch

import com.flux.bank.data.routing.WalkingRoute
import com.flux.bank.data.routing.WalkingRouter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

/** Một chi nhánh kèm khoảng cách đường thẳng và (nếu đã tính được) đường đi bộ. */
data class RankedBranch(val branch: Branch, val straightMeters: Double, val walk: WalkingRoute?)

/**
 * Kết quả gợi ý. [ranked]: các ứng viên có đường đi bộ (ngắn trước), rồi các chi nhánh còn lại theo đường thẳng.
 * [routed] = false khi không tính được đường đi bộ nào (lỗi mạng, hoặc ở quá xa).
 */
data class Recommendation(
    val ranked: List<RankedBranch>,
    val pick: RankedBranch,
    val straightNearest: RankedBranch,
    val routed: Boolean
) {
    /** Có hiện dòng "X gần hơn theo đường thẳng nhưng phải đi vòng" hay không. */
    val showsDetour: Boolean get() = routed && pick.branch.id != straightNearest.branch.id
}

/** Gợi ý chi nhánh có quãng đi bộ ngắn nhất trong vài chi nhánh gần nhất theo đường thẳng (spec F6 §5). */
class BranchRecommender(
    private val router: WalkingRouter,
    private val branches: List<Branch> = Branches.ALL
) {

    /**
     * Xếp hạng [branches] quanh [user]. Router lỗi với ứng viên nào thì bỏ ứng viên đó; lỗi hết thì gợi ý theo đường thẳng.
     * Yêu cầu [branches] không rỗng.
     */
    suspend fun recommend(user: GeoPoint): Recommendation {
        val byStraight = branches
            .map { RankedBranch(it, GeoMath.distanceMeters(user, it.location), walk = null) }
            .sortedBy { it.straightMeters }
        val nearest = byStraight.first()
        if (nearest.straightMeters > MAX_ROUTED_STRAIGHT_METERS) {
            return Recommendation(byStraight, nearest, nearest, routed = false)
        }
        val candidates = byStraight.take(CANDIDATES)
        val routed = coroutineScope {
            candidates.map { c -> async { c.copy(walk = routeOrNull(user, c.branch)) } }.awaitAll()
        }.filter { it.walk != null }.sortedBy { it.walk!!.distanceMeters }
        if (routed.isEmpty()) return Recommendation(byStraight, nearest, nearest, routed = false)
        val routedIds = routed.map { it.branch.id }.toSet()
        val ranked = routed + byStraight.filter { it.branch.id !in routedIds }
        return Recommendation(ranked, routed.first(), nearest, routed = true)
    }

    /** Đường đi bộ tới một chi nhánh bất kỳ; lỗi để bên gọi xử lý. */
    suspend fun routeTo(user: GeoPoint, branch: Branch): WalkingRoute = router.route(user, branch.location)

    private suspend fun routeOrNull(user: GeoPoint, branch: Branch): WalkingRoute? = try {
        router.route(user, branch.location)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null // ứng viên lỗi thì bỏ qua, các ứng viên khác vẫn dùng được
    }

    companion object {
        const val CANDIDATES = 3
        /** Xa hơn mức này thì không ai đi bộ, không gọi server. */
        const val MAX_ROUTED_STRAIGHT_METERS = 20_000.0
    }
}
