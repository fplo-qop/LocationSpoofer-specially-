package com.vincenthzr.locationspoofer.data.model

/**
 * 定位注入的运行模式。
 *
 * - [LSPOSED]：原有方案，需要 Root + LSPosed，能够伪装系统级定位与无线环境。
 * - [ROOTLESS]：普通 App 的 Mock Location 方案，用户在开发者选项里手动选择本应用。
 * - [ADB]：同样是 Mock Location，但由 ADB 授予 `android:mock_location` AppOps，
 *   适合开发者选项里没有“选择模拟位置应用”入口的机型。
 */
enum class RunMode {
    LSPOSED,
    ROOTLESS,
    ADB;

    /** 是否走标准 Android Mock Location 通道，而不是 LSPosed Hook。 */
    val isMockLocation: Boolean
        get() = this != LSPOSED
}
