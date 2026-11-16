package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GameRepository
import com.example.hardware.HardwareDetector
import com.example.ui.FrameCheckScreen
import com.example.ui.FrameCheckViewModel
import com.example.ui.theme.FrameCheckTheme

class MainActivity : ComponentActivity() {

  private val viewModel: FrameCheckViewModel by viewModels {
    FrameCheckViewModel.Factory(
      hardwareDetector = HardwareDetector(applicationContext),
      gameRepository = GameRepository(applicationContext),
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )

    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      FrameCheckTheme {
        FrameCheckScreen(
          uiState = uiState,
          onSearchQueryChanged = viewModel::onSearchQueryChanged,
          onFilterSelected = viewModel::onFilterSelected,
          onGameSelected = viewModel::onGameSelected,
          onRescanHardware = viewModel::rescanHardware,
        )
      }
    }
  }
}
