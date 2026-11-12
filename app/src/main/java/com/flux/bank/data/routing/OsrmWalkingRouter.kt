package com.flux.bank.data.routing

import com.flux.bank.domain.branch.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Đường đi bộ từ server OSRM công cộng của FOSSGIS (profile đi bộ, miễn phí, không key, không cam kết SLA).
 * [fetch] nhận URL và trả nội dung phản hồi; mặc định là HTTP GET thật.
 */
class OsrmWalkingRouter(private val fetch: (url: String) -> String = ::httpGet) : WalkingRouter {

    override suspend fun route(from: GeoPoint, to: GeoPoint): WalkingRoute =
        withContext(Dispatchers.IO) { parse(fetch(url(from, to))) }

    companion object {
        private const val BASE = "https://routing.openstreetmap.de/routed-foot/route/v1/foot/"
        private const val TIMEOUT_MS = 8_000
        private const val USER_AGENT = "com.flux.bank"

        /** OSRM nhận toạ độ theo thứ tự kinh độ,vĩ độ; số luôn dùng dấu chấm. */
        fun url(from: GeoPoint, to: GeoPoint): String =
            String.format(Locale.US, "%s%s,%s;%s,%s?overview=full&geometries=geojson", BASE, from.lng, from.lat, to.lng, to.lat)

        /** Đọc tuyến đầu tiên; `code` khác "Ok" hoặc thiếu dữ liệu thì ném [RoutingException]. */
        fun parse(json: String): WalkingRoute = try {
            val root = JSONObject(json)
            val code = root.optString("code")
            if (code != "Ok") throw RoutingException("OSRM code=$code")
            val route = root.getJSONArray("routes").optJSONObject(0) ?: throw RoutingException("OSRM: không có tuyến")
            val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
            val path = (0 until coords.length()).map { i ->
                coords.getJSONArray(i).let { GeoPoint(lat = it.getDouble(1), lng = it.getDouble(0)) }
            }
            WalkingRoute(route.getDouble("distance"), route.getDouble("duration"), path)
        } catch (e: JSONException) {
            throw RoutingException("OSRM: phản hồi không hợp lệ", e)
        }

        private fun httpGet(url: String): String {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = TIMEOUT_MS
                conn.readTimeout = TIMEOUT_MS
                conn.setRequestProperty("User-Agent", USER_AGENT)
                if (conn.responseCode != HttpURLConnection.HTTP_OK) throw RoutingException("OSRM HTTP ${conn.responseCode}")
                return conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }
    }
}
