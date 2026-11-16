package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CommunityVoteRepository
import com.example.data.GameRepository
import com.example.domain.CompatibilityEvaluator
import com.example.hardware.HardwareDetector
import com.example.model.CommunityVerdictState
import com.example.model.CommunityVoteOption
import com.example.model.DeviceSpecs
import com.example.model.GameEvaluation
import com.example.model.GameRequirement
import com.example.model.GameVerdict
import com.example.model.StorageBenchmarkResult
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VerdictFilter(val label: String) {
  ALL("All"),
  PERFECT_60("60 FPS"),
  PLAYABLE_30("30 FPS"),
  UNSUPPORTED("Unsupported"),
}

sealed interface FrameCheckContentState {
  data object Scanning : FrameCheckContentState

  data class Ready(
    val deviceSpecs: DeviceSpecs,
    val allEvaluations: List<GameEvaluation>,
    val filteredEvaluations: List<GameEvaluation>,
    val perfectCount: Int,
    val playableCount: Int,
    val unsupportedCount: Int,
  ) : FrameCheckContentState

  data class Error(val message: String) : FrameCheckContentState
}

data class FrameCheckUiState(
  val content: FrameCheckContentState = FrameCheckContentState.Scanning,
  val searchQuery: String = "",
  val activeFilter: VerdictFilter = VerdictFilter.ALL,
  val selectedEvaluation: GameEvaluation? = null,
  val isRescanning: Boolean = false,
  val storageBenchmark: StorageBenchmarkResult? = null,
  val isStorageTesting: Boolean = false,
  val storageTestError: String? = null,
  val communityVerdict: CommunityVerdictState? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FrameCheckViewModel(
  private val hardwareDetector: HardwareDetector,
  private val gameRepository: GameRepository,
  private val communityVoteRepository: CommunityVoteRepository? = null,
) : ViewModel() {

  private val deviceSpecsFlow = MutableStateFlow<DeviceSpecs?>(null)
  private val gamesFlow = MutableStateFlow<List<GameRequirement>>(emptyList())
  private val errorMessageFlow = MutableStateFlow<String?>(null)
  private val searchQueryFlow = MutableStateFlow("")
  private val activeFilterFlow = MutableStateFlow(VerdictFilter.ALL)
  private val selectedEvaluationFlow = MutableStateFlow<GameEvaluation?>(null)
  private val isRescanningFlow = MutableStateFlow(false)
  private val storageBenchmarkFlow = MutableStateFlow<StorageBenchmarkResult?>(null)
  private val isStorageTestingFlow = MutableStateFlow(false)
  private val storageTestErrorFlow = MutableStateFlow<String?>(null)
  private val authenticatedUidFlow = MutableStateFlow<String?>(null)
  private val isSubmittingVoteFlow = MutableStateFlow(false)
  private val voteErrorFlow = MutableStateFlow<String?>(null)

  // Strictly auth-gated Firestore stream: never queries Firestore while authenticatedUidFlow == null
  private val communityVerdictFlow: StateFlow<CommunityVerdictState?> =
    combine(authenticatedUidFlow, selectedEvaluationFlow) { uid, selected ->
        uid to selected?.game?.id
      }
      .flatMapLatest { (uid, gameId) ->
        if (uid == null || gameId == null || communityVoteRepository == null) {
          flowOf(null)
        } else {
          combine(
              communityVoteRepository.observeGameVoteSummary(gameId),
              communityVoteRepository.observeUserVote(gameId),
              isSubmittingVoteFlow,
              voteErrorFlow,
            ) { summary, userVote, submitting, voteError ->
              CommunityVerdictState(
                gameId = gameId,
                summary = summary,
                userVote = CommunityVoteOption.fromWire(userVote?.verdict),
                isLoading = false,
                isSubmitting = submitting,
                errorMessage = voteError,
              )
            }
            .onStart {
              emit(CommunityVerdictState(gameId = gameId, isLoading = true))
            }
            .catch { error ->
              val firestoreError =
                generateSequence(error) { it.cause }
                  .filterIsInstance<FirebaseFirestoreException>()
                  .firstOrNull()
              if (firestoreError == null && error is CancellationException) throw error
              val rootError = firestoreError ?: error
              Log.w(TAG, "Community vote observation failed for $gameId", rootError)
              emit(
                CommunityVerdictState(
                  gameId = gameId,
                  isLoading = false,
                  errorMessage = rootError.message ?: "Unable to load community votes",
                )
              )
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
      )

  private val baseContentFlow =
    combine(
      deviceSpecsFlow,
      gamesFlow,
      errorMessageFlow,
      searchQueryFlow,
      activeFilterFlow,
    ) { specs, games, error, query, filter ->
      when {
        error != null -> FrameCheckContentState.Error(error)
        specs == null -> FrameCheckContentState.Scanning
        else -> {
          val evaluations = games.map { game -> CompatibilityEvaluator.evaluate(specs, game) }
          val trimmedQuery = query.trim()
          val filtered =
            evaluations.filter { eval ->
              val matchesQuery =
                trimmedQuery.isEmpty() ||
                  eval.game.name.contains(trimmedQuery, ignoreCase = true) ||
                  eval.game.genre.contains(trimmedQuery, ignoreCase = true)
              val matchesFilter =
                when (filter) {
                  VerdictFilter.ALL -> true
                  VerdictFilter.PERFECT_60 -> eval.verdict == GameVerdict.RUNS_PERFECTLY
                  VerdictFilter.PLAYABLE_30 -> eval.verdict == GameVerdict.PLAYABLE
                  VerdictFilter.UNSUPPORTED -> eval.verdict == GameVerdict.NOT_SUPPORTED
                }
              matchesQuery && matchesFilter
            }

          FrameCheckContentState.Ready(
            deviceSpecs = specs,
            allEvaluations = evaluations,
            filteredEvaluations = filtered,
            perfectCount = evaluations.count { it.verdict == GameVerdict.RUNS_PERFECTLY },
            playableCount = evaluations.count { it.verdict == GameVerdict.PLAYABLE },
            unsupportedCount = evaluations.count { it.verdict == GameVerdict.NOT_SUPPORTED },
          )
        }
      }
    }

  val uiState: StateFlow<FrameCheckUiState> =
    combine(
        baseContentFlow,
        searchQueryFlow,
        activeFilterFlow,
        selectedEvaluationFlow,
        isRescanningFlow,
        storageBenchmarkFlow,
        isStorageTestingFlow,
        storageTestErrorFlow,
        communityVerdictFlow,
      ) { values ->
        @Suppress("UNCHECKED_CAST")
        FrameCheckUiState(
          content = values[0] as FrameCheckContentState,
          searchQuery = values[1] as String,
          activeFilter = values[2] as VerdictFilter,
          selectedEvaluation = values[3] as GameEvaluation?,
          isRescanning = values[4] as Boolean,
          storageBenchmark = values[5] as StorageBenchmarkResult?,
          isStorageTesting = values[6] as Boolean,
          storageTestError = values[7] as String?,
          communityVerdict = values[8] as CommunityVerdictState?,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FrameCheckUiState(),
      )

  init {
    runInitialScan()
  }

  private fun runInitialScan() {
    viewModelScope.launch {
      try {
        errorMessageFlow.value = null
        val specs = withContext(Dispatchers.IO) { hardwareDetector.detect() }
        val games = gameRepository.loadGames()
        deviceSpecsFlow.value = specs
        gamesFlow.value = games
      } catch (e: Exception) {
        errorMessageFlow.value = e.message ?: "Unable to read hardware telemetry."
      }
    }
  }

  fun setAuthenticatedUserId(uid: String?) {
    authenticatedUidFlow.value = uid
    voteErrorFlow.value = null
  }

  fun rescanHardware() {
    if (isRescanningFlow.value) return
    viewModelScope.launch {
      isRescanningFlow.value = true
      try {
        val specs = withContext(Dispatchers.IO) { hardwareDetector.detect() }
        deviceSpecsFlow.value = specs
        if (gamesFlow.value.isEmpty()) {
          gamesFlow.value = gameRepository.loadGames()
        }
      } catch (e: Exception) {
        errorMessageFlow.value = e.message ?: "Hardware re-scan failed."
      } finally {
        isRescanningFlow.value = false
      }
    }
  }

  fun runStorageSpeedTest() {
    if (isStorageTestingFlow.value) return
    viewModelScope.launch {
      isStorageTestingFlow.value = true
      storageTestErrorFlow.value = null
      try {
        val result = hardwareDetector.runStorageSpeedTest(sizeMb = 50)
        storageBenchmarkFlow.value = result
      } catch (e: Exception) {
        storageTestErrorFlow.value = e.message ?: "Storage I/O benchmark failed"
      } finally {
        isStorageTestingFlow.value = false
      }
    }
  }

  fun submitCommunityVote(gameId: String, option: CommunityVoteOption) {
    val repo = communityVoteRepository ?: return
    val specs = deviceSpecsFlow.value ?: return
    if (authenticatedUidFlow.value == null || isSubmittingVoteFlow.value) return

    viewModelScope.launch {
      isSubmittingVoteFlow.value = true
      voteErrorFlow.value = null
      try {
        repo.submitVote(gameId = gameId, option = option, deviceSoc = specs.hardwareName)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        voteErrorFlow.value = e.localizedMessage ?: "Failed to submit community vote"
      } finally {
        isSubmittingVoteFlow.value = false
      }
    }
  }

  fun onSearchQueryChanged(query: String) {
    searchQueryFlow.value = query
  }

  fun onFilterSelected(filter: VerdictFilter) {
    activeFilterFlow.value = filter
  }

  fun onGameSelected(evaluation: GameEvaluation?) {
    voteErrorFlow.value = null
    selectedEvaluationFlow.value = evaluation
  }

  class Factory(
    private val hardwareDetector: HardwareDetector,
    private val gameRepository: GameRepository,
    private val communityVoteRepository: CommunityVoteRepository? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return FrameCheckViewModel(hardwareDetector, gameRepository, communityVoteRepository) as T
    }
  }

  private companion object {
    const val TAG = "FrameCheckVM"
  }
}
