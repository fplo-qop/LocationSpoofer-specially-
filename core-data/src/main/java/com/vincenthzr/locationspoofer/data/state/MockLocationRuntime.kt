package com.vincenthzr.locationspoofer.data.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Mock Location 服务在后台失败时的原因。 */
enum class MockLocationError {
    PERMISSION_REVOKED,
    PROVIDER_UNAVAILABLE,
    FOREGROUND_SERVICE_FAILED
}

/**
 * 前台服务与 UI 之间的轻量错误通道。
 * 服务进程和界面在同一应用进程内，用 StateFlow 比广播更简单，也不会暴露给其他应用。
 */
object MockLocationRuntime {
    private val _error = MutableStateFlow<MockLocationError?>(null)
    val error: StateFlow<MockLocationError?> = _error.asStateFlow()

    fun report(error: MockLocationError) {
        _error.value = error
    }

    fun clear() {
        _error.value = null
    }
}
