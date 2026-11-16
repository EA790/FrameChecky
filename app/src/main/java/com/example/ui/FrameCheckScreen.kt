package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CommunityVoteOption
import com.example.model.GameEvaluation
import com.example.ui.components.GameDetailBottomSheet
import com.example.ui.components.GameEvaluationCard
import com.example.ui.components.HardwareTelemetryCard
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueDark
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.ElevatedChip
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VerdictUnsupportedColor

private val ScreenSharpShape = RoundedCornerShape(2.dp)

data class AuthUserProfile(
  val uid: String,
  val displayName: String,
  val photoUrl: String?,
)

@Composable
fun FrameCheckScreen(
  uiState: FrameCheckUiState,
  currentUser: AuthUserProfile?,
  isSigningIn: Boolean,
  authErrorMessage: String?,
  onSignInClicked: () -> Unit,
  onSignOutClicked: () -> Unit,
  onSearchQueryChanged: (String) -> Unit,
  onFilterSelected: (VerdictFilter) -> Unit,
  onGameSelected: (GameEvaluation?) -> Unit,
  onRescanHardware: () -> Unit,
  onRunStorageTest: () -> Unit,
  onVoteClicked: (String, CommunityVoteOption) -> Unit,
) {
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = CarbonBlack,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      val isWideLayout = maxWidth >= 680.dp

      when (val content = uiState.content) {
        is FrameCheckContentState.Scanning -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = AccentBlue)
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Scanning CPU, Thermal & RAM telemetry…",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }
        }

        is FrameCheckContentState.Error -> {
          Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = content.message,
              style = MaterialTheme.typography.bodyLarge,
              color = TextPrimary,
            )
          }
        }

        is FrameCheckContentState.Ready -> {
          if (isWideLayout) {
            LazyVerticalGrid(
              columns = GridCells.Fixed(2),
              modifier = Modifier.widthIn(max = 960.dp).fillMaxSize(),
              contentPadding = PaddingValues(16.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              item(span = { GridItemSpan(2) }) {
                HeaderAndSearchSection(
                  content = content,
                  uiState = uiState,
                  currentUser = currentUser,
                  isSigningIn = isSigningIn,
                  authErrorMessage = authErrorMessage,
                  onSignInClicked = onSignInClicked,
                  onSignOutClicked = onSignOutClicked,
                  onSearchQueryChanged = onSearchQueryChanged,
                  onFilterSelected = onFilterSelected,
                  onRescanHardware = onRescanHardware,
                  onRunStorageTest = onRunStorageTest,
                )
              }

              if (content.filteredEvaluations.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                  EmptyGamesState()
                }
              } else {
                items(
                  items = content.filteredEvaluations,
                  key = { it.game.id },
                ) { evaluation ->
                  GameEvaluationCard(
                    evaluation = evaluation,
                    deviceSpecs = content.deviceSpecs,
                    onClick = { onGameSelected(evaluation) },
                  )
                }
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              item(key = "header_section") {
                HeaderAndSearchSection(
                  content = content,
                  uiState = uiState,
                  currentUser = currentUser,
                  isSigningIn = isSigningIn,
                  authErrorMessage = authErrorMessage,
                  onSignInClicked = onSignInClicked,
                  onSignOutClicked = onSignOutClicked,
                  onSearchQueryChanged = onSearchQueryChanged,
                  onFilterSelected = onFilterSelected,
                  onRescanHardware = onRescanHardware,
                  onRunStorageTest = onRunStorageTest,
                )
              }

              if (content.filteredEvaluations.isEmpty()) {
                item(key = "empty_state") {
                  EmptyGamesState()
                }
              } else {
                items(
                  items = content.filteredEvaluations,
                  key = { it.game.id },
                ) { evaluation ->
                  GameEvaluationCard(
                    evaluation = evaluation,
                    deviceSpecs = content.deviceSpecs,
                    onClick = { onGameSelected(evaluation) },
                  )
                }
              }
            }
          }

          uiState.selectedEvaluation?.let { selected ->
            GameDetailBottomSheet(
              evaluation = selected,
              deviceSpecs = content.deviceSpecs,
              isAuthenticated = currentUser != null,
              isSigningIn = isSigningIn,
              communityVerdict = uiState.communityVerdict,
              onSignInClicked = onSignInClicked,
              onVoteClicked = { option -> onVoteClicked(selected.game.id, option) },
              onDismiss = { onGameSelected(null) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HeaderAndSearchSection(
  content: FrameCheckContentState.Ready,
  uiState: FrameCheckUiState,
  currentUser: AuthUserProfile?,
  isSigningIn: Boolean,
  authErrorMessage: String?,
  onSignInClicked: () -> Unit,
  onSignOutClicked: () -> Unit,
  onSearchQueryChanged: (String) -> Unit,
  onFilterSelected: (VerdictFilter) -> Unit,
  onRescanHardware: () -> Unit,
  onRunStorageTest: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    // Subtle Top Header Bar with Google Sign-In or User Avatar + Name
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text(
          text = "FRAMECHECK",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary,
        )
        Text(
          text = "HARDWARE & FPS TELEMETRY",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }

      if (currentUser == null) {
        OutlinedButton(
          onClick = onSignInClicked,
          enabled = !isSigningIn,
          shape = ScreenSharpShape,
          border = BorderStroke(1.dp, SubtleBorder),
          colors =
            ButtonDefaults.outlinedButtonColors(
              containerColor = CharcoalCard,
              contentColor = TextPrimary,
            ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("google_sign_in_button"),
        ) {
          if (isSigningIn) {
            CircularProgressIndicator(
              modifier = Modifier.size(14.dp),
              strokeWidth = 2.dp,
              color = AccentBlue,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Signing in…",
              style = MaterialTheme.typography.labelSmall,
            )
          } else {
            Icon(
              imageVector = Icons.Filled.AccountCircle,
              contentDescription = null,
              tint = AccentBlue,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.sign_in_google),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            )
          }
        }
      } else {
        Surface(
          color = CharcoalCard,
          shape = ScreenSharpShape,
          border = BorderStroke(1.dp, SubtleBorder),
          modifier = Modifier.testTag("user_profile_header"),
        ) {
          Row(
            modifier = Modifier.padding(start = 6.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (!currentUser.photoUrl.isNullOrBlank()) {
              AsyncImage(
                model = currentUser.photoUrl,
                contentDescription = "${currentUser.displayName} avatar",
                contentScale = ContentScale.Crop,
                modifier =
                  Modifier.size(26.dp)
                    .clip(ScreenSharpShape)
                    .background(ElevatedChip),
              )
            } else {
              Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(24.dp),
              )
            }
            Text(
              text = currentUser.displayName,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.widthIn(max = 110.dp),
            )
            IconButton(
              onClick = onSignOutClicked,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("sign_out_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = stringResource(R.string.sign_out),
                tint = TextSecondary,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }
    }

    if (!authErrorMessage.isNullOrBlank()) {
      Text(
        text = authErrorMessage,
        style = MaterialTheme.typography.labelSmall,
        color = VerdictUnsupportedColor,
        modifier = Modifier.padding(bottom = 8.dp),
      )
    }

    HardwareTelemetryCard(
      specs = content.deviceSpecs,
      perfectCount = content.perfectCount,
      playableCount = content.playableCount,
      unsupportedCount = content.unsupportedCount,
      isRescanning = uiState.isRescanning,
      storageBenchmark = uiState.storageBenchmark,
      isStorageTesting = uiState.isStorageTesting,
      storageTestError = uiState.storageTestError,
      onRescan = onRescanHardware,
      onRunStorageTest = onRunStorageTest,
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
      value = uiState.searchQuery,
      onValueChange = onSearchQueryChanged,
      modifier = Modifier.fillMaxWidth().testTag("game_search_input"),
      singleLine = true,
      shape = ScreenSharpShape,
      placeholder = {
        Text(
          text = stringResource(R.string.search_games_hint),
          style = MaterialTheme.typography.bodyLarge,
          color = TextSecondary,
        )
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Filled.Search,
          contentDescription = null,
          tint = AccentBlue,
        )
      },
      trailingIcon = {
        if (uiState.searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { onSearchQueryChanged("") },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_search_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = stringResource(R.string.clear_search),
              tint = TextSecondary,
            )
          }
        }
      },
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedContainerColor = CharcoalCard,
          unfocusedContainerColor = CharcoalCard,
          focusedBorderColor = AccentBlue,
          unfocusedBorderColor = SubtleBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          cursorColor = AccentGreen,
        ),
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      VerdictFilter.entries.forEach { filter ->
        val selected = filter == uiState.activeFilter
        FilterChip(
          selected = selected,
          onClick = { onFilterSelected(filter) },
          shape = ScreenSharpShape,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .testTag("filter_${filter.name.lowercase()}"),
          label = {
            Text(
              text = filter.label,
              style = MaterialTheme.typography.labelLarge,
            )
          },
          colors =
            FilterChipDefaults.filterChipColors(
              containerColor = CharcoalCard,
              labelColor = TextSecondary,
              selectedContainerColor = AccentBlueDark,
              selectedLabelColor = TextPrimary,
            ),
          border =
            FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = selected,
              borderColor = SubtleBorder,
              selectedBorderColor = AccentBlue,
            ),
        )
      }
    }

    Spacer(modifier = Modifier.height(2.dp))
  }
}

@Composable
private fun EmptyGamesState() {
  Surface(
    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp).testTag("empty_games_state"),
    color = CharcoalCard,
    shape = ScreenSharpShape,
    border = BorderStroke(1.dp, SubtleBorder),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        color = ElevatedChip,
        shape = ScreenSharpShape,
      ) {
        Icon(
          imageVector = Icons.Filled.SportsEsports,
          contentDescription = null,
          tint = AccentBlue,
          modifier = Modifier.padding(10.dp).size(26.dp),
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = stringResource(R.string.no_games_found),
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stringResource(R.string.no_games_found_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
      )
    }
  }
}
