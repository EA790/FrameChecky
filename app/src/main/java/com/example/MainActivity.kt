package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthManager
import com.example.data.CommunityVoteRepository
import com.example.data.GameRepository
import com.example.hardware.HardwareDetector
import com.example.ui.AuthUserProfile
import com.example.ui.FrameCheckScreen
import com.example.ui.FrameCheckViewModel
import com.example.ui.theme.FrameCheckTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

  private val viewModel: FrameCheckViewModel by viewModels {
    FrameCheckViewModel.Factory(
      hardwareDetector = HardwareDetector(applicationContext),
      gameRepository = GameRepository(applicationContext),
      communityVoteRepository = CommunityVoteRepository(applicationContext),
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (FirebaseApp.getApps(applicationContext).isEmpty()) {
      FirebaseApp.initializeApp(applicationContext)
    }
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )

    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val coroutineScope = rememberCoroutineScope()
      val credentialManager = remember { CredentialManager.create(this) }
      val firebaseUser by
        AuthManager.authStateFlow()
          .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

      var isSigningIn by remember { mutableStateOf(false) }
      var authErrorMessage by remember { mutableStateOf<String?>(null) }

      LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(
          context = this@MainActivity,
          credentialManager = credentialManager,
          scope = coroutineScope,
        )
      }

      LaunchedEffect(firebaseUser?.uid) {
        viewModel.setAuthenticatedUserId(firebaseUser?.uid)
      }

      val userProfile =
        firebaseUser?.let { user ->
          AuthUserProfile(
            uid = user.uid,
            displayName = user.displayName ?: user.email?.substringBefore('@') ?: "Gamer",
            photoUrl = user.photoUrl?.toString(),
          )
        }

      FrameCheckTheme {
        FrameCheckScreen(
          uiState = uiState,
          currentUser = userProfile,
          isSigningIn = isSigningIn,
          authErrorMessage = authErrorMessage,
          onSignInClicked = {
            if (!isSigningIn) {
              isSigningIn = true
              authErrorMessage = null
              AuthManager.onGoogleSignInClicked(
                context = this@MainActivity,
                credentialManager = credentialManager,
                scope = coroutineScope,
                onAuthSuccess = {
                  isSigningIn = false
                  authErrorMessage = null
                },
                onAuthError = { msg ->
                  isSigningIn = false
                  authErrorMessage = msg
                },
                onAuthCancelled = {
                  isSigningIn = false
                },
              )
            }
          },
          onSignOutClicked = {
            authErrorMessage = null
            AuthManager.signOut(
              credentialManager = credentialManager,
              scope = coroutineScope,
            )
          },
          onSearchQueryChanged = viewModel::onSearchQueryChanged,
          onFilterSelected = viewModel::onFilterSelected,
          onGameSelected = viewModel::onGameSelected,
          onRescanHardware = viewModel::rescanHardware,
          onRunStorageTest = viewModel::runStorageSpeedTest,
          onVoteClicked = viewModel::submitCommunityVote,
        )
      }
    }
  }
}
