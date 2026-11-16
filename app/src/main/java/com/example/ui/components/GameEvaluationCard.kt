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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DeviceSpecs
import com.example.model.GameEvaluation
import com.example.model.GameVerdict
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
fun GameEvaluationCard(
  evaluation: GameEvaluation,
  deviceSpecs: DeviceSpecs,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val game = evaluation.game
  val (accentColor, badgeContainer, badgeIcon) =
    when (evaluation.verdict) {
      GameVerdict.RUNS_PERFECTLY ->
        Triple(VerdictPerfectColor, VerdictPerfectContainer, Icons.Filled.CheckCircle)
      GameVerdict.PLAYABLE ->
        Triple(VerdictPlayableColor, VerdictPlayableContainer, Icons.Filled.Speed)
      GameVerdict.NOT_SUPPORTED ->
        Triple(VerdictUnsupportedColor, VerdictUnsupportedContainer, Icons.Filled.Block)
    }

  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth().testTag("game_card_${game.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CharcoalCard),
    border = BorderStroke(1.dp, SubtleBorder),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
          Text(
            text = game.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              color = ElevatedChip,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = game.genre,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
              )
            }
            Text(
              text = "${game.storageGb} GB",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
        }

        Surface(
          color = badgeContainer,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
          modifier = Modifier.testTag("verdict_badge_${game.id}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = badgeIcon,
              contentDescription = evaluation.verdict.label,
              tint = accentColor,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = evaluation.verdict.label,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = accentColor,
              maxLines = 1,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        HeadroomMiniBar(
          label = "RAM",
          specText =
            String.format(
              Locale.US,
              "Min %.0fG • Rec %.0fG",
              game.minRamGb,
              game.recommendedRamGb,
            ),
          ratio = evaluation.ramHeadroomRatio,
          accentColor = accentColor,
          modifier = Modifier.weight(1f),
        )
        HeadroomMiniBar(
          label = "CPU / GPU",
          specText = "Req ${game.minCpuScore} • Rec ${game.recommendedCpuScore}",
          ratio = minOf(evaluation.cpuHeadroomRatio, evaluation.gpuHeadroomRatio),
          accentColor = accentColor,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text =
            if (evaluation.verdict == GameVerdict.NOT_SUPPORTED) {
              "Est. ~${evaluation.estimatedFps} FPS • ${evaluation.bottleneck.label}"
            } else {
              "Preset: ${evaluation.recommendedPreset} • Device RAM ${
                String.format(Locale.US, "%.1f GB", deviceSpecs.totalRamGb)
              }"
            },
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = "Inspect benchmark details",
          tint = TextSecondary,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun HeadroomMiniBar(
  label: String,
  specText: String,
  ratio: Float,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  val fillFraction = (ratio / 1.15f).coerceIn(0.06f, 1f)
  Column(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = TextPrimary,
      )
      Text(
        text = specText,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.height(5.dp))
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(ElevatedChip)
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth(fillFraction)
            .fillMaxHeight()
            .clip(RoundedCornerShape(3.dp))
            .background(accentColor)
      )
    }
  }
}
