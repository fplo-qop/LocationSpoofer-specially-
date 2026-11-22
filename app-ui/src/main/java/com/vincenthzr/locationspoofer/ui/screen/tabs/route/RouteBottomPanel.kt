package com.vincenthzr.locationspoofer.ui.screen.tabs.route

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincenthzr.locationspoofer.ui.R
import com.vincenthzr.locationspoofer.data.model.AppState
import com.vincenthzr.locationspoofer.data.model.RoutePlanStage
import com.vincenthzr.locationspoofer.data.model.RoutePoint
import com.vincenthzr.locationspoofer.data.model.RouteRunMode
import com.vincenthzr.locationspoofer.ui.theme.AccentBlue
import com.vincenthzr.locationspoofer.ui.theme.AccentGreen
import com.vincenthzr.locationspoofer.ui.theme.AccentOrange
import com.vincenthzr.locationspoofer.ui.recording.RouteRecordingState
import com.vincenthzr.locationspoofer.utils.RouteMetrics

@Composable
fun RouteBottomPanel(
    modifier: Modifier,
    stage: RoutePlanStage,
    routePoints: List<RoutePoint>,
    uiState: AppState,
    onConfirmPoint: () -> Unit,
    onFinishSelecting: () -> Unit,
    onRestartSelecting: () -> Unit,
    onSaveRoute: () -> Unit,
    onStartPlanning: () -> Unit,
    onStopRoute: () -> Unit,
    onPauseRoute: () -> Unit,
    onResumeRoute: () -> Unit,
    searchBar: (@Composable (Modifier) -> Unit)? = null,
    inputMode: RouteInputMode = RouteInputMode.NONE,
    drawPoints: List<RoutePoint> = emptyList(),
    drawMapMovable: Boolean = false,
    recordingState: RouteRecordingState = RouteRecordingState(),
    onStartDraw: () -> Unit = {},
    onStartRecord: () -> Unit = {},
    onFinishDraw: () -> Unit = {},
    onUndoDraw: () -> Unit = {},
    onClearDraw: () -> Unit = {},
    onToggleDrawMapMovable: () -> Unit = {},
    onExitInput: () -> Unit = {},
    onStopRecord: () -> Unit = {},
    onDiscardRecord: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (inputMode == RouteInputMode.NONE && searchBar != null) {
                searchBar(Modifier.fillMaxWidth())
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (inputMode == RouteInputMode.DRAW) {
                        RouteDrawControls(
                            points = drawPoints,
                            mapMovable = drawMapMovable,
                            onUndo = onUndoDraw,
                            onClear = onClearDraw,
                            onFinish = onFinishDraw,
                            onToggleMapMovable = onToggleDrawMapMovable,
                            onExit = onExitInput
                        )
                    } else if (inputMode == RouteInputMode.RECORD) {
                        RouteRecordControls(
                            state = recordingState,
                            onStop = onStopRecord,
                            onDiscard = onDiscardRecord
                        )
                    } else {
                    // 选点阶段就把当前线走的长度、点数算出来实时显示，不用等开始模拟才知道这条路有多长
                    if (stage == RoutePlanStage.IDLE || stage == RoutePlanStage.SELECTING || stage == RoutePlanStage.READY) {
                        if (routePoints.size >= 2) {
                            RouteLengthSummary(points = routePoints)
                        }
                    }
                    if (stage == RoutePlanStage.IDLE || stage == RoutePlanStage.SELECTING) {
                        RouteInputEntryRow(
                            onStartDraw = onStartDraw,
                            onStartRecord = onStartRecord
                        )
                    }
                    when (stage) {
                        RoutePlanStage.IDLE, RoutePlanStage.SELECTING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 确认添加路点
                                Button(
                                    onClick = onConfirmPoint,
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                                ) {
                                    Icon(
                                        Icons.Rounded.AddLocation,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (routePoints.isEmpty()) stringResource(R.string.add_start_point) else stringResource(R.string.add_nth_point, routePoints.size + 1),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                // 完成规划
                                Button(
                                    onClick = onFinishSelecting,
                                    enabled = routePoints.size >= 2,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentGreen,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        stringResource(R.string.finish_selecting),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        RoutePlanStage.READY -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = onRestartSelecting,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Refresh,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        stringResource(R.string.reselect),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = onSaveRoute,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.BookmarkAdd,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        stringResource(R.string.bookmark),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1
                                    )
                                }

                                Button(
                                    onClick = onStartPlanning,
                                    modifier = Modifier
                                        .weight(1.35f)
                                        .height(50.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                                ) {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        stringResource(R.string.start_simulation),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        RoutePlanStage.RUNNING -> {
                            val paused = uiState.routePaused
                            val returning = uiState.routeReturning
                            // 手动模式本来就以"暂停 + 摇杆接管"的方式运行，暂停/继续由悬浮摇杆自己那套按钮负责，
                            // 这里不再重复放一个点了没反应的暂停键
                            val manualMode = uiState.routeRunMode == RouteRunMode.MANUAL
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    returning -> AccentBlue
                                                    manualMode -> AccentBlue
                                                    paused -> AccentOrange
                                                    else -> AccentGreen
                                                }
                                            )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            returning -> stringResource(R.string.route_returning_status)
                                            manualMode -> stringResource(R.string.joystick_controlling_active)
                                            paused -> stringResource(R.string.route_paused_status)
                                            else -> stringResource(R.string.cruising_speed_format, uiState.routeSimMode.speedMs.toInt())
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 暂停 = 就地冻结（位置不再推进）；继续 = 从冻结处接着沿原路线走。
                                    // 这两者与"停止"不同：停止会拆掉整段路线会话，暂停不会。
                                    if (!manualMode) {
                                        Button(
                                            onClick = if (paused) onResumeRoute else onPauseRoute,
                                            enabled = !returning,
                                            shape = RoundedCornerShape(16.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (paused) AccentGreen else AccentOrange,
                                                contentColor = androidx.compose.ui.graphics.Color.White,
                                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.height(46.dp)
                                        ) {
                                            Icon(
                                                if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                                null,
                                                modifier = Modifier.size(19.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                stringResource(if (paused) R.string.route_resume else R.string.route_pause),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = onStopRoute,
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.height(46.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.StopCircle,
                                            null,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            stringResource(R.string.stop_simulation),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
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

/**
 * 选点 / 准备阶段的路线摘要：总长 + 节点数 + 闭环或往返。
 * 长度按坐标逐段累加现算，加一个点就立刻变，不需要保存或开始模拟。
 */
@Composable
private fun RouteLengthSummary(points: List<RoutePoint>) {
    val latLngs = remember(points) { points.toLatLngs() }
    val meters = remember(latLngs) { RouteMetrics.totalDistance(latLngs) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AccentBlue.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Rounded.Route,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = listOf(
                stringResource(R.string.route_length_total_format, formatRouteLength(meters)),
                stringResource(R.string.route_nodes_count, points.size),
                routeShapeLabel(latLngs)
            ).joinToString("  ·  "),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
