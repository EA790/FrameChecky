package com.example.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CommunityVerdictState
import com.example.model.CommunityVoteOption
import com.example.model.DeviceSpecs
import com.example.model.GameEvaluation
import com.example.model.GameVerdict
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueDark
import com.example.ui.theme.CarbonBlack
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

private val SheetSharpShape = RoundedCornerShape(2.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailBottomSheet(
  evaluation: GameEvaluation,
  deviceSpecs: DeviceSpecs,
  isAuthenticated: Boolean,
  isSigningIn: Boolean,
  communityVerdict: CommunityVerdictState?,
  onSignInClicked: () -> Unit,
  onVoteClicked: (CommunityVoteOption) -> Unit,
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
    shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
    containerColor = CharcoalCard,
    contentColor = TextPrimary,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
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
          shape = SheetSharpShape,
          border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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

      Spacer(modifier = Modifier.height(16.dp))

      ComparisonRow(
        title = "Memory (Total / Free for Games)",
        yours =
          String.format(
            Locale.US,
            "%.1f GB (%.1f GB free)",
            deviceSpecs.totalRamGb,
            deviceSpecs.freeRamForGamesGb,
          ),
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

      Spacer(modifier = Modifier.height(16.dp))

      Surface(
        color = ElevatedChip,
        shape = SheetSharpShape,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
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

      Spacer(modifier = Modifier.height(16.dp))

      // Community Verdict Section (Firestore)
      CommunityVerdictPanel(
        isAuthenticated = isAuthenticated,
        isSigningIn = isSigningIn,
        state = communityVerdict,
        onSignInClicked = onSignInClicked,
        onVoteClicked = onVoteClicked,
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = onDismiss,
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("close_detail_button"),
        shape = SheetSharpShape,
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
private fun CommunityVerdictPanel(
  isAuthenticated: Boolean,
  isSigningIn: Boolean,
  state: CommunityVerdictState?,
  onSignInClicked: () -> Unit,
  onVoteClicked: (CommunityVoteOption) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("community_verdict_section"),
    color = ElevatedChip,
    shape = SheetSharpShape,
    border = BorderStroke(1.dp, SubtleBorder),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.community_verdict_title).uppercase(Locale.US),
          style = MaterialTheme.typography.labelLarge,
          color = TextPrimary,
        )
        if (isAuthenticated && state?.summary != null && state.summary.totalVotes > 0L) {
          Text(
            text = "${state.summary.totalVotes} votes",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (!isAuthenticated) {
        Text(
          text = "Sign in with Google to view live community percentages and cast your vote.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = onSignInClicked,
          enabled = !isSigningIn,
          shape = SheetSharpShape,
          border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.6f)),
          colors =
            ButtonDefaults.outlinedButtonColors(
              containerColor = AccentBlueDark,
              contentColor = TextPrimary,
            ),
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("community_sign_in_button"),
        ) {
          Text(
            text = if (isSigningIn) "Signing in…" else stringResource(R.string.sign_in_google),
            style = MaterialTheme.typography.labelMedium,
          )
        }
      } else {
        when {
          state == null || state.isLoading -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = AccentBlue,
              )
              Text(
                text = "Loading community votes…",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }
          else -> {
            val summary = state.summary
            val total = summary?.totalVotes ?: 0L
            val runsPercent = summary?.runsGreatPercentage ?: 0

            Text(
              text =
                if (total > 0L) "$runsPercent% say it runs"
                else "No community votes yet — cast the first vote",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = if (total > 0L) VerdictPerfectColor else TextSecondary,
              modifier = Modifier.testTag("community_verdict_percentage"),
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Percentage bar
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(8.dp)
                  .background(CarbonBlack)
            ) {
              if (total > 0L) {
                val runsFraction = (runsPercent / 100f).coerceIn(0f, 1f)
                Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                  if (runsFraction > 0f) {
                    Box(
                      modifier =
                        Modifier.weight(runsFraction)
                          .fillMaxHeight()
                          .background(VerdictPerfectColor)
                    )
                  }
                  if (runsFraction < 1f) {
                    Box(
                      modifier =
                        Modifier.weight(1f - runsFraction)
                          .fillMaxHeight()
                          .background(VerdictUnsupportedColor)
                    )
                  }
                }
              }
            }

            if (total > 0L) {
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Runs Great: ${summary?.runsGreatCount ?: 0L}",
                  style = MaterialTheme.typography.labelSmall,
                  color = VerdictPerfectColor,
                )
                Text(
                  text = "Lags: ${summary?.lagsCount ?: 0L}",
                  style = MaterialTheme.typography.labelSmall,
                  color = VerdictUnsupportedColor,
                )
              }
            }

            state.errorMessage?.let { err ->
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = err,
                style = MaterialTheme.typography.labelSmall,
                color = VerdictUnsupportedColor,
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              val votedRuns = state.userVote == CommunityVoteOption.RUNS_GREAT
              val votedLags = state.userVote == CommunityVoteOption.LAGS

              OutlinedButton(
                onClick = { onVoteClicked(CommunityVoteOption.RUNS_GREAT) },
                enabled = !state.isSubmitting,
                shape = SheetSharpShape,
                border =
                  BorderStroke(
                    1.dp,
                    if (votedRuns) VerdictPerfectColor else SubtleBorder,
                  ),
                colors =
                  ButtonDefaults.outlinedButtonColors(
                    containerColor = if (votedRuns) VerdictPerfectContainer else CarbonBlack,
                    contentColor = if (votedRuns) VerdictPerfectColor else TextPrimary,
                  ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("vote_runs_great_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.ThumbUp,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = CommunityVoteOption.RUNS_GREAT.label,
                  style = MaterialTheme.typography.labelMedium,
                )
              }

              OutlinedButton(
                onClick = { onVoteClicked(CommunityVoteOption.LAGS) },
                enabled = !state.isSubmitting,
                shape = SheetSharpShape,
                border =
                  BorderStroke(
                    1.dp,
                    if (votedLags) VerdictUnsupportedColor else SubtleBorder,
                  ),
                colors =
                  ButtonDefaults.outlinedButtonColors(
                    containerColor = if (votedLags) VerdictUnsupportedContainer else CarbonBlack,
                    contentColor = if (votedLags) VerdictUnsupportedColor else TextPrimary,
                  ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("vote_lags_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.ThumbDown,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = CommunityVoteOption.LAGS.label,
                  style = MaterialTheme.typography.labelMedium,
                )
              }
            }
          }
        }
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
          .height(6.dp)
          .background(CarbonBlack)
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth(fillFraction)
            .fillMaxHeight()
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
