package com.vincenthzr.locationspoofer.ui.screen.tabs.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vincenthzr.locationspoofer.data.model.RoutePoint
import com.vincenthzr.locationspoofer.ui.R
import com.vincenthzr.locationspoofer.utils.CoordinateUtils.LatLng
import com.vincenthzr.locationspoofer.utils.RouteMetrics
import java.util.Locale

/** 把 RoutePoint 列表换算成几何统计用的坐标列表 */
fun List<RoutePoint>.toLatLngs(): List<LatLng> = map { LatLng(it.lat, it.lng) }

/**
 * 米数 → 便于一眼读数的文案：不足 1 公里用整米（128 米），超过就用两位小数的公里（1.23 公里）。
 * 数值统一用 Locale.US 格式化，避免阿拉伯语等区域把小数点变成别的符号导致读不出来。
 */
@Composable
fun formatRouteLength(meters: Double): String {
    return if (meters >= 1000.0) {
        stringResource(R.string.route_length_km, String.format(Locale.US, "%.2f", meters / 1000.0))
    } else {
        stringResource(R.string.route_length_m, String.format(Locale.US, "%.0f", meters))
    }
}

/** 闭环 / 往返（首尾没接上就是沿路线走到终点再折返） */
@Composable
fun routeShapeLabel(points: List<LatLng>): String =
    stringResource(
        if (RouteMetrics.isClosedLoop(points)) R.string.route_loop_closed
        else R.string.route_loop_back
    )

/** 收藏时间：路线库里只用来辅助辨识，精确到分钟足够 */
fun formatRouteTimestamp(timestamp: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(timestamp))
