package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Routes
import com.example.ui.screens.ClientScreen
import com.example.ui.screens.HostScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.HostViewModel

class MainActivity : ComponentActivity() {
    private val hostViewModel: HostViewModel by viewModels()
    private val clientViewModel: ClientViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = Routes.ROLE_SELECTION) {
                        composable(Routes.ROLE_SELECTION) {
                            RoleSelectionScreen(
                                onNavigateToHost = { navController.navigate(Routes.HOST_ROOM) },
                                onNavigateToClient = { navController.navigate(Routes.CLIENT_ROOM) }
                            )
                        }
                        composable(Routes.HOST_ROOM) {
                            HostScreen(
                                viewModel = hostViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Routes.CLIENT_ROOM) {
                            ClientScreen(
                                viewModel = clientViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
