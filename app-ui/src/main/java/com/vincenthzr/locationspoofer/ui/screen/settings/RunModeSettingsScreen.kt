package com.vincenthzr.locationspoofer.ui.screen.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincenthzr.locationspoofer.data.model.AppState
import com.vincenthzr.locationspoofer.data.model.RunMode
import com.vincenthzr.locationspoofer.ui.R
import com.vincenthzr.locationspoofer.ui.theme.AccentBlue
import com.vincenthzr.locationspoofer.ui.theme.AccentGreen
import com.vincenthzr.locationspoofer.ui.theme.AccentOrange
import com.vincenthzr.locationspoofer.ui.theme.AppColors
import com.vincenthzr.locationspoofer.ui.theme.noRippleClickable
import com.vincenthzr.locationspoofer.viewmodel.MainViewModel
import com.vincenthzr.locationspoofer.viewmodel.refreshMockLocationPermission
import com.vincenthzr.locationspoofer.viewmodel.setRunMode

@Composable
fun RunModeSettingsScreen(
    viewModel: MainViewModel,
    uiState: AppState,
    isDark: Boolean,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val mockTitle = stringResource(R.string.run_mode_mock_available)
    val mockMissing = stringResource(R.string.run_mode_mock_unavailable)
    val currentModeName = runModeName(uiState.runMode)
    val spoofingActive = uiState.isSpoofingActive

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background(isDark))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF22272E) else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFE5E8EC),
                            shape = CircleShape
                        )
                        .noRippleClickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = if (isDark) Color.White else Color(0xFF1A1D20),
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.run_mode_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = currentModeName,
                        fontSize = 12.sp,
                        color = AppColors.textSecondary(isDark)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.run_mode_subtitle),
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = AppColors.textSecondary(isDark)
                )

                RunModeOptionCard(
                    icon = Icons.Rounded.Security,
                    tint = AccentGreen,
                    title = stringResource(R.string.run_mode_lsposed),
                    description = stringResource(R.string.run_mode_lsposed_desc),
                    selected = uiState.runMode == RunMode.LSPOSED,
                    enabled = !spoofingActive,
                    isDark = isDark,
                    onClick = { viewModel.setRunMode(RunMode.LSPOSED) }
                )

                RunModeOptionCard(
                    icon = Icons.Rounded.PhoneAndroid,
                    tint = AccentBlue,
                    title = stringResource(R.string.run_mode_rootless),
                    description = stringResource(R.string.run_mode_rootless_desc),
                    trailing = if (uiState.isMockLocationAllowed) mockTitle else mockMissing,
                    trailingOk = uiState.isMockLocationAllowed,
                    selected = uiState.runMode == RunMode.ROOTLESS,
                    enabled = !spoofingActive,
                    isDark = isDark,
                    onClick = { viewModel.setRunMode(RunMode.ROOTLESS) }
                )

                RunModeOptionCard(
                    icon = Icons.Rounded.Usb,
                    tint = AccentOrange,
                    title = stringResource(R.string.run_mode_adb),
                    description = stringResource(R.string.run_mode_adb_desc),
                    trailing = if (uiState.isMockLocationAllowed) mockTitle else mockMissing,
                    trailingOk = uiState.isMockLocationAllowed,
                    selected = uiState.runMode == RunMode.ADB,
                    enabled = !spoofingActive,
                    isDark = isDark,
                    onClick = {
                        viewModel.setRunMode(RunMode.ADB)
                        viewModel.refreshMockLocationPermission()
                    }
                )

                if (spoofingActive) {
                    Text(
                        text = stringResource(R.string.run_mode_switch_while_active),
                        fontSize = 12.sp,
                        color = AccentOrange
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.run_mode_limitations_title),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.run_mode_limitations_desc),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = AppColors.textSecondary(isDark)
                        )
                        Text(
                            text = stringResource(R.string.run_mode_data_collection_note),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = AppColors.textSecondary(isDark)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RunModeQuickSwitch(
    viewModel: MainViewModel,
    currentMode: RunMode,
    isDark: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.run_mode_lsposed_unavailable_desc),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = AppColors.textSecondary(isDark),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentMode != RunMode.ROOTLESS) {
                OutlinedButton(
                    onClick = { viewModel.setRunMode(RunMode.ROOTLESS) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    )
                ) {
                    Text(
                        stringResource(R.string.run_mode_switch_to_rootless),
                        fontSize = 12.sp
                    )
                }
            }
            if (currentMode != RunMode.ADB) {
                OutlinedButton(
                    onClick = {
                        viewModel.setRunMode(RunMode.ADB)
                        viewModel.refreshMockLocationPermission()
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    )
                ) {
                    Text(
                        stringResource(R.string.run_mode_switch_to_adb),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RunModeOptionCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    trailing: String? = null,
    trailingOk: Boolean = true
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                tint.copy(alpha = if (isDark) 0.18f else 0.10f)
            } else {
                AppColors.cardBackground(isDark)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (selected) {
                    Modifier.border(1.dp, tint.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            )
            .noRippleClickable {
                if (enabled) onClick()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = if (isDark) 0.24f else 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = AppColors.textSecondary(isDark)
                )
                if (trailing != null) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = trailing,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (trailingOk) AccentGreen else AccentOrange
                    )
                }
            }
            RadioButton(
                selected = selected,
                onClick = { if (enabled) onClick() },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun runModeName(mode: RunMode): String = when (mode) {
    RunMode.LSPOSED -> stringResource(R.string.run_mode_lsposed)
    RunMode.ROOTLESS -> stringResource(R.string.run_mode_rootless)
    RunMode.ADB -> stringResource(R.string.run_mode_adb)
}
