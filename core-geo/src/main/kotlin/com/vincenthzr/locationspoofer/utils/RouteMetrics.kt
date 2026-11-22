package com.vincenthzr.locationspoofer.utils

import com.vincenthzr.locationspoofer.utils.CoordinateUtils.LatLng

/**
 * 路线的纯几何统计：只回答"这条路有多长、是不是闭环"，不涉及时间与播放状态。
 *
 * 选点过程中要实时显示长度、路线库列表要展示里程，都不应该为此构造 [RoutePath]
 * （构造它会为播放引擎建一份累计距离数组），所以单独放在这里，App 端和 Hook 端都能用。
 */
object RouteMetrics {

    /** 与 [RoutePath] 判定闭环的阈值保持一致：首尾相距 5 米以内视为闭环 */
    const val CLOSED_LOOP_TOLERANCE_M = 5.0

    /** 逐段累加的总长度（米）；不足两个点时为 0 */
    fun totalDistance(points: List<LatLng>): Double {
        var sum = 0.0
        for (i in 1 until points.size) {
            sum += GeoMath.distance(points[i - 1], points[i])
        }
        return sum
    }

    /** 首尾相距 [CLOSED_LOOP_TOLERANCE_M] 米以内视为闭环，走完会从头继续而不是原路折返 */
    fun isClosedLoop(points: List<LatLng>): Boolean =
        points.size >= 2 && GeoMath.distance(points.first(), points.last()) <= CLOSED_LOOP_TOLERANCE_M
}
