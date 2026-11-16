package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DeviceSpecs
import com.example.model.GameEvaluation
import com.example.model.GameVerdict
import com.example.ui.theme.AccentBlue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailBottomSheet(
  evaluation: GameEvaluation,
  deviceSpecs: DeviceSpecs,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val game = evaluation.game

  val (statusColor, statusContainer) =
    when (evaluation.verdict) {
      GameVerdict.RUNS_PERFECTLY -> VerdictPerfectColor to VerdictPerfectContainer
      GameVerdict.PLAYABLE -> VerdictPlayableColor to VerdictPlayableContainer
      GameVerdict.NOT_SUPPORTED -> VerdictUnsupportedColor to VerdictUnsupportedContainer
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = CharcoalCard,
    contentColor = TextPrimary,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text(
            text = game.name,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${game.genre} • ${game.storageGb} GB Install",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
        Surface(
          color = statusContainer,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.End,
          ) {
            Text(
              text = "${evaluation.estimatedFps} FPS",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = statusColor,
            )
            Text(
              text = evaluation.verdict.label,
              style = MaterialTheme.typography.labelSmall,
              color = statusColor,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      ComparisonRow(
        title = "Memory (RAM)",
        yours = String.format(Locale.US, "%.1f GB", deviceSpecs.totalRamGb),
        minimum = String.format(Locale.US, "%.1f GB", game.minRamGb),
        recommended = String.format(Locale.US, "%.1f GB", game.recommendedRamGb),
        ratio = evaluation.ramHeadroomRatio,
        accentColor = statusColor,
      )
      Spacer(modifier = Modifier.height(12.dp))
      ComparisonRow(
        title = "CPU Benchmark",
        yours = "${deviceSpecs.cpuScore} pts",
        minimum = "${game.minCpuScore} pts",
        recommended = "${game.recommendedCpuScore} pts",
        ratio = evaluation.cpuHeadroomRatio,
        accentColor = statusColor,
      )
      Spacer(modifier = Modifier.height(12.dp))
      ComparisonRow(
        title = "GPU Benchmark",
        yours = "${deviceSpecs.gpuScore} pts",
        minimum = "${game.minGpuScore} pts",
        recommended = "${game.recommendedGpuScore} pts",
        ratio = evaluation.gpuHeadroomRatio,
        accentColor = statusColor,
      )

      Spacer(modifier = Modifier.height(18.dp))

      Surface(
        color = ElevatedChip,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "${evaluation.bottleneck.label} • ${evaluation.recommendedPreset}",
            style = MaterialTheme.typography.labelLarge,
            color = statusColor,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = evaluation.summaryTip,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = game.engineNote,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      Button(
        onClick = onDismiss,
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("close_detail_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = TextPrimary),
      ) {
        Text(
          text = "Done",
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

@Composable
private fun ComparisonRow(
  title: String,
  yours: String,
  minimum: String,
  recommended: String,
  ratio: Float,
  accentColor: Color,
) {
  val fillFraction = (ratio / 1.15f).coerceIn(0.06f, 1f)
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = TextPrimary,
      )
      Text(
        text = "You: $yours",
        style = MaterialTheme.typography.labelMedium,
        color = accentColor,
      )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(7.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(ElevatedChip)
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth(fillFraction)
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(accentColor)
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Min: $minimum",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
      )
      Text(
        text = "Rec: $recommended",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
      )
    }
  }
}
