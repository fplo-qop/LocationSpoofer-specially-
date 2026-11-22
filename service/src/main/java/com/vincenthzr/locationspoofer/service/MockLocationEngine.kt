package com.vincenthzr.locationspoofer.service

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.vincenthzr.locationspoofer.data.state.SpoofingState
import com.vincenthzr.locationspoofer.utils.CoordinateUtils
import com.vincenthzr.locationspoofer.utils.MockLocationPermission
import com.vincenthzr.locationspoofer.utils.SettingsManager

/**
 * 免 Root / ADB 模式使用的标准 Android Mock Location 引擎。
 *
 * 它不依赖 LSPosed，也不需要 root，只要求系统把本应用的
 * AppOps `android:mock_location` 设为 allowed：
 * - 免 Root 模式：用户在开发者选项里选择“模拟位置应用”；
 * - ADB 模式：`adb shell appops set <包名> android:mock_location allow`。
 *
 * 注入前会把项目统一使用的 GCJ-02 坐标转成 Android 原生定位使用的 WGS-84。
 */
class MockLocationEngine(context: Context) {

    companion object {
        private const val TAG = "MockLocationEngine"
        const val UPDATE_INTERVAL_MS = 300L
        private const val SATELLITE_COUNT = 8
    }

    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val settingsManager = SettingsManager(appContext)
    private val providers = BuildProviders()

    private var running = false
    private var lastLocation: Location? = null
    private var addedProviders = mutableSetOf<String>()

    fun start(): Boolean {
        if (running) return true
        if (!MockLocationPermission.isMockLocationAllowed(appContext)) {
            Log.w(TAG, "android:mock_location is not allowed")
            return false
        }
        val lm = locationManager ?: return false

        var enabledCount = 0
        for (provider in providers.list()) {
            if (addProvider(lm, provider)) {
                enabledCount++
            }
        }
        running = enabledCount > 0
        lastLocation = null
        if (!running) {
            Log.w(TAG, "no test provider could be added")
        }
        return running
    }

    /**
     * 把当前 [SpoofingState] 的位置写入测试 Provider。
     * 返回 false 表示授权被撤销或写入失败，调用方应停止服务。
     */
    fun update(): Boolean {
        if (!running) return true
        val lm = locationManager ?: return false
        if (!SpoofingState.isActive) return true

        val lat = SpoofingState.latitude
        val lng = SpoofingState.longitude
        if (lat == 0.0 && lng == 0.0) return true

        val wgs = CoordinateUtils.gcj02ToWgs84(lat, lng)
        val now = System.currentTimeMillis()
        val elapsed = android.os.SystemClock.elapsedRealtimeNanos()
        val altitude = settingsManager.altitude.toDoubleOrNull() ?: 0.0

        var speed = 0f
        var bearing = SpoofingState.simBearing
        lastLocation?.let { previous ->
            val dt = (now - previous.time) / 1000.0
            if (dt > 0.0) {
                val distance = FloatArray(1)
                Location.distanceBetween(
                    previous.latitude, previous.longitude,
                    wgs.lat, wgs.lng,
                    distance
                )
                // 只要两次写入之间发生了真实位移，就按位移计算速度。
                // 这样定点模式下在地图上手动跳点/拖动时，运动类 App 也能看到“移动”，
                // 而不是始终收到 speed=0 的静止点。
                if (distance[0] > 0.0f) {
                    speed = (distance[0] / dt).coerceIn(0.0, 100.0).toFloat()
                    if (bearing == 0f) {
                        bearing = previous.bearingTo(Location(LocationManager.GPS_PROVIDER).apply {
                            latitude = wgs.lat
                            longitude = wgs.lng
                        })
                    }
                }
            }
        }

        var written = false
        for (provider in providers.list()) {
            if (provider !in addedProviders) continue
            val location = Location(provider).apply {
                latitude = wgs.lat
                longitude = wgs.lng
                accuracy = 5f
                this.altitude = altitude
                time = now
                elapsedRealtimeNanos = elapsed
                this.speed = speed
                this.bearing = bearing
                verticalAccuracyMeters = 8f
                speedAccuracyMetersPerSecond = 0.5f
                bearingAccuracyDegrees = 5f
                // 影梭会为 GPS/fused 写入卫星数扩展；部分运动/轨迹类 App
                // 会读取这个字段判断 GPS 是否有效，缺少时可能直接丢弃定位。
                if (provider != LocationManager.NETWORK_PROVIDER) {
                    extras = Bundle().apply {
                        putInt("satellites", SATELLITE_COUNT)
                    }
                }
            }
            try {
                lm.setTestProviderLocation(provider, location)
                written = true
            } catch (e: Throwable) {
                Log.w(TAG, "setTestProviderLocation failed for $provider", e)
                if (e is SecurityException) {
                    running = false
                    return false
                }
            }
        }
        if (written) lastLocation = Location(providers.primary()).apply {
            latitude = wgs.lat
            longitude = wgs.lng
            time = now
            elapsedRealtimeNanos = elapsed
        }
        return written
    }

