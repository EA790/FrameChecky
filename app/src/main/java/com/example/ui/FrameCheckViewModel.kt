package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.domain.CompatibilityEvaluator
import com.example.hardware.HardwareDetector
import com.example.model.DeviceSpecs
import com.example.model.GameEvaluation
import com.example.model.GameRequirement
import com.example.model.GameVerdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
)

class FrameCheckViewModel(
  private val hardwareDetector: HardwareDetector,
  private val gameRepository: GameRepository,
) : ViewModel() {

  private val deviceSpecsFlow = MutableStateFlow<DeviceSpecs?>(null)
  private val gamesFlow = MutableStateFlow<List<GameRequirement>>(emptyList())
  private val errorMessageFlow = MutableStateFlow<String?>(null)
  private val searchQueryFlow = MutableStateFlow("")
  private val activeFilterFlow = MutableStateFlow(VerdictFilter.ALL)
  private val selectedEvaluationFlow = MutableStateFlow<GameEvaluation?>(null)
  private val isRescanningFlow = MutableStateFlow(false)

  val uiState: StateFlow<FrameCheckUiState> =
    combine(
        deviceSpecsFlow,
        gamesFlow,
        errorMessageFlow,
        searchQueryFlow,
        activeFilterFlow,
        selectedEvaluationFlow,
        isRescanningFlow,
      ) { values ->
        val specs = values[0] as DeviceSpecs?
        @Suppress("UNCHECKED_CAST")
        val games = values[1] as List<GameRequirement>
        val error = values[2] as String?
        val query = values[3] as String
        val filter = values[4] as VerdictFilter
        val selected = values[5] as GameEvaluation?
        val rescanning = values[6] as Boolean

        val content =
          when {
            error != null -> FrameCheckContentState.Error(error)
            specs == null -> FrameCheckContentState.Scanning
            else -> {
              val evaluations =
                games.map { game -> CompatibilityEvaluator.evaluate(specs, game) }
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

        FrameCheckUiState(
          content = content,
          searchQuery = query,
          activeFilter = filter,
          selectedEvaluation = selected,
          isRescanning = rescanning,
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

  fun onSearchQueryChanged(query: String) {
    searchQueryFlow.value = query
  }

  fun onFilterSelected(filter: VerdictFilter) {
    activeFilterFlow.value = filter
  }

  fun onGameSelected(evaluation: GameEvaluation?) {
    selectedEvaluationFlow.value = evaluation
  }

  class Factory(
    private val hardwareDetector: HardwareDetector,
    private val gameRepository: GameRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return FrameCheckViewModel(hardwareDetector, gameRepository) as T
    }
  }
}
