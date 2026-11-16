package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DeviceSpecs
import com.example.model.StorageBenchmarkResult
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueDark
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.ElevatedChip
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThermalWarningColor
import com.example.ui.theme.ThermalWarningContainer
import com.example.ui.theme.VerdictPerfectColor
import com.example.ui.theme.VerdictPerfectContainer
import com.example.ui.theme.VerdictPlayableColor
import com.example.ui.theme.VerdictPlayableContainer
import com.example.ui.theme.VerdictUnsupportedColor
import com.example.ui.theme.VerdictUnsupportedContainer
import java.util.Locale

private val SharpShape = RoundedCornerShape(2.dp)

@Composable
fun HardwareTelemetryCard(
  specs: DeviceSpecs,
  perfectCount: Int,
  playableCount: Int,
  unsupportedCount: Int,
  isRescanning: Boolean,
  storageBenchmark: StorageBenchmarkResult?,
  isStorageTesting: Boolean,
  storageTestError: String?,
  onRescan: () -> Unit,
  onRunStorageTest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val rotation by
    animateFloatAsState(
      targetValue = if (isRescanning) 360f else 0f,
      animationSpec = tween(durationMillis = 450),
      label = "rescan_rotation",
    )

  Surface(
    modifier = modifier.fillMaxWidth().testTag("hardware_telemetry_card"),
    shape = SharpShape,
    color = CharcoalCard,
    border = BorderStroke(1.dp, SubtleBorder),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = specs.hardwareName,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false).testTag("hardware_name_text"),
            )
            Surface(
              color = AccentBlueDark,
              shape = SharpShape,
              border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.45f)),
            ) {
              Text(
                text = specs.cpuInfoSourceDetail,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "SoC Score ${specs.compositeScore} • CPU ${specs.cpuScore} • GPU ${specs.gpuScore} • GLES ${specs.glEsVersion}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("soc_score_tile"),
          )
        }

        IconButton(
          onClick = onRescan,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("rescan_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = stringResource(R.string.hardware_rescan),
            tint = AccentBlue,
            modifier = Modifier.rotate(rotation),
          )
        }
      }

      if (specs.isThermalThrottling) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("thermal_throttle_warning"),
          color = ThermalWarningContainer,
          shape = SharpShape,
          border = BorderStroke(1.dp, ThermalWarningColor.copy(alpha = 0.65f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Warning,
              contentDescription = null,
              tint = ThermalWarningColor,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = stringResource(R.string.thermal_throttle_warning),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = ThermalWarningColor,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TelemetryMetricTile(
          icon = Icons.Filled.Memory,
          label = "CPU CORES",
          value = "${specs.cpuCores} Cores",
          subValue =
            specs.maxFreqGhz?.let { String.format(Locale.US, "%.2f GHz max", it) }
              ?: specs.cpuArchitecture,
          accentColor = AccentBlue,
          modifier = Modifier.weight(1f).testTag("cpu_cores_tile"),
        )

        val tempText =
          specs.batteryTempCelsius?.let { String.format(Locale.US, "%.1f°C", it) } ?: "N/A"
        val tempAccent = if (specs.isThermalThrottling) ThermalWarningColor else AccentGreen
        TelemetryMetricTile(
          icon = Icons.Filled.DeviceThermostat,
          label = "THERMAL",
          value = tempText,
          subValue = if (specs.isThermalThrottling) "Throttling (>40°C)" else specs.tempSourceLabel,
          accentColor = tempAccent,
          modifier = Modifier.weight(1f).testTag("thermal_tile"),
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Free RAM for Games with allocation bar
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("total_ram_tile"),
        color = ElevatedChip,
        shape = SharpShape,
        border = BorderStroke(1.dp, SubtleBorder),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Storage,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = stringResource(R.string.free_ram_for_games).uppercase(Locale.US),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
              )
            }
            Text(
              text =
                String.format(
                  Locale.US,
                  "%.1f GB free / %.1f GB total",
                  specs.freeRamForGamesGb,
                  specs.totalRamGb,
                ),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimary,
            )
          }

          Spacer(modifier = Modifier.height(7.dp))

          val freeFraction =
            if (specs.totalRamGb > 0.0) {
              (specs.freeRamForGamesGb / specs.totalRamGb).toFloat().coerceIn(0.05f, 1f)
            } else {
              0.5f
            }
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(6.dp)
                .background(CarbonBlack)
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth(freeFraction)
                  .fillMaxHeight()
                  .background(AccentGreen)
            )
          }

          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text =
                String.format(
                  Locale.US,
                  "Usable for games: %.1f GB",
                  specs.freeRamForGamesGb,
                ),
              style = MaterialTheme.typography.labelSmall,
              color = AccentGreen,
            )
            Text(
              text =
                String.format(
                  Locale.US,
                  "System/Used: %.1f GB",
                  specs.systemReservedRamGb,
                ),
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Storage I/O Speed Test Card
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("storage_speed_tile"),
        color = ElevatedChip,
        shape = SharpShape,
        border = BorderStroke(1.dp, SubtleBorder),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Speed,
                  contentDescription = null,
                  tint = AccentBlue,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = "STORAGE I/O (50MB)",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondary,
                )
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text =
                  when {
                    isStorageTesting -> "Writing & reading 50 MB temp file…"
                    storageTestError != null -> storageTestError
                    storageBenchmark != null ->
                      String.format(
                        Locale.US,
                        "Write %.1f MB/s • Read %.1f MB/s",
                        storageBenchmark.writeSpeedMbPerSec,
                        storageBenchmark.readSpeedMbPerSec,
                      )
                    else -> "Measure internal storage throughput"
                  },
                style = MaterialTheme.typography.labelMedium,
                color = if (storageTestError != null) VerdictUnsupportedColor else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            OutlinedButton(
              onClick = onRunStorageTest,
              enabled = !isStorageTesting,
              shape = SharpShape,
              border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.6f)),
              colors =
                ButtonDefaults.outlinedButtonColors(
                  containerColor = AccentBlueDark,
                  contentColor = TextPrimary,
                ),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("storage_speed_test_button"),
            ) {
              if (isStorageTesting) {
                CircularProgressIndicator(
                  modifier = Modifier.size(14.dp),
                  strokeWidth = 2.dp,
                  color = TextPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Testing",
                  style = MaterialTheme.typography.labelSmall,
                )
              } else {
                Text(
                  text = if (storageBenchmark == null) "50MB Test" else "Re-test",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                )
              }
            }
          }

          if (storageBenchmark != null) {
            Spacer(modifier = Modifier.height(7.dp))
            val readRatio = (storageBenchmark.readSpeedMbPerSec / 1200.0).toFloat().coerceIn(0.06f, 1f)
            val writeRatio = (storageBenchmark.writeSpeedMbPerSec / 1200.0).toFloat().coerceIn(0.06f, 1f)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              MiniThroughputBar(
                label = "WRITE",
                ratio = writeRatio,
                color = AccentBlue,
                modifier = Modifier.weight(1f),
              )
              MiniThroughputBar(
                label = "READ",
                ratio = readRatio,
                color = AccentGreen,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SummaryCountBadge(
          label = "60 FPS: $perfectCount",
          textColor = VerdictPerfectColor,
          containerColor = VerdictPerfectContainer,
          modifier = Modifier.weight(1f),
        )
        SummaryCountBadge(
          label = "30 FPS: $playableCount",
          textColor = VerdictPlayableColor,
          containerColor = VerdictPlayableContainer,
          modifier = Modifier.weight(1f),
        )
        SummaryCountBadge(
          label = "No Run: $unsupportedCount",
          textColor = VerdictUnsupportedColor,
          containerColor = VerdictUnsupportedContainer,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun TelemetryMetricTile(
  icon: ImageVector,
  label: String,
  value: String,
  subValue: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = ElevatedChip,
    shape = SharpShape,
    border = BorderStroke(1.dp, SubtleBorder),
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(14.dp),
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.height(5.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subValue,
        style = MaterialTheme.typography.labelSmall,
        color = accentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun MiniThroughputBar(
  label: String,
  ratio: Float,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = TextSecondary,
    )
    Spacer(modifier = Modifier.height(3.dp))
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(5.dp)
          .background(CarbonBlack)
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth(ratio)
            .fillMaxHeight()
            .background(color)
      )
    }
  }
}

@Composable
private fun SummaryCountBadge(
  label: String,
  textColor: Color,
  containerColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = containerColor,
    shape = SharpShape,
    border = BorderStroke(1.dp, textColor.copy(alpha = 0.4f)),
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
