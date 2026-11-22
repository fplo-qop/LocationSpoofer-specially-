package com.vincenthzr.locationspoofer.ui.screen.tabs.route

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincenthzr.locationspoofer.data.model.RoutePoint
import com.vincenthzr.locationspoofer.ui.R
import com.vincenthzr.locationspoofer.ui.recording.RouteRecordingPhase
import com.vincenthzr.locationspoofer.ui.recording.RouteRecordingState
import com.vincenthzr.locationspoofer.ui.theme.AccentBlue
import com.vincenthzr.locationspoofer.ui.theme.AccentGreen
import com.vincenthzr.locationspoofer.ui.theme.AccentOrange
import kotlinx.coroutines.delay
import java.util.Locale

/** 路线页的输入方式：默认点选 / 手绘 / 实际录制 */
enum class RouteInputMode {
    NONE,
    DRAW,
    RECORD
}

/** 默认选点阶段的两个快捷入口 */
@Composable
fun RouteInputEntryRow(
    onStartDraw: () -> Unit,
    onStartRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onStartDraw,
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                Icons.Rounded.Gesture,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.route_draw_entry),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        OutlinedButton(
            onClick = onStartRecord,
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                Icons.Rounded.FiberManualRecord,
                contentDescription = null,
                tint = AccentOrange,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.route_record_entry),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** 手绘进行中的底部操作条 */
@Composable
fun RouteDrawControls(
    points: List<RoutePoint>,
    mapMovable: Boolean,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onFinish: () -> Unit,
    onToggleMapMovable: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val count = points.size
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Gesture,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.route_draw_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.route_draw_points_count, count),
                    fontSize = 12.5.sp,
                    color = AccentBlue,
                    fontWeight = FontWeight.Medium
                )
            }
            TextButton(
                onClick = onToggleMapMovable,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(
                    if (mapMovable) Icons.Rounded.Gesture else Icons.Rounded.OpenWith,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    stringResource(
                        if (mapMovable) R.string.route_draw_resume_drawing
                        else R.string.route_draw_move_map
                    ),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentBlue,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onExit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.route_input_exit),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            stringResource(R.string.route_draw_hint),
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onUndo,
                enabled = count > 0,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Undo,
                    null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.route_draw_undo),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            OutlinedButton(
                onClick = onClear,
                enabled = count > 0,
                modifier = Modifier
                    .weight(0.8f)
                    .height(46.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Rounded.DeleteSweep,
                    null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.route_draw_clear),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Button(
                onClick = onFinish,
                enabled = count >= 2,
                modifier = Modifier
                    .weight(1.2f)
                    .height(46.dp),
                contentPadding = PaddingValues(horizontal = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Icon(
                    Icons.Rounded.Route,
                    null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.route_draw_finish),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/** 实际路线录制中的底部操作条 */
@Composable
fun RouteRecordControls(
    state: RouteRecordingState,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowElapsed by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.phase, state.startedAtElapsedMs) {
        if (state.phase == RouteRecordingPhase.RECORDING) {
            while (true) {
                nowElapsed = SystemClock.elapsedRealtime()
                delay(500)
            }
        }
    }

    val elapsedMs = if (state.phase == RouteRecordingPhase.RECORDING) {
        (nowElapsed - state.startedAtElapsedMs).coerceAtLeast(0L)
    } else {
        state.elapsedMs
    }
    val elapsedText = remember(elapsedMs / 1000) { formatElapsed(elapsedMs) }
    val points = state.points.size

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(AccentOrange.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FiberManualRecord,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.route_record_status),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(
                        R.string.route_record_stats,
                        elapsedText,
                        points,
                        formatRouteLength(state.distanceMeters)
                    ),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentOrange
                )
            }
        }

        Text(
            stringResource(R.string.route_record_hint),
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    stringResource(R.string.route_record_discard),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(
                onClick = onStop,
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                Icon(
                    Icons.Rounded.StopCircle,
                    null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    stringResource(R.string.route_record_stop),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatElapsed(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
