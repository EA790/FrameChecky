package com.example.domain

import com.example.model.BottleneckType
import com.example.model.DeviceSpecs
import com.example.model.GameEvaluation
import com.example.model.GameRequirement
import com.example.model.GameVerdict
import kotlin.math.min
import kotlin.math.roundToInt

object CompatibilityEvaluator {

  fun evaluate(device: DeviceSpecs, game: GameRequirement): GameEvaluation {
    val meetsMinRam = device.totalRamGb >= game.minRamGb
    val meetsMinCpu = device.cpuScore >= game.minCpuScore
    val meetsMinGpu = device.gpuScore >= game.minGpuScore

    val meetsRecRam = device.totalRamGb >= game.recommendedRamGb
    val meetsRecCpu = device.cpuScore >= game.recommendedCpuScore
    val meetsRecGpu = device.gpuScore >= game.recommendedGpuScore

    val ramHeadroom = (device.totalRamGb / game.recommendedRamGb).toFloat().coerceIn(0f, 1.35f)
    val cpuHeadroom = (device.cpuScore.toFloat() / game.recommendedCpuScore.toFloat()).coerceIn(0f, 1.35f)
    val gpuHeadroom = (device.gpuScore.toFloat() / game.recommendedGpuScore.toFloat()).coerceIn(0f, 1.35f)

    val verdict: GameVerdict
    val estimatedFps: Int
    val recommendedPreset: String
    val bottleneck: BottleneckType
    val summaryTip: String

    if (!meetsMinRam || !meetsMinCpu || !meetsMinGpu) {
      verdict = GameVerdict.NOT_SUPPORTED
      val minRatio = min(
        (device.totalRamGb / game.minRamGb).toFloat(),
        min(
          device.cpuScore.toFloat() / game.minCpuScore.toFloat(),
          device.gpuScore.toFloat() / game.minGpuScore.toFloat(),
        ),
      ).coerceIn(0.2f, 0.99f)
      estimatedFps = (minRatio * 24f).roundToInt().coerceIn(8, 24)
      recommendedPreset = "Unsupported"
      bottleneck = when {
        !meetsMinRam && (!meetsMinCpu || !meetsMinGpu) -> BottleneckType.SYSTEM_LIMITED
        !meetsMinRam -> BottleneckType.RAM_LIMITED
        !meetsMinGpu -> BottleneckType.GPU_LIMITED
        else -> BottleneckType.CPU_LIMITED
      }
      summaryTip = when (bottleneck) {
        BottleneckType.RAM_LIMITED ->
          "Requires ${formatRam(game.minRamGb)} RAM minimum; device has ${formatRam(device.totalRamGb)}."
        BottleneckType.GPU_LIMITED ->
          "GPU benchmark (${device.gpuScore}) is below the ${game.minGpuScore} minimum threshold."
        BottleneckType.CPU_LIMITED ->
          "CPU benchmark (${device.cpuScore}) is below the ${game.minCpuScore} minimum threshold."
        else ->
          "Both memory and compute fall short of minimum specs for stable frame pacing."
      }
    } else if (meetsRecRam && meetsRecCpu && meetsRecGpu) {
      verdict = GameVerdict.RUNS_PERFECTLY
      estimatedFps = 60
      val avgHeadroom = (cpuHeadroom + gpuHeadroom + ramHeadroom) / 3f
      recommendedPreset = if (avgHeadroom >= 1.15f) "Ultra / High (60 FPS)" else "High (60 FPS)"
      bottleneck = BottleneckType.BALANCED
      summaryTip =
        "Meets all recommended RAM (${formatRam(game.recommendedRamGb)}) and compute targets for locked 60 FPS."
    } else {
      verdict = GameVerdict.PLAYABLE
      val limitingProgress = min(
        ((device.totalRamGb - game.minRamGb) / (game.recommendedRamGb - game.minRamGb).coerceAtLeast(0.5)).toFloat(),
        min(
          (device.cpuScore - game.minCpuScore).toFloat() /
            (game.recommendedCpuScore - game.minCpuScore).coerceAtLeast(100),
          (device.gpuScore - game.minGpuScore).toFloat() /
            (game.recommendedGpuScore - game.minGpuScore).coerceAtLeast(100),
        ),
      ).coerceIn(0f, 0.99f)

      estimatedFps = (30 + (limitingProgress * 18f)).roundToInt().coerceIn(30, 48)
      recommendedPreset = if (estimatedFps >= 40) "Medium (30–45 FPS)" else "Low / Medium (30 FPS)"
      bottleneck = when {
        !meetsRecRam -> BottleneckType.RAM_LIMITED
        gpuHeadroom <= cpuHeadroom -> BottleneckType.GPU_LIMITED
        else -> BottleneckType.CPU_LIMITED
      }
      summaryTip = when (bottleneck) {
        BottleneckType.RAM_LIMITED ->
          "Playable at 30 FPS; close background apps since ${formatRam(game.recommendedRamGb)} RAM is recommended."
        BottleneckType.GPU_LIMITED ->
          "Cap frame rate at 30 FPS and lower shadow/anti-aliasing quality to avoid thermal throttling."
        else ->
          "Cap frame rate at 30 FPS and reduce draw distance for consistent frame times."
      }
    }

    return GameEvaluation(
      game = game,
      verdict = verdict,
      estimatedFps = estimatedFps,
      recommendedPreset = recommendedPreset,
      bottleneck = bottleneck,
      ramHeadroomRatio = ramHeadroom,
      cpuHeadroomRatio = cpuHeadroom,
      gpuHeadroomRatio = gpuHeadroom,
      summaryTip = summaryTip,
    )
  }

  private fun formatRam(gb: Double): String = String.format(java.util.Locale.US, "%.1f GB", gb)
}
