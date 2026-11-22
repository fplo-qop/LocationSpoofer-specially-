package com.vincenthzr.locationspoofer.ui.screen

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vincenthzr.locationspoofer.data.model.AppState
import com.vincenthzr.locationspoofer.data.model.RunMode
import com.vincenthzr.locationspoofer.ui.R
import com.vincenthzr.locationspoofer.ui.theme.AccentBlue
import com.vincenthzr.locationspoofer.ui.theme.AccentGreen
import com.vincenthzr.locationspoofer.ui.theme.AccentOrange
import com.vincenthzr.locationspoofer.ui.theme.AppColors
import com.vincenthzr.locationspoofer.utils.MockLocationPermission
import com.vincenthzr.locationspoofer.viewmodel.MainViewModel
import com.vincenthzr.locationspoofer.viewmodel.refreshMockLocationPermission
import com.vincenthzr.locationspoofer.viewmodel.setRunMode

/**
 * 免 Root / ADB 模式的授权引导页。
 *
 * 两个模式注入机制相同，区别只在“android:mock_location”的来源：
 * 免 Root 由用户在开发者选项里选择本应用，ADB 由电脑执行 appops 命令。
 */
@Composable
fun MockLocationSetupScreen(
    viewModel: MainViewModel,
    uiState: AppState,
    isDark: Boolean
) {
    val context = LocalContext.current
    val isAdbMode = uiState.runMode == RunMode.ADB
    val modeTitle = if (isAdbMode) {
        stringResource(R.string.mock_setup_title_adb)
    } else {
        stringResource(R.string.mock_setup_title_rootless)
    }
    var fineLocationGranted by remember {
        mutableStateOf(MockLocationPermission.hasFineLocation(context))
    }
    var systemLocationEnabled by remember {
        mutableStateOf(MockLocationPermission.isSystemLocationEnabled(context))
    }
    var lastMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        fineLocationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            MockLocationPermission.hasFineLocation(context)
        systemLocationEnabled = MockLocationPermission.isSystemLocationEnabled(context)
        viewModel.refreshMockLocationPermission()
        if (fineLocationGranted) {
            lastMessage = context.getString(R.string.mock_setup_permission_ok)
        }
    }

    val commands = remember(context.packageName) {
        buildString {
            append("adb shell appops set ")
            append(context.packageName)
            append(" android:mock_location allow\n")
            append("adb shell dumpsys deviceidle whitelist +")
            append(context.packageName)
        }
    }

    fun recheck() {
        fineLocationGranted = MockLocationPermission.hasFineLocation(context)
        systemLocationEnabled = MockLocationPermission.isSystemLocationEnabled(context)
        viewModel.refreshMockLocationPermission()
        val allowed = MockLocationPermission.isMockLocationAllowed(context)
        lastMessage = if (allowed) {
            context.getString(R.string.mock_setup_mock_ok)
        } else {
            context.getString(R.string.mock_setup_mock_missing)
        }
    }

    // 用户从开发者选项/ADB 授权页返回时自动刷新，无需手动点“重新检测”。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fineLocationGranted = MockLocationPermission.hasFineLocation(context)
                systemLocationEnabled = MockLocationPermission.isSystemLocationEnabled(context)
                viewModel.refreshMockLocationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background(isDark))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAdbMode) AccentOrange.copy(alpha = 0.15f)
                            else AccentBlue.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAdbMode) Icons.Rounded.Usb else Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = if (isAdbMode) AccentOrange else AccentBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = modeTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isAdbMode) {
                            stringResource(R.string.run_mode_adb_desc)
                        } else {
                            stringResource(R.string.run_mode_rootless_desc)
                        },
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = AppColors.textSecondary(isDark)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.cardBackground(isDark)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    StatusLine(
                        ok = uiState.isMockLocationAllowed,
                        text = if (uiState.isMockLocationAllowed) {
                            stringResource(R.string.mock_setup_mock_ok)
                        } else {
                            stringResource(R.string.mock_setup_mock_missing)
                        },
                        isDark = isDark
                    )
                    StatusLine(
                        ok = fineLocationGranted,
                        text = if (fineLocationGranted) {
                            stringResource(R.string.mock_setup_permission_ok)
                        } else {
                            stringResource(R.string.mock_setup_permission_missing)
                        },
                        isDark = isDark
                    )
                    if (!fineLocationGranted) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.mock_setup_grant_location))
                        }
                    }
                    StatusLine(
                        ok = systemLocationEnabled,
                        text = if (systemLocationEnabled) {
                            stringResource(R.string.mock_setup_location_service_ok)
                        } else {
                            stringResource(R.string.mock_setup_location_service_missing)
                        },
                        isDark = isDark
                    )
                    if (!systemLocationEnabled) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Throwable) {
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.mock_setup_open_location_settings))
                        }
                    }
                }
            }

            if (!isAdbMode) {
                InstructionCard(
                    title = stringResource(R.string.mock_setup_rootless_title),
                    lines = listOf(
                        stringResource(R.string.mock_setup_rootless_step2),
                        stringResource(R.string.mock_setup_rootless_step3)
                    ),
                    footer = stringResource(R.string.mock_setup_rootless_hint),
                    isDark = isDark
                )
                OutlinedButton(
                    onClick = { openMockLocationSettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.open_developer_options))
                }
            } else {
                InstructionCard(
                    title = stringResource(R.string.mock_setup_adb_title),
                    lines = listOf(
                        stringResource(R.string.mock_setup_adb_step1),
                        stringResource(R.string.mock_setup_adb_step2)
                    ),
                    footer = stringResource(R.string.mock_setup_adb_hint),
                    isDark = isDark
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF10151C) else Color(0xFFF0F3F7)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = commands,
                        modifier = Modifier.padding(13.dp),
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as? ClipboardManager
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText("LocationSpoofer ADB commands", commands)
                        )
                        Toast.makeText(
                            context,
                            context.getString(R.string.adb_commands_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(stringResource(R.string.copy_adb_commands))
                }
            }

            Button(
                onClick = { recheck() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.mock_setup_recheck))
            }

            lastMessage?.let { message ->
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = AppColors.textSecondary(isDark)
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.cardBackground(isDark)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = stringResource(R.string.run_mode_limitations_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.run_mode_limitations_desc),
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        color = AppColors.textSecondary(isDark)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.setRunMode(if (isAdbMode) RunMode.ROOTLESS else RunMode.ADB)
                        viewModel.refreshMockLocationPermission()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (isAdbMode) {
                            stringResource(R.string.mock_setup_switch_rootless)
                        } else {
                            stringResource(R.string.mock_setup_switch_adb)
                        },
                        fontSize = 12.sp
                    )
                }
                TextButton(
                    onClick = { viewModel.setRunMode(RunMode.LSPOSED) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(R.string.mock_setup_switch_lsposed),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StatusLine(ok: Boolean, text: String, isDark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = if (ok) AccentGreen else AccentOrange,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.5.sp,
            color = if (ok) MaterialTheme.colorScheme.onSurface else AppColors.textSecondary(isDark)
        )
    }
}

@Composable
private fun InstructionCard(
    title: String,
    lines: List<String>,
    footer: String,
    isDark: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = AppColors.cardBackground(isDark)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            lines.forEach { line ->
                Row {
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = AccentBlue
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = line,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = AppColors.textSecondary(isDark)
                    )
                }
            }
            Text(
                text = footer,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = AppColors.textSecondary(isDark).copy(alpha = 0.75f)
            )
        }
    }
}

private fun openMockLocationSettings(context: Context) {
    val intents = listOf(
        Intent("android.settings.MOCK_LOCATION_SETTINGS"),
        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
    )
    for (intent in intents) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(intent)
                return
            } catch (_: Throwable) {
            }
        }
    }
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (_: Throwable) {
    }
}
