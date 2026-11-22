package com.vincenthzr.locationspoofer.data.model

/**
 * 一条收藏的路线。
 *
 * @param timestamp 收藏时间（毫秒）。Room 里一直存着这个字段，只是此前没有透出到界面；
 *   老版本写在 SharedPreferences 的路线没有时间，给 0 表示"未知"，界面会跳过这一项。
 */
data class SavedRoute(
    val name: String,
    val points: List<RoutePoint>,
    val timestamp: Long = 0L
)
