package com.vincenthzr.locationspoofer.ui.recording

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.vincenthzr.locationspoofer.ui.R
import com.vincenthzr.locationspoofer.ui.components.map.wgs84ToGcj02
import java.util.Locale

/**
 * 实际路线录制的前台服务。
 *
 * 使用 FusedLocationProviderClient 采集真实 GPS，按距离抽稀后写入 [RouteRecordingStore]。
 * 页面切到后台或锁屏后录制仍会继续，通知栏提供“结束录制”入口。
 */
class RouteRecordingService : Service() {

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::handleLocation)
        }
    }

    private var tracking = false
    private var lastNotificationAt = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> finishRecording()
            ACTION_DISCARD -> discardRecording()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        if (tracking || RouteRecordingStore.state.value.phase == RouteRecordingPhase.RECORDING) return

        // Android 14 起，location 类型的 Foreground Service 同时要求精确/粗略定位权限。
        // 权限不满足时直接结束，避免带着 location 类型调用 startForeground 触发安全异常。
        if (!hasFineLocationPermission()) {
            RouteRecordingStore.fail(RouteRecordingError.PERMISSION_DENIED)
            stopSelf()
            return
        }
        if (!isLocationEnabled()) {
            RouteRecordingStore.fail(RouteRecordingError.LOCATION_UNAVAILABLE)
            stopSelf()
            return
        }

        RouteRecordingStore.begin()
        try {
            startForegroundCompat(buildNotification())
        } catch (e: Exception) {
            e.printStackTrace()
            RouteRecordingStore.fail(
                if (e is SecurityException) RouteRecordingError.PERMISSION_DENIED
                else RouteRecordingError.LOCATION_UNAVAILABLE
            )
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_INTERVAL_MS)
            .setMinUpdateDistanceMeters(0f)
            .build()

        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            // 先拿一次当前点，避免用户站在原地时前几秒没有任何轨迹
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location -> location?.let(::handleLocation) }
            tracking = true
            updateNotification(force = true)
        } catch (e: Exception) {
            e.printStackTrace()
            tracking = false
            try {
                fusedClient.removeLocationUpdates(locationCallback)
            } catch (removeError: Exception) {
                removeError.printStackTrace()
            }
            RouteRecordingStore.fail(
                if (e is SecurityException) RouteRecordingError.PERMISSION_DENIED
                else RouteRecordingError.LOCATION_UNAVAILABLE
            )
            stopForegroundCompat()
            stopSelf()
        }
    }

    private fun finishRecording() {
        stopLocationUpdates()
        RouteRecordingStore.finish()
        stopForegroundCompat()
        stopSelf()
    }

    private fun discardRecording() {
        stopLocationUpdates()
        RouteRecordingStore.reset()
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopLocationUpdates() {
        if (!tracking) return
        tracking = false
        try {
            fusedClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleLocation(location: Location) {
        if (!tracking) return
        if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_METERS) return

        // 项目内路线/配置统一使用 GCJ-02；海外坐标转换函数会原样返回
        val gcj = wgs84ToGcj02(location.latitude, location.longitude)
        if (RouteRecordingStore.addPoint(gcj.first, gcj.second)) {
            updateNotification(force = false)
        }
    }

    private fun updateNotification(force: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastNotificationAt < NOTIFICATION_UPDATE_INTERVAL_MS) return
        lastNotificationAt = now
        try {
            notificationManager.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            // Android 13 未授予通知权限时 notify 可能被系统丢弃，录制本身不受影响
            e.printStackTrace()
        }
    }

    private fun buildNotification(): Notification {
        val state = RouteRecordingStore.state.value
        val contentIntent = packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RouteRecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_route_recording)
            .setContentTitle(getString(R.string.route_record_notification_title))
            .setContentText(
                getString(
                    R.string.route_record_notification_text,
                    state.points.size,
                    formatDistance(state.distanceMeters)
                )
            )
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(R.drawable.ic_route_recording, getString(R.string.route_record_notification_stop), stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.route_record_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.route_record_notification_channel_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun isLocationEnabled(): Boolean {
        val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun formatDistance(meters: Double): String =
        if (meters >= 1000.0) {
            getString(
                R.string.route_length_km,
                String.format(Locale.US, "%.2f", meters / 1000.0)
            )
        } else {
            getString(
                R.string.route_length_m,
                String.format(Locale.US, "%.0f", meters)
            )
        }

    override fun onDestroy() {
        // 进程被系统回收时也保留已经采到的轨迹，用户回到页面仍可预览/保存
        if (tracking) {
            stopLocationUpdates()
            RouteRecordingStore.finish()
        }
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "route_recording"
        private const val NOTIFICATION_ID = 4301
        private const val LOCATION_INTERVAL_MS = 1000L
        private const val MAX_ACCURACY_METERS = 80f
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 5000L

        const val ACTION_START = "com.vincenthzr.locationspoofer.action.START_ROUTE_RECORDING"
        const val ACTION_STOP = "com.vincenthzr.locationspoofer.action.STOP_ROUTE_RECORDING"
        const val ACTION_DISCARD = "com.vincenthzr.locationspoofer.action.DISCARD_ROUTE_RECORDING"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, RouteRecordingService::class.java).setAction(ACTION_START)
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, RouteRecordingService::class.java).setAction(ACTION_STOP)
            )
        }

        fun discard(context: Context) {
            context.startService(
                Intent(context, RouteRecordingService::class.java).setAction(ACTION_DISCARD)
            )
        }
    }
}
