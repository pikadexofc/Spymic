package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.AuraViewModel
import com.example.ui.screens.ActiveSessionScreen
import com.example.ui.screens.GatewayScreen
import com.example.ui.screens.PlaybackScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.serialization.Serializable

@Serializable object GatewayRoute
@Serializable object ActiveSessionRoute
@Serializable data class PlaybackRoute(val sessionId: Int)

class MainActivity : ComponentActivity() {

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) {}

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            val viewModel: AuraViewModel = viewModel()
            
            NavHost(navController = navController, startDestination = GatewayRoute) {
                composable<GatewayRoute> {
                    GatewayScreen(
                        viewModel = viewModel,
                        onNavigateToActiveSession = {
                            navController.navigate(ActiveSessionRoute)
                        },
                        onNavigateToPlayback = { sessionId ->
                            navController.navigate(PlaybackRoute(sessionId))
                        }
                    )
                }
                composable<ActiveSessionRoute> {
                    ActiveSessionScreen(
                        viewModel = viewModel,
                        onStopSession = {
                            navController.popBackStack()
                        }
                    )
                }
                composable<PlaybackRoute> { backStackEntry ->
                    val route: PlaybackRoute = backStackEntry.toRoute()
                    PlaybackScreen(
                        sessionId = route.sessionId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
      }
    }
  }
}
