package com.tether.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tether.app.ui.screens.CanvasScreen
import com.tether.app.ui.screens.LoginScreen
import com.tether.app.ui.screens.PairingScreen
import com.tether.app.ui.theme.TetherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            TetherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TetherApp()
                }
            }
        }
    }
}

@Composable
fun TetherApp() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("pairing") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("pairing") {
            PairingScreen(
                onPairingComplete = {
                    navController.navigate("canvas") {
                        popUpTo("pairing") { inclusive = true }
                    }
                },
                onSkipPairing = {
                    // Navigate to canvas anyway (for demo/testing)
                    navController.navigate("canvas") {
                        popUpTo("pairing") { inclusive = true }
                    }
                }
            )
        }
        
        composable("canvas") {
            CanvasScreen()
        }
    }
}
