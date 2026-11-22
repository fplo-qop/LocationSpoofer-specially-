package com.vincenthzr.locationspoofer.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.vincenthzr.locationspoofer.data.model.AppMapType
import com.vincenthzr.locationspoofer.data.model.MapEngine
import com.vincenthzr.locationspoofer.ui.components.map.AMapViewContainer
import com.vincenthzr.locationspoofer.ui.components.map.BaiduMapViewContainer
import com.vincenthzr.locationspoofer.ui.components.map.GoogleMapViewContainer

interface AppMapMarker {
    fun setPosition(lat: Double, lng: Double)
}

enum class MarkerType { GREEN, RED, ORANGE, DEFAULT }

interface AppMapController {
    fun clear()
    fun addPolyline(points: List<Pair<Double, Double>>, colorInt: Int, width: Float)
    fun addCircle(
        lat: Double,
        lng: Double,
        radius: Double,
        fillColorInt: Int,
        strokeColorInt: Int,
        strokeWidth: Float
    )

    fun addMarker(lat: Double, lng: Double, title: String, type: MarkerType): AppMapMarker
    fun animateCamera(lat: Double, lng: Double, zoom: Float? = null)
    fun fitBounds(points: List<Pair<Double, Double>>, padding: Int)
    fun fitBounds(
        points: List<Pair<Double, Double>>,
        paddingLeft: Int,
        paddingTop: Int,
        paddingRight: Int,
        paddingBottom: Int
    ) {
        fitBounds(points, maxOf(paddingLeft, paddingRight, paddingTop, paddingBottom))
    }

    fun moveCamera(lat: Double, lng: Double, zoom: Float? = null)

    /**
     * 屏幕像素坐标（相对于地图 View 左上角）反投影为经纬度。
     * 返回的坐标与 RoutePoint / addPolyline 使用的坐标口径保持一致。
     */
    fun screenToLatLng(x: Float, y: Float): Pair<Double, Double>?

    /** 在当前缩放级别基础上增减缩放级别，用于手绘时用按钮替代被手势层拦截的双指缩放 */
    fun zoomBy(delta: Float)

    /** 手绘时临时关闭地图平移/缩放手势，避免底层地图跟着手指一起拖动 */
    fun setGesturesEnabled(enabled: Boolean)

    val cameraTargetLat: Double?
    val cameraTargetLng: Double?
    fun setOnCameraChangeListener(onFinish: (lat: Double, lng: Double) -> Unit)
    fun setOnCameraMoveListener(onMove: (lat: Double, lng: Double) -> Unit)
    fun disableUiControls()
    fun setMapType(type: AppMapType)
    fun setDarkMode(isDark: Boolean, context: android.content.Context)
}

// Re-export implementations for backwards compatibility if needed
typealias AMapControllerImpl = com.vincenthzr.locationspoofer.ui.components.map.AMapControllerImpl
typealias BaiduMapControllerImpl = com.vincenthzr.locationspoofer.ui.components.map.BaiduMapControllerImpl
typealias GMapControllerImpl = com.vincenthzr.locationspoofer.ui.components.map.GMapControllerImpl
typealias GaodeMarkerHelper = com.vincenthzr.locationspoofer.ui.components.map.GaodeMarkerHelper

@Composable
fun AppMapView(
    mapEngine: MapEngine,
    isDomestic: Boolean,
    modifier: Modifier = Modifier,
    onMapReady: (AppMapController) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = isSystemInDarkTheme()
    var mapController by remember { mutableStateOf<AppMapController?>(null) }

    LaunchedEffect(isDark, mapController) {
        mapController?.setDarkMode(isDark, context)
    }

    val activeEngine = if (mapEngine == MapEngine.AUTO) {
        MapEngine.AMAP
    } else {
        mapEngine
    }

    when (activeEngine) {
        MapEngine.BAIDU -> {
            BaiduMapViewContainer(
                context = context,
                lifecycleOwner = lifecycleOwner,
                isDark = isDark,
                modifier = modifier,
                onMapReady = onMapReady,
                onControllerCreated = { mapController = it }
            )
        }

        MapEngine.GOOGLE -> {
            GoogleMapViewContainer(
                context = context,
                lifecycleOwner = lifecycleOwner,
                isDark = isDark,
                modifier = modifier,
                onMapReady = onMapReady,
                onControllerCreated = { mapController = it }
            )
        }

        else -> {
            AMapViewContainer(
                context = context,
                lifecycleOwner = lifecycleOwner,
                isDark = isDark,
                modifier = modifier,
                onMapReady = onMapReady,
                onControllerCreated = { mapController = it }
            )
        }
    }
}
