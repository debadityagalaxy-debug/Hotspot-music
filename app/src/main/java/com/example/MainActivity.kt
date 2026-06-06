package com.example

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Routes
import com.example.ui.screens.ClientScreen
import com.example.ui.screens.HostScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ThemeBackground
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.HostViewModel
import com.example.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    private val hostViewModel: HostViewModel by viewModels()
    private val clientViewModel: ClientViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("crash_logs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val stackTrace = e.stackTraceToString()
            prefs.edit().putString("last_crash", stackTrace).commit()
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
        
        val lastCrash = prefs.getString("last_crash", null)

        try {
            enableEdgeToEdge()
            setContent {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = ThemeBackground
                    ) {
                        if (lastCrash != null) {
                            CrashScreen(
                                crashLog = lastCrash,
                                onClear = { 
                                    prefs.edit().remove("last_crash").apply()
                                    recreate()
                                }
                            )
                        } else {
                            val navController = rememberNavController()
                            NavHost(navController = navController, startDestination = Routes.ROLE_SELECTION) {
                                composable(Routes.ROLE_SELECTION) {
                                    RoleSelectionScreen(
                                        onNavigateToHost = { navController.navigate(Routes.HOST_ROOM) },
                                        onNavigateToClient = { navController.navigate(Routes.CLIENT_ROOM) },
                                        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
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
                                composable(Routes.SETTINGS) {
                                    SettingsScreen(
                                        viewModel = settingsViewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            setContent {
                MaterialTheme {
                    CrashScreen(
                        crashLog = e.stackTraceToString(),
                        onClear = { recreate() }
                    )
                }
            }
        }
    }
}

@Composable
fun CrashScreen(crashLog: String, onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 48.dp)
    ) {
        Text("App Crashed 😢", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClear) {
            Text("Clear Log & Restart")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = crashLog,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

