package com.tether.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tether.app.ui.theme.TetherPrimary

@Composable
fun PairingScreen(
    onPairingComplete: () -> Unit,
    onSkipPairing: () -> Unit
) {
    var partnerCode by remember { mutableStateOf("") }
    var showInviteOption by remember { mutableStateOf(false) }
    var pairingStep by remember { mutableStateOf(0) } // 0 = enter code, 1 = confirm phrase
    
    val generatedPhrase = remember { generateRandomPhrase() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Connect with Your Partner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TetherPrimary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!showInviteOption) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Do you have your partner's twin code?",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showInviteOption = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Invite Partner")
                        }
                        
                        Button(
                            onClick = { /* Navigate to code entry */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("I Have Code")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TextButton(
                        onClick = onSkipPairing,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Skip for now (Demo)")
                    }
                }
            }
        } else {
            // Invite flow
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your Connection Phrase",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = generatedPhrase,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TetherPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Share this phrase with your partner. They must enter the exact same phrase to connect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onPairingComplete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("I've Shared This - Continue")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "How it works:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "1. Both partners use the same twin code to create accounts\n" +
                           "2. One partner shares their connection phrase\n" +
                           "3. The other partner enters the phrase to confirm\n" +
                           "4. Start drawing together!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Generate a random 4-word phrase for pairing confirmation
private fun generateRandomPhrase(): String {
    val words = listOf(
        "ocean", "quiet", "ember", "rose", "sky", "dawn", "mist", "wave",
        "star", "moon", "sun", "leaf", "stone", "wind", "rain", "snow",
        "fire", "cloud", "river", "mountain", "forest", "valley", "meadow", "garden"
    )
    
    return buildString {
        repeat(4) { index ->
            if (index > 0) append(" ")
            append(words.random())
        }
    }.replaceFirstChar { it.uppercase() }
}
