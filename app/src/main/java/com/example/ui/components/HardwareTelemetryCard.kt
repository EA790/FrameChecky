package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DeviceSpecs
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueDark
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.ElevatedChip
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VerdictPerfectColor
import com.example.ui.theme.VerdictPerfectContainer
import com.example.ui.theme.VerdictPlayableColor
import com.example.ui.theme.VerdictPlayableContainer
import com.example.ui.theme.VerdictUnsupportedColor
import com.example.ui.theme.VerdictUnsupportedContainer
import java.util.Locale

@Composable
fun HardwareTelemetryCard(
  specs: DeviceSpecs,
  perfectCount: Int,
  playableCount: Int,
  unsupportedCount: Int,
  isRescanning: Boolean,
  onRescan: () -> Unit,
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
    shape = RoundedCornerShape(18.dp),
    color = CharcoalCard,
    border = BorderStroke(1.dp, SubtleBorder),
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Image(
        painter = painterResource(id = R.drawable.img_hardware_banner_1794804779855),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize().alpha(0.24f),
      )

      Box(
        modifier =
          Modifier.matchParentSize()
            .background(
              Brush.verticalGradient(
                colors =
                  listOf(
                    CharcoalCard.copy(alpha = 0.65f),
                    CharcoalCard.copy(alpha = 0.94f),
                  )
              )
            )
      )

      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
              )
              Surface(
                color = AccentBlueDark,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.45f)),
              ) {
                Text(
                  text = specs.cpuInfoSourceDetail,
                  style = MaterialTheme.typography.labelSmall,
                  color = TextPrimary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = specs.hardwareName,
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("hardware_name_text"),
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

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          TelemetryMetricTile(
            icon = Icons.Filled.Memory,
            label = "CPU CORES",
            value = "${specs.cpuCores} Cores",
            subValue =
              specs.maxFreqGhz?.let { String.format(Locale.US, "%.2f GHz", it) }
                ?: specs.cpuArchitecture,
            accentColor = AccentBlue,
            modifier = Modifier.weight(1f).testTag("cpu_cores_tile"),
          )
          TelemetryMetricTile(
            icon = Icons.Filled.Storage,
            label = "TOTAL RAM",
            value = String.format(Locale.US, "%.1f GB", specs.totalRamGb),
            subValue = String.format(Locale.US, "%.1f GB free", specs.availableRamGb),
            accentColor = AccentGreen,
            modifier = Modifier.weight(1f).testTag("total_ram_tile"),
          )
          TelemetryMetricTile(
            icon = Icons.Filled.Speed,
            label = "SOC SCORE",
            value = "${specs.compositeScore}",
            subValue = "CPU ${specs.cpuScore}",
            accentColor = AccentBlue,
            modifier = Modifier.weight(1f).testTag("soc_score_tile"),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
    modifier = modifier.clip(RoundedCornerShape(12.dp)),
    color = ElevatedChip.copy(alpha = 0.85f),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, SubtleBorder),
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
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
      Spacer(modifier = Modifier.height(6.dp))
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
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, textColor.copy(alpha = 0.35f)),
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
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
