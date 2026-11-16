package com.example.hardware

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.example.model.DeviceSpecs
import com.example.model.StorageBenchmarkResult
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ParsedCpuInfo(
  val hardwareName: String,
  val coreCount: Int,
  val architecture: String,
  val sourceDetail: String,
)

class HardwareDetector(private val context: Context) {

  fun detect(): DeviceSpecs {
    val rawCpuInfo = readProcCpuInfo()
    val fallbackCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
    val fallbackHardware = resolveSystemHardwareFallback()
    val parsedCpu = parseCpuInfo(
      cpuInfoText = rawCpuInfo,
      fallbackCores = fallbackCores,
      fallbackHardware = fallbackHardware,
    )

    val maxFreqGhz = readMaxCpuFreqGhz()
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager?.getMemoryInfo(memoryInfo)

    val totalRamBytes = memoryInfo.totalMem.coerceAtLeast(1L)
    val availRamBytes = memoryInfo.availMem.coerceAtLeast(0L)
    val reservedThresholdBytes = memoryInfo.threshold.coerceAtLeast(0L)
    val usableForGamesBytes = (availRamBytes - reservedThresholdBytes).coerceAtLeast(0L)

    val totalRamGb = bytesToGigabytes(totalRamBytes)
    val availableRamGb = bytesToGigabytes(availRamBytes)
    val freeRamForGamesGb = bytesToGigabytes(usableForGamesBytes).coerceAtMost(totalRamGb)
    val systemReservedRamGb =
      (((totalRamGb - freeRamForGamesGb).coerceAtLeast(0.0)) * 10.0).roundToInt() / 10.0

    val glEsVersion = activityManager?.deviceConfigurationInfo?.glEsVersion ?: "3.0"
    val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

    val (cpuScore, gpuScore) = estimateScores(
      hardwareName = parsedCpu.hardwareName,
      coreCount = parsedCpu.coreCount,
      maxFreqGhz = maxFreqGhz,
      totalRamGb = totalRamGb,
      glEsVersion = glEsVersion,
      is64Bit = is64Bit,
    )
    val compositeScore = ((cpuScore * 0.52) + (gpuScore * 0.48)).roundToInt()

    val (tempCelsius, tempSource) = detectThermalCelsius()
    val isThrottling = tempCelsius != null && tempCelsius > THERMAL_THROTTLE_THRESHOLD_C

    return DeviceSpecs(
      hardwareName = parsedCpu.hardwareName,
      cpuArchitecture = parsedCpu.architecture,
      cpuCores = parsedCpu.coreCount,
      maxFreqGhz = maxFreqGhz,
      totalRamGb = totalRamGb,
      availableRamGb = availableRamGb,
      freeRamForGamesGb = freeRamForGamesGb,
      systemReservedRamGb = systemReservedRamGb,
      totalRamBytes = totalRamBytes,
      glEsVersion = glEsVersion,
      cpuScore = cpuScore,
      gpuScore = gpuScore,
      compositeScore = compositeScore,
      batteryTempCelsius = tempCelsius,
      isThermalThrottling = isThrottling,
      tempSourceLabel = tempSource,
      cpuInfoSourceDetail = parsedCpu.sourceDetail,
    )
  }

  suspend fun runStorageSpeedTest(sizeMb: Int = 50): StorageBenchmarkResult =
    withContext(Dispatchers.IO) {
      val targetSizeMb = sizeMb.coerceIn(10, 100)
      val chunkSize = 1024 * 1024
      val buffer = ByteArray(chunkSize) { index -> (index and 0xFF).toByte() }
      val tempFile = File(context.cacheDir, "framecheck_io_bench.tmp")

      try {
        if (tempFile.exists()) tempFile.delete()

        val writeStartNs = System.nanoTime()
        FileOutputStream(tempFile).use { fos ->
          repeat(targetSizeMb) {
            fos.write(buffer)
          }
          fos.flush()
          runCatching { fos.fd.sync() }
        }
        val writeElapsedSec = ((System.nanoTime() - writeStartNs).coerceAtLeast(1L)) / 1_000_000_000.0
        val writeMbPerSec = ((targetSizeMb / writeElapsedSec) * 10.0).roundToInt() / 10.0

        val readBuffer = ByteArray(chunkSize)
        val readStartNs = System.nanoTime()
        FileInputStream(tempFile).use { fis ->
          while (fis.read(readBuffer) != -1) {
            // Sequential read pass
          }
        }
        val readElapsedSec = ((System.nanoTime() - readStartNs).coerceAtLeast(1L)) / 1_000_000_000.0
        val readMbPerSec = ((targetSizeMb / readElapsedSec) * 10.0).roundToInt() / 10.0

        val avgMbPerSec = (((writeMbPerSec + readMbPerSec) / 2.0) * 10.0).roundToInt() / 10.0
        StorageBenchmarkResult(
          writeSpeedMbPerSec = writeMbPerSec,
          readSpeedMbPerSec = readMbPerSec,
          avgSpeedMbPerSec = avgMbPerSec,
          testSizeMb = targetSizeMb,
        )
      } finally {
        runCatching { if (tempFile.exists()) tempFile.delete() }
      }
    }