    fun stop() {
        running = false
        lastLocation = null
        // 只清理本次真正添加成功的 Provider。真实 GPS/NETWORK/FUSED Provider 不能碰，
        // 否则在没有 test provider 的情况下调用 setTestProviderEnabled 可能影响系统定位。
        addedProviders.toList().forEach { provider ->
            try {
                locationManager?.setTestProviderEnabled(provider, false)
            } catch (_: Throwable) {
            }
            try {
                locationManager?.removeTestProvider(provider)
            } catch (_: Throwable) {
            }
        }
        addedProviders.clear()
    }

    private fun addProvider(lm: LocationManager, provider: String): Boolean {
        // 影梭的做法：先清理同名残留 test provider，再 addTestProvider。
        // removeTestProvider 对真实 Provider 会抛异常，这里直接忽略，
        // 不会影响系统真实 GPS/网络/融合 Provider。
        try {
            lm.removeTestProvider(provider)
        } catch (_: Throwable) {
        }

        try {
            addTestProviderRaw(lm, provider)
        } catch (e: Throwable) {
            Log.w(TAG, "addTestProvider failed for $provider", e)
            return false
        }

        try {
            lm.setTestProviderEnabled(provider, true)
        } catch (e: Throwable) {
            Log.w(TAG, "setTestProviderEnabled failed for $provider", e)
            // 已经 addTestProvider 成功但启用失败时，把这个半成品 test provider 清掉。
            try {
                lm.removeTestProvider(provider)
            } catch (_: Throwable) {
            }
            return false
        }
        addedProviders.add(provider)
        Log.d(TAG, "test provider ready: $provider")
        return true
    }

    private fun addTestProviderRaw(lm: LocationManager, provider: String) {
        // 参数与影梭 2.5.3 保持一致：
        // - GPS: 需要卫星、高精度、高功耗；
        // - NETWORK: 需要网络/基站、粗精度、低功耗；
        // - FUSED: 需要网络/卫星/基站、高精度、高功耗。
        // 尤其是 fused，不能按粗精度网络 Provider 配置，否则部分
        // 使用融合定位的运动 App 会认为精度不足而收不到有效位置。
        when (provider) {
            LocationManager.GPS_PROVIDER -> lm.addTestProvider(
                provider,
                false,  // requiresNetwork
                true,   // requiresSatellite
                false,  // requiresCell
                false,  // hasMonetaryCost
                true,   // supportsAltitude
                true,   // supportsSpeed
                true,   // supportsBearing
                Criteria.POWER_HIGH,
                Criteria.ACCURACY_FINE
            )

            LocationManager.NETWORK_PROVIDER -> lm.addTestProvider(
                provider,
                true,   // requiresNetwork
                false,  // requiresSatellite
                true,   // requiresCell
                true,   // hasMonetaryCost
                true,   // supportsAltitude
                true,   // supportsSpeed
                true,   // supportsBearing
                Criteria.POWER_LOW,
                Criteria.ACCURACY_COARSE
            )

            LocationManager.FUSED_PROVIDER -> lm.addTestProvider(
                provider,
                true,   // requiresNetwork
                true,   // requiresSatellite
                true,   // requiresCell
                false,  // hasMonetaryCost
                true,   // supportsAltitude
                true,   // supportsSpeed
                true,   // supportsBearing
                Criteria.POWER_HIGH,
                Criteria.ACCURACY_FINE
            )

            else -> lm.addTestProvider(
                provider,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                Criteria.POWER_HIGH,
                Criteria.ACCURACY_FINE
            )
        }
    }

    private class BuildProviders {
        fun list(): List<String> = buildList {
            // 按影梭的顺序：先 fused，再 gps，最后 network。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }

        fun primary(): String = LocationManager.GPS_PROVIDER
    }
}
