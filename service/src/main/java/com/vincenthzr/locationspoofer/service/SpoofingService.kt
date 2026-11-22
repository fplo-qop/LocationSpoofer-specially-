package com.vincenthzr.locationspoofer.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.vincenthzr.locationspoofer.data.model.RunMode
import com.vincenthzr.locationspoofer.data.state.MockLocationError
import com.vincenthzr.locationspoofer.data.state.MockLocationRuntime
import com.vincenthzr.locationspoofer.data.state.SpoofingState
import com.vincenthzr.locationspoofer.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SpoofingService : Service() {

    private lateinit var locationManager: LocationManager
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mockLocationEngine: MockLocationEngine? = null
    private var mockUpdateJob: Job? = null
    /** 当前这次服务启动使用的模式；null 表示服务没有处于一次有效模拟中。 */
    private var activeRunMode: RunMode? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_LAT = "EXTRA_LAT"
        const val EXTRA_LNG = "EXTRA_LNG"
        const val EXTRA_RUN_MODE = "EXTRA_RUN_MODE"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "SpoofingServiceChannel"

        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            // START_STICKY 在进程被系统回收后可能以 null intent 拉起服务。
            // Mock 模式没有 LSPosed 配置通道，必须从持久化设置里恢复坐标和模式，
            // 否则服务会处于“已启动但没有写 Test Provider”的空转状态。
            restoreAfterProcessRestart()
            return START_STICKY
        }
        when (intent.action) {
            ACTION_START -> {
                val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
                val lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
                val runMode = try {
                    RunMode.valueOf(intent.getStringExtra(EXTRA_RUN_MODE) ?: RunMode.LSPOSED.name)
                } catch (e: Exception) {
                    RunMode.LSPOSED
                }
                startSpoofing(lat, lng, runMode)
            }

            ACTION_STOP -> stopSpoofing()
        }
        return START_STICKY
    }

    private fun restoreAfterProcessRestart() {
        val settings = SettingsManager(this)
        if (!settings.isSpoofingActive) {
            stopSelf()
            return
        }
        val lat = settings.lastSpoofedLat.toDoubleOrNull() ?: 0.0
        val lng = settings.lastSpoofedLng.toDoubleOrNull() ?: 0.0
        if (lat == 0.0 && lng == 0.0) {
            stopSelf()
            return
        }
        val runMode = try {
            RunMode.valueOf(settings.runMode)
        } catch (_: Exception) {
            RunMode.LSPOSED
        }
        SpoofingState.isActive = true
        SpoofingState.latitude = lat
        SpoofingState.longitude = lng
        SpoofingState.simMode = "STILL"
        SpoofingState.isRouteMode = false
        startSpoofing(lat, lng, runMode)
    }

    @SuppressLint("WakelockTimeout")
    private fun startSpoofing(lat: Double, lng: Double, runMode: RunMode) {
        activeRunMode = runMode
        acquireWakeLock()

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = if (runMode.isMockLocation) {
            getString(
                R.string.spoofing_service_content_mock,
                String.format(java.util.Locale.US, "%.6f", lat),
                String.format(java.util.Locale.US, "%.6f", lng)
            )
        } else {
            getString(
                R.string.spoofing_service_content,
                String.format(java.util.Locale.US, "%.6f", lat),
                String.format(java.util.Locale.US, "%.6f", lng)
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.spoofing_service_title))
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            // 前台服务都起不来的话继续写 Test Provider 只会留下一个随时被杀的后台进程。
            isRunning = false
            if (runMode.isMockLocation) {
                MockLocationRuntime.report(MockLocationError.FOREGROUND_SERVICE_FAILED)
            }
            stopMockLocationEngine()
            stopSpoofing()
            return
        }
        isRunning = true

        if (runMode.isMockLocation) {
            startMockLocationEngine()
        } else {
            MockLocationRuntime.clear()
            stopMockLocationEngine()
        }
    }

    private fun startMockLocationEngine() {
        mockUpdateJob?.cancel()
        mockUpdateJob = null
        mockLocationEngine?.stop()

        val engine = MockLocationEngine(this)
        if (!engine.start()) {
            // 没有 AppOps 授权时不要留下“正在模拟”的假象。
            MockLocationRuntime.report(
                if (!com.vincenthzr.locationspoofer.utils.MockLocationPermission
                        .isMockLocationAllowed(this)
                ) {
                    MockLocationError.PERMISSION_REVOKED
                } else {
                    MockLocationError.PROVIDER_UNAVAILABLE
                }
            )
            stopSpoofing()
            return
        }
        MockLocationRuntime.clear()
        mockLocationEngine = engine
        mockUpdateJob = serviceScope.launch {
            while (isActive) {
                if (!engine.update()) {
                    val reason = if (!com.vincenthzr.locationspoofer.utils.MockLocationPermission
                            .isMockLocationAllowed(this@SpoofingService)
                    ) {
                        MockLocationError.PERMISSION_REVOKED
                    } else {
                        MockLocationError.PROVIDER_UNAVAILABLE
                    }
                    MockLocationRuntime.report(reason)
                    stopSpoofing()
                    break
                }
                delay(MockLocationEngine.UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopMockLocationEngine() {
        mockUpdateJob?.cancel()
        mockUpdateJob = null
        mockLocationEngine?.stop()
        mockLocationEngine = null
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "LocationSpoofer:BackgroundSpoofingWakeLock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(24 * 60 * 60 * 1000L) // 保持 24 小时唤醒，防止后台系统冻结
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Throwable) {
        }
        wakeLock = null
    }

    private fun stopSpoofing() {
        val wasMock = activeRunMode?.isMockLocation == true
        activeRunMode = null
        isRunning = false
        SpoofingState.isActive = false
        SettingsManager(this).isSpoofingActive = false
        stopMockLocationEngine()
        releaseWakeLock()
        if (wasMock) cleanupLegacyTestProviders()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanupLegacyTestProviders() {
        val providers =
            mutableListOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providers.add(LocationManager.FUSED_PROVIDER)
        }
        for (provider in providers) {
            // removeTestProvider 只对 test provider 生效；真实 Provider 会抛异常，
            // 因此这里不要先调用 setTestProviderEnabled，避免误碰系统真实定位源。
            try {
                locationManager.removeTestProvider(provider)
            } catch (_: Throwable) {
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.spoofing_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.spoofing_bg_notification_title)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 用户在多任务卡片划掉 Activity 时，如果正在模拟，继续在后台保持运行
        if (isRunning) {
            acquireWakeLock()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val wasMock = activeRunMode?.isMockLocation == true
        activeRunMode = null
        isRunning = false
        SpoofingState.isActive = false
        stopMockLocationEngine()
        releaseWakeLock()
        serviceScope.cancel()
        if (wasMock) cleanupLegacyTestProviders()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
