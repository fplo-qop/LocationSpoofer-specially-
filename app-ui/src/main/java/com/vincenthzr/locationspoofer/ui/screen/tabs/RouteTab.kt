package com.vincenthzr.locationspoofer.ui.screen.tabs

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vincenthzr.locationspoofer.ui.R
import com.vincenthzr.locationspoofer.data.model.*
import com.vincenthzr.locationspoofer.ui.components.AppMapController
import com.vincenthzr.locationspoofer.ui.components.AppMapMarker
import com.vincenthzr.locationspoofer.ui.components.MapTypeDialog
import com.vincenthzr.locationspoofer.ui.components.MarkerType
import com.vincenthzr.locationspoofer.ui.recording.RouteRecordingError
import com.vincenthzr.locationspoofer.ui.recording.RouteRecordingPhase
import com.vincenthzr.locationspoofer.ui.recording.RouteRecordingService
import com.vincenthzr.locationspoofer.ui.recording.RouteRecordingStore
import com.vincenthzr.locationspoofer.ui.screen.AppPoiItem
import com.vincenthzr.locationspoofer.ui.screen.HomeSearchBar
import com.vincenthzr.locationspoofer.ui.screen.performPoiSearch
import com.vincenthzr.locationspoofer.ui.screen.tabs.route.*
import com.vincenthzr.locationspoofer.ui.theme.AccentBlue
import com.vincenthzr.locationspoofer.ui.theme.AccentOrange
import com.vincenthzr.locationspoofer.ui.theme.noRippleClickable
import com.vincenthzr.locationspoofer.ui.components.MapCoverageHelper
import com.vincenthzr.locationspoofer.viewmodel.applyInputRoutePoints
import com.vincenthzr.locationspoofer.viewmodel.beginManualRouteInput
import com.vincenthzr.locationspoofer.viewmodel.cancelRoutePlanning
import com.vincenthzr.locationspoofer.viewmodel.prepareRouteSimulationOptions
import com.vincenthzr.locationspoofer.viewmodel.MainViewModel
import com.vincenthzr.locationspoofer.viewmodel.setAltitude
import com.vincenthzr.locationspoofer.viewmodel.setAltitudeVariation
import com.vincenthzr.locationspoofer.viewmodel.ManageDataViewModel
import com.vincenthzr.locationspoofer.viewmodel.addRoutePoint
import com.vincenthzr.locationspoofer.viewmodel.fetchCurrentLocation
import com.vincenthzr.locationspoofer.viewmodel.finishSelectingPoints
import com.vincenthzr.locationspoofer.viewmodel.isDomesticEnvironment
import com.vincenthzr.locationspoofer.viewmodel.performLocalSearch
import com.vincenthzr.locationspoofer.viewmodel.restartSelectingPoints
import com.vincenthzr.locationspoofer.viewmodel.setCustomSpeedMs
import com.vincenthzr.locationspoofer.viewmodel.setEnableStepSimulation
import com.vincenthzr.locationspoofer.viewmodel.setIsAutoCadence
import com.vincenthzr.locationspoofer.viewmodel.setMapEngine
import com.vincenthzr.locationspoofer.viewmodel.setMapType
import com.vincenthzr.locationspoofer.viewmodel.setRouteRunMode
import com.vincenthzr.locationspoofer.viewmodel.setRouteSimMode
import com.vincenthzr.locationspoofer.viewmodel.setSatelliteCount
import com.vincenthzr.locationspoofer.viewmodel.setSearchMode
import com.vincenthzr.locationspoofer.viewmodel.setStepCadenceSpm
import com.vincenthzr.locationspoofer.viewmodel.setStopAtDestination
import com.vincenthzr.locationspoofer.viewmodel.setUseRealRoute
import com.vincenthzr.locationspoofer.viewmodel.startRoutePlanning
import com.vincenthzr.locationspoofer.viewmodel.pauseRouteSimulation
import com.vincenthzr.locationspoofer.viewmodel.resumeRouteSimulation
import com.vincenthzr.locationspoofer.viewmodel.stopRoutePlanning
import com.vincenthzr.locationspoofer.viewmodel.toggleEnableJitter
import com.vincenthzr.locationspoofer.viewmodel.toggleMockBluetooth
import com.vincenthzr.locationspoofer.viewmodel.toggleMockCell
import com.vincenthzr.locationspoofer.viewmodel.toggleMockWifi
import com.vincenthzr.locationspoofer.viewmodel.undoLastRoutePoint
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val DRAW_SAMPLE_PIXELS = 4f
private const val DRAW_SAMPLE_METERS = 0.8f
private const val DRAW_LINE_WIDTH = 8f
private val DRAW_LINE_COLOR = android.graphics.Color.parseColor("#FFFF7A00")
private val RECORD_LINE_COLOR = android.graphics.Color.parseColor("#FF00B578")