  private fun detectThermalCelsius(): Pair<Double?, String> {
    val thermalPaths = listOf(
      "/sys/class/thermal/thermal_zone0/temp",
      "/sys/class/thermal/thermal_zone1/temp",
      "/sys/class/power_supply/battery/temp",
    )
    for (path in thermalPaths) {
      try {
        val file = File(path)
        if (file.exists() && file.canRead()) {
          val parsed = parseThermalTempCelsius(file.readText())
          if (parsed != null) {
            return parsed to path.substringAfterLast("/sys/class/")
          }
        }
      } catch (_: Exception) {
        // Ignore SELinux-restricted thermal nodes
      }
    }

    val batteryIntent = runCatching {
      context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }.getOrNull()
    val rawTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
    if (rawTenths != null && rawTenths != Int.MIN_VALUE && rawTenths > 0) {
      val celsius = ((rawTenths / 10.0) * 10.0).roundToInt() / 10.0
      if (celsius in 0.0..120.0) {
        return celsius to "BatteryManager"
      }
    }
    return null to "Sensor unavailable"
  }

  private fun readProcCpuInfo(): String {
    return try {
      val file = File("/proc/cpuinfo")
      if (file.exists() && file.canRead()) {
        file.readText()
      } else {
        ""
      }
    } catch (_: Exception) {
      ""
    }
  }

  private fun readMaxCpuFreqGhz(): Double? {
    val freqPaths = listOf(
      "/sys/devices/system/cpu/cpu7/cpufreq/cpuinfo_max_freq",
      "/sys/devices/system/cpu/cpu6/cpufreq/cpuinfo_max_freq",
      "/sys/devices/system/cpu/cpu4/cpufreq/cpuinfo_max_freq",
      "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq",
    )
    var maxKhz = 0L
    for (path in freqPaths) {
      try {
        val file = File(path)
        if (file.exists() && file.canRead()) {
          val value = file.readText().trim().toLongOrNull() ?: 0L
          if (value > maxKhz) maxKhz = value
        }
      } catch (_: Exception) {
        // Ignore restricted sysfs nodes on locked-down kernels
      }
    }
    return if (maxKhz > 0L) {
      ((maxKhz / 1_000_000.0) * 100.0).roundToInt() / 100.0
    } else {
      null
    }
  }

