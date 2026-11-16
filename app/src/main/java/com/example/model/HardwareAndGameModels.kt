package com.example.model

import com.squareup.moshi.JsonClass

data class DeviceSpecs(
  val hardwareName: String,
  val cpuArchitecture: String,
  val cpuCores: Int,
  val maxFreqGhz: Double?,
  val totalRamGb: Double,
  val availableRamGb: Double,
  val totalRamBytes: Long,
  val glEsVersion: String,
  val cpuScore: Int,
  val gpuScore: Int,
  val compositeScore: Int,
  val cpuInfoSourceDetail: String,
)

@JsonClass(generateAdapter = true)
data class GameRequirement(
  val id: String,
  val name: String,
  val genre: String,
  val minRamGb: Double,
  val recommendedRamGb: Double,
  val minCpuScore: Int,
  val minGpuScore: Int,
  val recommendedCpuScore: Int,
  val recommendedGpuScore: Int,
  val storageGb: Int,
  val engineNote: String,
)

enum class GameVerdict(val label: String, val targetFps: Int) {
  RUNS_PERFECTLY("Runs Perfectly (60 FPS)", 60),
  PLAYABLE("Playable (30 FPS)", 30),
  NOT_SUPPORTED("Not Supported", 0),
}

enum class BottleneckType(val label: String) {
  BALANCED("Balanced Hardware"),
  RAM_LIMITED("RAM Constrained"),
  CPU_LIMITED("CPU Constrained"),
  GPU_LIMITED("GPU Constrained"),
  SYSTEM_LIMITED("CPU/GPU & RAM Below Min"),
}

data class GameEvaluation(
  val game: GameRequirement,
  val verdict: GameVerdict,
  val estimatedFps: Int,
  val recommendedPreset: String,
  val bottleneck: BottleneckType,
  val ramHeadroomRatio: Float,
  val cpuHeadroomRatio: Float,
  val gpuHeadroomRatio: Float,
  val summaryTip: String,
)