@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun RouteTab(
    viewModel: MainViewModel,
    uiState: AppState,
    mapController: AppMapController?,
    isActive: Boolean = true,
    bottomBarHeight: Dp = 90.dp,
    manageDataViewModel: ManageDataViewModel = koinViewModel()
) {
    val collectionRoutes by viewModel.collectionRoutes.collectAsState()
    val context = LocalContext.current
    var showMapTypeDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showSaveRouteDialog by remember { mutableStateOf(false) }
    var showSavedRoutesDialog by remember { mutableStateOf(false) }
    val isDomestic = viewModel.isDomesticEnvironment()
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AppPoiItem>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchBounds by remember { mutableStateOf(Rect.Zero) }
    var searchResultBounds by remember { mutableStateOf(Rect.Zero) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    var bottomActionHeightPx by remember { mutableIntStateOf(0) }

    val stage = uiState.routePlanStage
    val isRunning = stage == RoutePlanStage.RUNNING
    val isManual = uiState.routeRunMode == RouteRunMode.MANUAL
    val routePoints = uiState.routePoints

    // 手绘 / 实际路线录制的页面状态。轨迹在完成前只存在于本地，避免每次移动都刷新全局路线。
    val recordingState by RouteRecordingStore.state.collectAsState()
    // 录制期间离开路线页再回来时，页面会重新组合；按服务状态恢复录制面板，
    // 避免只留下通知栏里的“结束录制”而页面看不到进度。
    var inputMode by remember {
        mutableStateOf(
            when (recordingState.phase) {
                RouteRecordingPhase.RECORDING,
                RouteRecordingPhase.FINISHED -> RouteInputMode.RECORD
                RouteRecordingPhase.IDLE -> RouteInputMode.NONE
            }
        )
    }
    val drawPoints = remember { mutableStateListOf<RoutePoint>() }
    val drawStrokeEnds = remember { mutableStateListOf<Int>() }
    var drawRevision by remember { mutableIntStateOf(0) }
    var recordedDrawnCount by remember { mutableIntStateOf(0) }
    var pendingInputMode by remember { mutableStateOf<RouteInputMode?>(null) }
    var showDiscardDrawDialog by remember { mutableStateOf(false) }
    var showDiscardRecordDialog by remember { mutableStateOf(false) }
    var strokeStartCount by remember { mutableIntStateOf(0) }
    var lastDrawScreen by remember { mutableStateOf<Offset?>(null) }
    var drawMapMovable by remember { mutableStateOf(false) }

    fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun isDeviceLocationEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun clearDrawDraft() {
        drawPoints.clear()
        drawStrokeEnds.clear()
        drawRevision++
    }

    fun beginDrawNow() {
        viewModel.beginManualRouteInput()
        clearDrawDraft()
        drawMapMovable = false
        isSearchActive = false
        showSearchResults = false
        focusManager.clearFocus()
        keyboardController?.hide()
        inputMode = RouteInputMode.DRAW
    }

    fun beginRecordNow() {
        if (uiState.isSpoofingActive) {
            Toast.makeText(context, context.getString(R.string.route_record_stop_spoof_first), Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasFineLocationPermission()) {
            Toast.makeText(context, context.getString(R.string.route_record_permission), Toast.LENGTH_SHORT).show()
            return
        }
        if (!isDeviceLocationEnabled()) {
            Toast.makeText(context, context.getString(R.string.route_record_location_disabled), Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.beginManualRouteInput()
        RouteRecordingStore.reset()
        recordedDrawnCount = 0
        isSearchActive = false
        showSearchResults = false
        focusManager.clearFocus()
        keyboardController?.hide()
        inputMode = RouteInputMode.RECORD
        try {
            RouteRecordingService.start(context)
        } catch (e: Exception) {
            e.printStackTrace()
            RouteRecordingStore.fail(RouteRecordingError.LOCATION_UNAVAILABLE)
        }
    }

    fun requestDrawMode() {
        if (routePoints.isNotEmpty()) {
            pendingInputMode = RouteInputMode.DRAW
        } else {
            beginDrawNow()
        }
    }

    fun requestRecordMode() {
        if (uiState.isSpoofingActive) {
            Toast.makeText(context, context.getString(R.string.route_record_stop_spoof_first), Toast.LENGTH_SHORT).show()
            return
        }
        if (routePoints.isNotEmpty()) {
            pendingInputMode = RouteInputMode.RECORD
        } else {
            beginRecordNow()
        }
    }

    fun discardDrawMode() {
        clearDrawDraft()
        drawMapMovable = false
        inputMode = RouteInputMode.NONE
        viewModel.cancelRoutePlanning()
    }

    fun finishDrawMode() {
        if (drawPoints.size < 2) {
            Toast.makeText(context, context.getString(R.string.route_draw_need_points), Toast.LENGTH_SHORT).show()
            return
        }
        val points = drawPoints.toList()
        clearDrawDraft()
        drawMapMovable = false
        inputMode = RouteInputMode.NONE
        viewModel.applyInputRoutePoints(points)
    }

    fun undoLastDrawStroke() {
        val end = drawStrokeEnds.lastOrNull() ?: return
        val start = drawStrokeEnds.getOrNull(drawStrokeEnds.lastIndex - 1) ?: 0
        val from = minOf(start, end)
        while (drawPoints.size > from) {
            drawPoints.removeAt(drawPoints.lastIndex)
        }
        drawStrokeEnds.removeAt(drawStrokeEnds.lastIndex)
        lastDrawScreen = null
        drawRevision++
    }

    fun appendDrawPoint(screen: Offset) {
        val map = mapController ?: return
        val previousScreen = lastDrawScreen
        if (previousScreen != null && (screen - previousScreen).getDistance() < DRAW_SAMPLE_PIXELS) {
            return
        }
        val latLng = map.screenToLatLng(screen.x, screen.y) ?: return
        val lastPoint = drawPoints.lastOrNull()
        if (lastPoint != null) {
            val distance = FloatArray(1)
            android.location.Location.distanceBetween(
                lastPoint.lat,
                lastPoint.lng,
                latLng.first,
                latLng.second,
                distance
            )
            if (distance[0] < DRAW_SAMPLE_METERS) {
                lastDrawScreen = screen
                return
            }
            map.addPolyline(
                listOf(
                    Pair(lastPoint.lat, lastPoint.lng),
                    Pair(latLng.first, latLng.second)
                ),
                DRAW_LINE_COLOR,
                DRAW_LINE_WIDTH
            )
        }
        drawPoints.add(RoutePoint(latLng.first, latLng.second))
        lastDrawScreen = screen
    }

    // 录制服务结束时（无论来自页面按钮还是通知栏）把轨迹交给现有路线链路预览
    LaunchedEffect(recordingState.phase, recordingState.error) {
        when (recordingState.phase) {
            RouteRecordingPhase.RECORDING -> {
                if (inputMode != RouteInputMode.RECORD) inputMode = RouteInputMode.RECORD
            }
            RouteRecordingPhase.FINISHED -> {
                val points = recordingState.points
                if (points.size >= 2) {
                    viewModel.applyInputRoutePoints(points)
                    Toast.makeText(
                        context,
                        context.getString(R.string.route_record_finished, points.size),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.route_record_too_short), Toast.LENGTH_SHORT).show()
                    viewModel.cancelRoutePlanning()
                }
                RouteRecordingStore.consumeFinished()
                recordedDrawnCount = 0
                inputMode = RouteInputMode.NONE
            }
            RouteRecordingPhase.IDLE -> Unit
        }
        recordingState.error?.let { error ->
            val message = when (error) {
                RouteRecordingError.PERMISSION_DENIED -> context.getString(R.string.route_record_error_permission)
                RouteRecordingError.LOCATION_UNAVAILABLE -> context.getString(R.string.route_record_error_location)
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            RouteRecordingStore.clearError()
            if (recordingState.phase != RouteRecordingPhase.RECORDING) {
                inputMode = RouteInputMode.NONE
                viewModel.cancelRoutePlanning()
            }
        }
    }

    // 手绘层会拦截所有单指手势，因此绘制期间关闭地图自身的平移/缩放手势；
    // 用户切到“移动地图”时恢复手势，方便把地图挪到下一段要画的位置。
    LaunchedEffect(inputMode, mapController, drawMapMovable) {
        mapController?.setGesturesEnabled(inputMode != RouteInputMode.DRAW || drawMapMovable)
    }
    DisposableEffect(mapController) {
        onDispose { mapController?.setGesturesEnabled(true) }
    }

    BackHandler(enabled = inputMode == RouteInputMode.DRAW) {
        if (drawPoints.isEmpty()) discardDrawMode() else showDiscardDrawDialog = true
    }
    BackHandler(enabled = inputMode == RouteInputMode.RECORD) {
        showDiscardRecordDialog = true
    }

    val submitSearch: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        if (uiState.searchMode == SearchMode.LOCAL) {
            coroutineScope.launch {
                val results = viewModel.performLocalSearch()
                searchResults = results
                showSearchResults = results.isNotEmpty()
                if (results.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.no_matching_local_data), Toast.LENGTH_SHORT).show()
                }
            }
        } else if (searchQuery.isNotBlank()) {
            performPoiSearch(
                context = context,
                mapEngine = uiState.mapEngine,
                keyword = searchQuery,
                isDomestic = isDomestic
            ) { r ->
                searchResults = r
                showSearchResults = r.isNotEmpty()
                if (r.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.no_search_results), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    BackHandler(enabled = isSearchActive || showSearchResults) {
        isSearchActive = false
        showSearchResults = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val density = LocalDensity.current
    val bottomActionHeightDp = with(density) { bottomActionHeightPx.toDp() }

    LaunchedEffect(mapController, uiState.mapType, isActive) {
        if (!isActive) return@LaunchedEffect
        mapController?.setMapType(uiState.mapType)
    }

    val manageDataList = manageDataViewModel.uiState.collectAsState().value.dataList
    var liveMarker by remember { mutableStateOf<AppMapMarker?>(null) }
    LaunchedEffect(
        routePoints,
        mapController,
        manageDataList,
        isActive,
        collectionRoutes,
        uiState.selectedCollectionRouteId,
        uiState.routePlanStage,
        inputMode,
        drawRevision
    ) {
        if (!isActive) return@LaunchedEffect
        val map = mapController ?: return@LaunchedEffect
        map.clear()
        collectionRoutes.firstOrNull { it.route.id == uiState.selectedCollectionRouteId }?.let {
            com.vincenthzr.locationspoofer.ui.components.CollectionRouteMapHelper.draw(map, it.route)
        }
        liveMarker = null
        val locations = manageDataList.map { it.location }
        MapCoverageHelper.drawCoverage(map, locations)

        if (routePoints.size >= 2) {
            map.addPolyline(
                routePoints.map { Pair(it.lat, it.lng) },
                android.graphics.Color.parseColor("#FF388BFD"),
                8f
            )
        }
        routePoints.forEachIndexed { idx, p ->
            val type = when {
                idx == 0 -> MarkerType.GREEN
                idx == routePoints.lastIndex && routePoints.size > 1 -> MarkerType.RED
                else -> MarkerType.DEFAULT
            }
            if (uiState.useRealRoute && uiState.routePlanStage == RoutePlanStage.RUNNING && type == MarkerType.DEFAULT) {
                return@forEachIndexed
            }
            val startBadge = context.getString(R.string.route_start_badge)
            val endBadge = context.getString(R.string.route_end_badge)
            val label = when (type) {
                MarkerType.GREEN -> startBadge
                MarkerType.RED -> endBadge
                else -> "${idx + 1}"
            }
            map.addMarker(
                p.lat,
                p.lng,
                if (type == MarkerType.RED && uiState.useRealRoute && uiState.routePlanStage == RoutePlanStage.RUNNING) endBadge else label,
                type
            )
        }

        // 手绘草稿用橙色实时预览；撤销/清除后由 drawRevision 触发整层重绘
        if (inputMode == RouteInputMode.DRAW && drawPoints.size >= 2) {
            map.addPolyline(
                drawPoints.map { Pair(it.lat, it.lng) },
                DRAW_LINE_COLOR,
                DRAW_LINE_WIDTH
            )
        }

        // 录制中重绘（例如环境图层刷新导致 clear）后，把已录制轨迹补回来
        if (inputMode == RouteInputMode.RECORD) {
            if (recordingState.points.size >= 2) {
                map.addPolyline(
                    recordingState.points.map { Pair(it.lat, it.lng) },
                    RECORD_LINE_COLOR,
                    DRAW_LINE_WIDTH
                )
            }
            recordedDrawnCount = recordingState.points.size
        } else {
            recordedDrawnCount = 0
        }

        if (uiState.isSpoofingActive) {
            val currentLat = uiState.latitudeInput.toDoubleOrNull()
            val currentLng = uiState.longitudeInput.toDoubleOrNull()
            if (currentLat != null && currentLng != null) {
                liveMarker = map.addMarker(
                    currentLat, currentLng,
                    context.getString(R.string.current_location),
                    MarkerType.ORANGE
                )
            }
        }
    }

    // 录制时只追加重叠的一段，避免每秒都 clear 整张地图
    LaunchedEffect(inputMode, recordingState.points.size, mapController) {
        if (inputMode != RouteInputMode.RECORD) {
            recordedDrawnCount = 0
            return@LaunchedEffect
        }
        val map = mapController ?: return@LaunchedEffect
        val points = recordingState.points
        if (points.isEmpty()) {
            recordedDrawnCount = 0
            return@LaunchedEffect
        }
        if (points.size < recordedDrawnCount) recordedDrawnCount = 0
        if (points.size > recordedDrawnCount) {
            val fromIndex = (recordedDrawnCount - 1).coerceAtLeast(0)
            val segment = points.subList(fromIndex, points.size)
            if (segment.size >= 2) {
                map.addPolyline(
                    segment.map { Pair(it.lat, it.lng) },
                    RECORD_LINE_COLOR,
                    DRAW_LINE_WIDTH
                )
            }
            if (recordedDrawnCount == 0) {
                map.animateCamera(points.last().lat, points.last().lng, 17f)
            }
            recordedDrawnCount = points.size
        }
    }

    val lat = uiState.latitudeInput.toDoubleOrNull()
    val lng = uiState.longitudeInput.toDoubleOrNull()
    LaunchedEffect(lat, lng, uiState.isSpoofingActive, uiState.routePlanStage, inputMode) {
        if (uiState.isSpoofingActive && lat != null && lng != null) {
            if (uiState.routePlanStage != RoutePlanStage.RUNNING && inputMode == RouteInputMode.NONE) {
                mapController?.animateCamera(lat, lng)
            }
            if (liveMarker != null) {
                liveMarker?.setPosition(lat, lng)
            } else {
                liveMarker = mapController?.addMarker(
                    lat, lng,
                    context.getString(R.string.current_location),
                    MarkerType.ORANGE
                )
            }
        }
    }

    LaunchedEffect(uiState.routePlanStage, routePoints, isActive, mapController, uiState.selectedCollectionRouteId, inputMode) {
        if (!isActive) return@LaunchedEffect
        if (inputMode == RouteInputMode.NONE &&
            (uiState.routePlanStage == RoutePlanStage.RUNNING ||
                (uiState.routePlanStage == RoutePlanStage.READY && uiState.selectedCollectionRouteId != null)) && routePoints.size >= 2) {
            val padLeft = with(density) { 36.dp.roundToPx() }
            val padTop = with(density) { 80.dp.roundToPx() }
            val padRight = with(density) { 36.dp.roundToPx() }
            val effectiveBottomDp =
                if (bottomActionHeightDp > 0.dp) bottomActionHeightDp + bottomBarHeight + 24.dp else bottomBarHeight + 160.dp
            val padBottom =
                with(density) { effectiveBottomDp.roundToPx() }
            mapController?.fitBounds(
                points = routePoints.map { Pair(it.lat, it.lng) },
                paddingLeft = padLeft,
                paddingTop = padTop,
                paddingRight = padRight,
                paddingBottom = padBottom
            )
        }
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = isSearchActive,
            transitionSpec = {
                androidx.compose.animation.EnterTransition.None togetherWith
                        androidx.compose.animation.ExitTransition.None
            },
            label = "route_search_transition"
        ) content@{ searchActive ->
            val searchModifier = Modifier
                .sharedElement(
                    sharedContentState = rememberSharedContentState(key = "route_search_bar"),
                    animatedVisibilityScope = this@content,
                    boundsTransform = { initialBounds, targetBounds ->
                        val isTopSearchTransition = (initialBounds.top < 350f && targetBounds.top > 350f) ||
                                (initialBounds.top > 350f && targetBounds.top < 350f)
                        if (isTopSearchTransition) {
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        } else {
                            snap()
                        }
                    }
                )
                .onGloballyPositioned { searchBounds = it.boundsInRoot() }

            Box(modifier = Modifier.fillMaxSize()) {
                // 搜索激活且显示联想结果时：点击外部遮罩退出联想列表
                if (searchActive && showSearchResults) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(searchBounds, searchResultBounds) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val up = waitForUpOrCancellation()
                                    if (up != null &&
                                        (up.position - down.position).getDistance() < viewConfiguration.touchSlop &&
                                        !searchBounds.contains(up.position) &&
                                        !searchResultBounds.contains(up.position)
                                    ) {
                                        showSearchResults = false
                                    }
                                }
                            }
                    )
                }

                // 手绘手势层：铺满地图，但声明顺序在面板/按钮之前，因此不会挡住底部操作
                if (inputMode == RouteInputMode.DRAW && !drawMapMovable && !searchActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // 留出底部操作面板的高度，避免在面板空白处落笔时穿透到地图层
                            .padding(bottom = bottomBarHeight + bottomActionHeightDp + 6.dp)
                            .pointerInput(mapController, inputMode) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        strokeStartCount = drawPoints.size
                                        lastDrawScreen = null
                                        appendDrawPoint(offset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        appendDrawPoint(change.position)
                                    },
                                    onDragEnd = {
                                        if (drawPoints.size > strokeStartCount) {
                                            drawStrokeEnds.add(drawPoints.size)
                                        }
                                        lastDrawScreen = null
                                    },
                                    onDragCancel = {
                                        if (drawPoints.size > strokeStartCount) {
                                            drawStrokeEnds.add(drawPoints.size)
                                        }
                                        lastDrawScreen = null
                                    }
                                )
                            }
                    )
                }

                // 中心瞄准十字准心（选点阶段显示，搜索激活时隐藏）
                if ((stage == RoutePlanStage.SELECTING || stage == RoutePlanStage.IDLE) &&
                    inputMode == RouteInputMode.NONE && !searchActive
                ) {
                    Icon(
                        Icons.Rounded.AddLocationAlt,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                            .padding(bottom = 16.dp)
                    )
                }

                if (isRunning && isManual) {
                    Icon(
                        Icons.Rounded.PersonPin,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                            .padding(bottom = 16.dp)
                    )
                }

                // 顶部操作卡片：包含路径点统计与撤销操作（搜索激活时隐藏，避免与顶部搜索栏发生遮挡）
                if (routePoints.isNotEmpty() &&
                    (stage == RoutePlanStage.IDLE || stage == RoutePlanStage.SELECTING) &&
                    inputMode == RouteInputMode.NONE && !searchActive
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 12.dp)
                            .animateContentSize(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(AccentBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${routePoints.size}",
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Text(
                                stringResource(R.string.selected_waypoints_count, routePoints.size),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            VerticalDivider(
                                modifier = Modifier.height(18.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )

                            // 撤销上一个点
                            IconButton(
                                onClick = { viewModel.undoLastRoutePoint() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Undo,
                                    contentDescription = stringResource(R.string.undo),
                                    tint = AccentOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 全部清除 / 重新选点
                            IconButton(
                                onClick = { viewModel.restartSelectingPoints() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteSweep,
                                    contentDescription = stringResource(R.string.reselect),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 右侧悬浮地图控制按钮组
                val animatedBottomPadding by animateDpAsState(
                    targetValue = bottomBarHeight + bottomActionHeightDp + 16.dp,
                    animationSpec = tween(durationMillis = 200),
                    label = "fab_padding"
                )

                if (inputMode == RouteInputMode.NONE) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = animatedBottomPadding)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RouteControlButton(
                                icon = Icons.Rounded.Layers,
                                onClick = { showMapTypeDialog = true }
                            )
                            RouteControlButton(
                                icon = Icons.Rounded.Bookmarks,
                                onClick = { showSavedRoutesDialog = true }
                            )
                            RouteControlButton(
                                icon = Icons.Rounded.MyLocation,
                                onClick = {
                                    viewModel.fetchCurrentLocation(context) { lat, lng ->
                                        mapController?.animateCamera(lat, lng, 16f)
                                    }
                                }
                            )
                        }
                    }
                } else if (inputMode == RouteInputMode.DRAW) {
                    // 手绘时地图手势被关闭，这里用按钮替代双指缩放
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = animatedBottomPadding)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RouteControlButton(
                                icon = Icons.Rounded.Add,
                                onClick = { mapController?.zoomBy(1f) }
                            )
                            RouteControlButton(
                                icon = Icons.Rounded.Remove,
                                onClick = { mapController?.zoomBy(-1f) }
                            )
                        }
                    }
                }

                // 底部多状态操作面板
                RouteBottomPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomBarHeight + 6.dp, start = 14.dp, end = 14.dp)
                        .onGloballyPositioned { coordinates ->
                            bottomActionHeightPx = coordinates.size.height
                        },
                    stage = stage,
                    routePoints = routePoints,
                    uiState = uiState,
                    onConfirmPoint = {
                        val tLat = mapController?.cameraTargetLat
                        val tLng = mapController?.cameraTargetLng
                        if (tLat != null && tLng != null) {
                            viewModel.addRoutePoint(tLat, tLng)
                        } else {
                            val fallbackLat = uiState.latitudeInput.toDoubleOrNull()
                            val fallbackLng = uiState.longitudeInput.toDoubleOrNull()
                            if (fallbackLat != null && fallbackLng != null) {
                                viewModel.addRoutePoint(fallbackLat, fallbackLng)
                            }
                        }
                    },
                    onFinishSelecting = {
                        viewModel.finishSelectingPoints()
                    },
                    onRestartSelecting = {
                        viewModel.restartSelectingPoints()
                    },
                    onSaveRoute = { showSaveRouteDialog = true },
                    onStartPlanning = { viewModel.prepareRouteSimulationOptions { showConfigDialog = true } },
                    onStopRoute = {
                        viewModel.stopRoutePlanning()
                    },
                    onPauseRoute = { viewModel.pauseRouteSimulation() },
                    onResumeRoute = { viewModel.resumeRouteSimulation() },
                    inputMode = inputMode,
                    drawPoints = drawPoints,
                    drawMapMovable = drawMapMovable,
                    recordingState = recordingState,
                    onStartDraw = { requestDrawMode() },
                    onStartRecord = { requestRecordMode() },
                    onFinishDraw = { finishDrawMode() },
                    onUndoDraw = { undoLastDrawStroke() },
                    onClearDraw = { clearDrawDraft() },
                    onToggleDrawMapMovable = { drawMapMovable = !drawMapMovable },
                    onExitInput = {
                        if (inputMode == RouteInputMode.RECORD) {
                            showDiscardRecordDialog = true
                        } else if (drawPoints.isEmpty()) {
                            discardDrawMode()
                        } else {
                            showDiscardDrawDialog = true
                        }
                    },
                    onStopRecord = {
                        try {
                            RouteRecordingService.stop(context)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onDiscardRecord = { showDiscardRecordDialog = true },
                    searchBar = if (inputMode == RouteInputMode.NONE &&
                        (stage == RoutePlanStage.IDLE || stage == RoutePlanStage.SELECTING)
                    ) {
                        { barModifier ->
                            if (!searchActive) {
                                HomeSearchBar(
                                    query = searchQuery,
                                    searchMode = uiState.searchMode,
                                    onSearchModeChange = viewModel::setSearchMode,
                                    onQueryChange = { searchQuery = it },
                                    onSearch = submitSearch,
                                    onFocus = { isSearchActive = true },
                                    modifier = searchModifier.then(barModifier),
                                    focusRequester = searchFocusRequester
                                )
                            } else {
                                Spacer(modifier = barModifier.height(52.dp))
                            }
                        }
                    } else null
                )

                // 搜索激活时：顶部搜索栏与联想结果卡片
                if (searchActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 8.dp)
                    ) {
                        HomeSearchBar(
                            query = searchQuery,
                            searchMode = uiState.searchMode,
                            onSearchModeChange = viewModel::setSearchMode,
                            onQueryChange = { searchQuery = it },
                            onSearch = submitSearch,
                            onFocus = { isSearchActive = true },
                            modifier = searchModifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            focusRequester = searchFocusRequester
                        )

                        AnimatedVisibility(
                            visible = showSearchResults && searchResults.isNotEmpty(),
                            enter = fadeIn(tween(160)) + expandVertically(tween(220)),
                            exit = fadeOut(tween(120)) + shrinkVertically(tween(160))
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .onGloballyPositioned { searchResultBounds = it.boundsInRoot() }
                            ) {
                                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                                    items(searchResults) { poi ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .noRippleClickable {
                                                    mapController?.animateCamera(poi.lat, poi.lng, 17f)
                                                    isSearchActive = false
                                                    showSearchResults = false
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(AccentBlue.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Place,
                                                    null,
                                                    tint = AccentBlue,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    poi.title,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (poi.snippet.isNotBlank()) {
                                                    Text(
                                                        poi.snippet,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                                            alpha = 0.6f
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingInputMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { pendingInputMode = null },
            title = { Text(stringResource(R.string.route_input_replace_title)) },
            text = { Text(stringResource(R.string.route_input_replace_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingInputMode = null
                        if (mode == RouteInputMode.DRAW) beginDrawNow() else beginRecordNow()
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingInputMode = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showDiscardDrawDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDrawDialog = false },
            title = { Text(stringResource(R.string.route_draw_discard_title)) },
            text = { Text(stringResource(R.string.route_draw_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDrawDialog = false
                        discardDrawMode()
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDrawDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showDiscardRecordDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardRecordDialog = false },
            title = { Text(stringResource(R.string.route_record_discard_title)) },
            text = { Text(stringResource(R.string.route_record_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardRecordDialog = false
                        try {
                            if (recordingState.phase == RouteRecordingPhase.RECORDING) {
                                RouteRecordingService.discard(context)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        RouteRecordingStore.reset()
                        inputMode = RouteInputMode.NONE
                        viewModel.cancelRoutePlanning()
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardRecordDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showConfigDialog) {
        RouteConfigDialog(
            uiState = uiState,
            onDismiss = {
                showConfigDialog = false
            },
            onStartRoute = {
                showConfigDialog = false
                viewModel.startRoutePlanning()
            },
            onRunModeChange = viewModel::setRouteRunMode,
            onSpeedChange = viewModel::setRouteSimMode,
            onCustomSpeedChange = viewModel::setCustomSpeedMs,
            onUseRealRouteChange = viewModel::setUseRealRoute,
            onStopAtDestinationChange = viewModel::setStopAtDestination,
            onEnableStepSimulationChange = viewModel::setEnableStepSimulation,
            onStepCadenceChange = viewModel::setStepCadenceSpm,
            onIsAutoCadenceChange = viewModel::setIsAutoCadence,
            onToggleWifi = viewModel::toggleMockWifi,
            onToggleCell = viewModel::toggleMockCell,
            onToggleBluetooth = viewModel::toggleMockBluetooth,
            onToggleJitter = viewModel::toggleEnableJitter,
            onAltitudeChange = viewModel::setAltitude,
            onAltitudeVariationChange = viewModel::setAltitudeVariation,
            onSatelliteCountChange = viewModel::setSatelliteCount
        )
    }

    if (uiState.isFetchingRoute) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AccentBlue)
                    Text(
                        stringResource(R.string.planning_route_trajectory),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showMapTypeDialog) {
        MapTypeDialog(
            currentMapType = uiState.mapType,
            onMapTypeSelected = { viewModel.setMapType(it) },
            currentMapEngine = uiState.mapEngine,
            onMapEngineSelected = { viewModel.setMapEngine(it) },
            onDismiss = { showMapTypeDialog = false }
        )
    }

    if (showSaveRouteDialog) {
        SaveRouteDialog(
            routePoints = routePoints,
            viewModel = viewModel,
            onDismiss = { showSaveRouteDialog = false }
        )
    }

    if (showSavedRoutesDialog) {
        SavedRoutesDialog(
            uiState = uiState,
            viewModel = viewModel,
            mapController = mapController,
            bottomActionHeightDp = bottomActionHeightDp,
            bottomBarHeight = bottomBarHeight,
            onDismiss = { showSavedRoutesDialog = false }
        )
    }
}
