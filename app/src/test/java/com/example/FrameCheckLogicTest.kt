package com.example

import com.example.domain.CompatibilityEvaluator
import com.example.hardware.HardwareDetector
import com.example.model.BottleneckType
import com.example.model.DeviceSpecs
import com.example.model.GameRequirement
import com.example.model.GameVerdict
import com.example.model.GameVoteSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrameCheckLogicTest {

  private val sampleGame =
    GameRequirement(
      id = "genshin_impact",
      name = "Genshin Impact",
      genre = "Open-World RPG",
      minRamGb = 4.0,
      recommendedRamGb = 8.0,
      minCpuScore = 4200,
      minGpuScore = 4000,
      recommendedCpuScore = 7600,
      recommendedGpuScore = 7500,
      storageGb = 28,
      engineNote = "Open-world shader streaming.",
    )

  private fun deviceWith(
    ramGb: Double,
    cpuScore: Int,
    gpuScore: Int,
  ) =
    DeviceSpecs(
      hardwareName = "Qualcomm Snapdragon 8 Gen 3",
      cpuArchitecture = "arm64-v8a",
      cpuCores = 8,
      maxFreqGhz = 3.3,
      totalRamGb = ramGb,
      availableRamGb = ramGb * 0.6,
      freeRamForGamesGb = ramGb * 0.5,
      systemReservedRamGb = ramGb * 0.5,
      totalRamBytes = (ramGb * 1024 * 1024 * 1024).toLong(),
      glEsVersion = "3.2",
      cpuScore = cpuScore,
      gpuScore = gpuScore,
      compositeScore = (cpuScore + gpuScore) / 2,
      batteryTempCelsius = 36.5,
      isThermalThrottling = false,
      tempSourceLabel = "thermal_zone0/temp",
      cpuInfoSourceDetail = "/proc/cpuinfo",
    )

  @Test
  fun `parseCpuInfo extracts hardware name and counts all processor cores`() {
    val cpuInfoSample =
      """
      processor       : 0
      BogoMIPS        : 38.40
      Features        : fp asimd evtstrm aes pmull sha1 sha2 crc32
      CPU implementer : 0x41
      CPU architecture: 8

      processor       : 1
      CPU implementer : 0x41

      processor       : 2
      CPU implementer : 0x41

      processor       : 3
      CPU implementer : 0x41
      Hardware        : Qualcomm Technologies, Inc SM8550
      """
        .trimIndent()

    val parsed =
      HardwareDetector.parseCpuInfo(
        cpuInfoText = cpuInfoSample,
        fallbackCores = 1,
        fallbackHardware = "Fallback SoC",
      )

    assertEquals("Qualcomm Technologies, Inc SM8550", parsed.hardwareName)
    assertEquals(4, parsed.coreCount)
  }

  @Test
  fun `parseThermalTempCelsius handles millidegrees, decidegrees, and celsius`() {
    assertEquals(42.5, HardwareDetector.parseThermalTempCelsius("42500")!!, 0.01)
    assertEquals(41.2, HardwareDetector.parseThermalTempCelsius("412")!!, 0.01)
    assertEquals(37.0, HardwareDetector.parseThermalTempCelsius("37")!!, 0.01)
    assertNull(HardwareDetector.parseThermalTempCelsius("invalid"))
  }

  @Test
  fun `game vote summary calculates percentage accurately`() {
    val summary = GameVoteSummary(gameId = "genshin_impact", runsGreatCount = 3L, lagsCount = 1L)
    assertEquals(4L, summary.totalVotes)
    assertEquals(75, summary.runsGreatPercentage)
  }

  @Test
  fun `evaluate returns Runs Perfectly 60 FPS when all recommended specs are met`() {
    val flagship = deviceWith(ramGb = 12.0, cpuScore = 8800, gpuScore = 8500)
    val result = CompatibilityEvaluator.evaluate(flagship, sampleGame)

    assertEquals(GameVerdict.RUNS_PERFECTLY, result.verdict)
    assertEquals("Runs Perfectly (60 FPS)", result.verdict.label)
    assertEquals(60, result.estimatedFps)
    assertEquals(BottleneckType.BALANCED, result.bottleneck)
  }

  @Test
  fun `evaluate returns Playable 30 FPS when minimum met but below recommended`() {
    val midRange = deviceWith(ramGb = 6.0, cpuScore = 5600, gpuScore = 5200)
    val result = CompatibilityEvaluator.evaluate(midRange, sampleGame)

    assertEquals(GameVerdict.PLAYABLE, result.verdict)
    assertEquals("Playable (30 FPS)", result.verdict.label)
    assertEquals(BottleneckType.RAM_LIMITED, result.bottleneck)
  }

  @Test
  fun `evaluate returns Not Supported when RAM or compute is below minimum`() {
    val lowEnd = deviceWith(ramGb = 3.0, cpuScore = 3000, gpuScore = 2800)
    val result = CompatibilityEvaluator.evaluate(lowEnd, sampleGame)

    assertEquals(GameVerdict.NOT_SUPPORTED, result.verdict)
    assertEquals("Not Supported", result.verdict.label)
    assertEquals(BottleneckType.SYSTEM_LIMITED, result.bottleneck)
  }
}
