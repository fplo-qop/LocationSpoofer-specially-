package com.vincenthzr.locationspoofer.ui.recording

import android.os.SystemClock
import com.vincenthzr.locationspoofer.data.model.RoutePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 录制路线的生命周期状态 */
enum class RouteRecordingPhase {
    IDLE,
    RECORDING,
    /** 服务已经停止，轨迹保留在这里，等待路线页接收 */
    FINISHED
}

/** 录制启动失败的原因，界面据此给出可读提示 */
enum class RouteRecordingError {
    PERMISSION_DENIED,
    LOCATION_UNAVAILABLE
}

data class RouteRecordingState(
    val phase: RouteRecordingPhase = RouteRecordingPhase.IDLE,
    val points: List<RoutePoint> = emptyList(),
    val startedAtElapsedMs: Long = 0L,
    val elapsedMs: Long = 0L,
    val distanceMeters: Double = 0.0,
    val error: RouteRecordingError? = null
)

/**
 * 录制服务与路线页之间的进程内状态通道。
 * 服务每秒最多写入一个有效定位点，路线页只读取，不直接持有定位客户端。
 */
object RouteRecordingStore {
    private const val MIN_POINT_DISTANCE_METERS = 1.2
    private const val MAX_JUMP_METERS = 1000.0
    private const val MAX_POINTS = 30000

    private val _state = MutableStateFlow(RouteRecordingState())
    val state: StateFlow<RouteRecordingState> = _state.asStateFlow()

    fun begin(nowElapsedMs: Long = SystemClock.elapsedRealtime()) {
        _state.value = RouteRecordingState(
            phase = RouteRecordingPhase.RECORDING,
            startedAtElapsedMs = nowElapsedMs
        )
    }

    /** @return 是否真的新增了一个轨迹点（原地抖动、GPS 跳点会被丢弃） */
    fun addPoint(lat: Double, lng: Double, nowElapsedMs: Long = SystemClock.elapsedRealtime()): Boolean {
        val current = _state.value
        if (current.phase != RouteRecordingPhase.RECORDING) return false
        if (current.points.size >= MAX_POINTS) return false

        val last = current.points.lastOrNull()
        var addedDistance = 0.0
        if (last != null) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                last.lat,
                last.lng,
                lat,
                lng,
                results
            )
            val distance = results[0].toDouble()
            if (!distance.isFinite() || distance < MIN_POINT_DISTANCE_METERS) return false
            // 单次定位跳变过大通常是信号漂移，不写入轨迹，避免路线出现长直线
            if (distance > MAX_JUMP_METERS) return false
            addedDistance = distance
        }

        _state.value = current.copy(
            points = current.points + RoutePoint(lat, lng),
            elapsedMs = (nowElapsedMs - current.startedAtElapsedMs).coerceAtLeast(0L),
            distanceMeters = current.distanceMeters + addedDistance
        )
        return true
    }

    fun finish(nowElapsedMs: Long = SystemClock.elapsedRealtime()) {
        val current = _state.value
        if (current.phase != RouteRecordingPhase.RECORDING) return
        _state.value = current.copy(
            phase = RouteRecordingPhase.FINISHED,
            elapsedMs = (nowElapsedMs - current.startedAtElapsedMs).coerceAtLeast(current.elapsedMs)
        )
    }

    fun fail(error: RouteRecordingError) {
        _state.value = RouteRecordingState(error = error)
    }

    fun clearError() {
        if (_state.value.error != null && _state.value.phase == RouteRecordingPhase.IDLE) {
            _state.value = RouteRecordingState()
        }
    }

    /** 路线页接收完 FINISHED 轨迹后调用，回到空闲状态 */
    fun consumeFinished() {
        if (_state.value.phase == RouteRecordingPhase.FINISHED) {
            _state.value = RouteRecordingState()
        }
    }

    fun reset() {
        _state.value = RouteRecordingState()
    }
}