  private fun resolveSystemHardwareFallback(): String {
    val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      Build.SOC_MODEL.takeUnless { it.isBlank() || it.equals(Build.UNKNOWN, ignoreCase = true) }
    } else {
      null
    }
    val hardware = Build.HARDWARE.takeUnless { it.isBlank() || it.equals(Build.UNKNOWN, ignoreCase = true) }
    val board = Build.BOARD.takeUnless { it.isBlank() || it.equals(Build.UNKNOWN, ignoreCase = true) }
    return socModel ?: hardware ?: board ?: Build.MODEL
  }

  companion object {
    const val THERMAL_THROTTLE_THRESHOLD_C = 40.0

    fun parseThermalTempCelsius(raw: String): Double? {
      val numeric = raw.trim().toDoubleOrNull() ?: return null
      val celsius = when {
        numeric > 1000.0 -> numeric / 1000.0
        numeric > 150.0 -> numeric / 10.0
        else -> numeric
      }
      val rounded = (celsius * 10.0).roundToInt() / 10.0
      return rounded.takeIf { it in 0.0..120.0 }
    }

    fun bytesToGigabytes(bytes: Long): Double {
      val gb = bytes / (1024.0 * 1024.0 * 1024.0)
      return (gb * 10.0).roundToInt() / 10.0
    }

    fun parseCpuInfo(
      cpuInfoText: String,
      fallbackCores: Int,
      fallbackHardware: String,
      systemAbi: String? = runCatching { Build.SUPPORTED_ABIS?.firstOrNull() }.getOrNull(),
    ): ParsedCpuInfo {
      if (cpuInfoText.isBlank()) {
        val arch = systemAbi ?: "ARM64"
        return ParsedCpuInfo(
          hardwareName = fallbackHardware,
          coreCount = fallbackCores.coerceAtLeast(1),
          architecture = arch,
          sourceDetail = "System fallback",
        )
      }

      var processorCount = 0
      var hardwareField: String? = null
      var modelNameField: String? = null
      var processorField: String? = null
      var cpuArchField: String? = null

      cpuInfoText.lineSequence().forEach { rawLine ->
        val colonIndex = rawLine.indexOf(':')
        if (colonIndex <= 0) return@forEach
        val key = rawLine.substring(0, colonIndex).trim()
        val value = rawLine.substring(colonIndex + 1).trim()
        if (value.isEmpty()) return@forEach

        when {
          key.equals("processor", ignoreCase = true) -> processorCount++
          key.equals("Hardware", ignoreCase = true) && hardwareField == null -> hardwareField = value
          key.equals("model name", ignoreCase = true) && modelNameField == null -> modelNameField = value
          key.equals("Processor", ignoreCase = true) && processorField == null -> processorField = value
          key.equals("CPU architecture", ignoreCase = true) && cpuArchField == null -> cpuArchField = value
        }
      }

      val resolvedName = hardwareField ?: modelNameField ?: processorField ?: fallbackHardware
      val resolvedCores = if (processorCount > 0) processorCount else fallbackCores.coerceAtLeast(1)
      val archLabel = when {
        !systemAbi.isNullOrBlank() -> systemAbi
        cpuArchField != null -> "ARMv$cpuArchField"
        else -> "ARM64"
      }
      val sourceLabel = if (hardwareField != null || modelNameField != null) {
        "/proc/cpuinfo"
      } else {
        "/proc/cpuinfo + SoC"
      }

      return ParsedCpuInfo(
        hardwareName = resolvedName,
        coreCount = resolvedCores,
        architecture = archLabel,
        sourceDetail = sourceLabel,
      )
    }

    fun estimateScores(
      hardwareName: String,
      coreCount: Int,
      maxFreqGhz: Double?,
      totalRamGb: Double,
      glEsVersion: String,
      is64Bit: Boolean,
    ): Pair<Int, Int> {
      val effectiveFreq = maxFreqGhz ?: when {
        coreCount >= 8 -> 2.4
        coreCount >= 6 -> 2.1
        else -> 1.8
      }

      val coreFactor = (coreCount.coerceIn(1, 16) * 430)
      val freqFactor = (effectiveFreq.coerceIn(1.0, 4.0) * 1050).roundToInt()
      val bitBonus = if (is64Bit) 750 else 0
      val socMultiplier = resolveSocMultiplier(hardwareName)

      val rawCpu = ((coreFactor + freqFactor + bitBonus) * socMultiplier).roundToInt()
      val cpuScore = rawCpu.coerceIn(1200, 12500)

      val glesMajorMinor = glEsVersion.toDoubleOrNull() ?: 3.0
      val glesBonus = when {
        glesMajorMinor >= 3.2 -> 1250
        glesMajorMinor >= 3.1 -> 800
        else -> 350
      }
      val ramGpuBoost = (totalRamGb.coerceIn(1.0, 16.0) * 210).roundToInt()
      val rawGpu = ((rawCpu * 0.62) + glesBonus + ramGpuBoost).roundToInt()
      val gpuScore = rawGpu.coerceIn(1100, 13000)

      return cpuScore to gpuScore
    }

    private fun resolveSocMultiplier(hardwareName: String): Double {
      val name = hardwareName.lowercase()
      return when {
        "sm8650" in name || "sm8750" in name || "8 gen 3" in name || "8 elite" in name ||
          "dimensity 9300" in name || "dimensity 9400" in name -> 1.48
        "sm8550" in name || "8 gen 2" in name || "tensor g4" in name ||
          "dimensity 9200" in name || "exynos 2400" in name -> 1.32
        "sm8450" in name || "8 gen 1" in name || "7+ gen" in name ||
          "tensor g3" in name || "dimensity 9000" in name || "dimensity 8300" in name -> 1.18
        "tensor" in name || "snapdragon" in name || "qualcomm" in name ||
          "sm7" in name || "sm8" in name || "exynos" in name || "mt68" in name || "mt69" in name -> 1.05
        "ranchu" in name || "goldfish" in name || "qemu" in name || "x86" in name -> 1.02
        else -> 0.96
      }
    }
  }
}
