package com.pocketcodeagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.ui.theme.CalmSage
import com.pocketcodeagent.ui.theme.DeepSlateBackground
import com.pocketcodeagent.ui.theme.SlateBlue
import com.pocketcodeagent.ui.theme.TextPrimary
import com.pocketcodeagent.ui.theme.TextSecondary
import com.pocketcodeagent.ui.theme.WarmCopper

@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit,
    onStartDemoModeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlateBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Brand Icon
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = SlateBlue,
                modifier = Modifier.size(80.dp)
            )

            // App Brand Name
            Text(
                text = "PocketCodeAgent 👁️",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            // Core pitch line
            Text(
                text = "Mobile Coding Agent für Android ohne Root",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = CalmSage,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Feature bullet list using cards
            WelcomeFeatureRow(
                icon = Icons.Default.Lock,
                title = "API-Keys lokal speichern",
                description = "Hardware-verschlüsselt über den Android Keystore."
            )

            WelcomeFeatureRow(
                icon = Icons.Default.FolderOpen,
                title = "Projekte öffnen",
                description = "Dateien im Handy-Speicher über das offizielle SAF verwalten."
            )

            WelcomeFeatureRow(
                icon = Icons.Default.Psychology,
                title = "Planen, coden, reviewen & fixen",
                description = "Spezialisierte Agenten arbeiten Schritt-für-Schritt."
            )

            WelcomeFeatureRow(
                icon = Icons.Default.Web,
                title = "Preview & Termux-Bridge",
                description = "Ergebnisse direkt ansehen und Node-Server ansteuern."
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Setup / Get Started Button
                Button(
                    onClick = onGetStartedClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SlateBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Jetzt einrichten 🚀",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                // Demo Mode Button (no API-keys required)
                OutlinedButton(
                    onClick = onStartDemoModeClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmSage),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Demo-Modus starten (Ohne Key) 🧪",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun WelcomeFeatureRow(icon: ImageVector, title: String, description: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SlateBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
