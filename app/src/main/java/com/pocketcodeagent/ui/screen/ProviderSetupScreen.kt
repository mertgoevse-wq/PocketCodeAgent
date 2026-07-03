package com.pocketcodeagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.ui.theme.ElectricTeal
import com.pocketcodeagent.ui.theme.NeonPurple
import com.pocketcodeagent.ui.viewmodel.ProviderViewModel

data class ProviderTemplate(
    val name: String,
    val baseUrl: String,
    val modelName: String
)

val providerTemplates = listOf(
    ProviderTemplate("OpenRouter", "https://openrouter.ai/api/v1", "google/gemini-2.5-flash"),
    ProviderTemplate("Gemini OpenAI-compatible", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.5-flash"),
    ProviderTemplate("NVIDIA NIM", "https://integrate.api.nvidia.com/v1", "meta/llama-3.1-70b-instruct"),
    ProviderTemplate("Groq", "https://api.groq.com/openai/v1", "llama3-70b-8192"),
    ProviderTemplate("Mistral AI", "https://api.mistral.ai/v1", "mistral-large-latest"),
    ProviderTemplate("Together", "https://api.together.xyz/v1", "meta-llama/Llama-3-70b-chat-hf"),
    ProviderTemplate("Custom OpenAI Endpoint", "http://10.0.2.2:8080/v1", "custom-model")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSetupScreen(
    viewModel: ProviderViewModel,
    activeProvider: Provider?,
    onSelectProvider: (Provider) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val providers by viewModel.providers.collectAsState()
    var editingProviderId by remember { mutableStateOf<Int?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Provider Setup", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C1B))
            )
        },
        containerColor = Color(0xFF0C0A14)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form to Add/Edit Provider
            if (showForm) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A33)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (editingProviderId == null) "Add API Provider" else "Edit API Provider",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Templates quick fill
                            if (editingProviderId == null) {
                                Text("Templates (Tap to fill):", style = MaterialTheme.typography.bodySmall, color = ElectricTeal)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LazyColumn(modifier = Modifier.height(100.dp)) {
                                        items(providerTemplates) { template ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF2E2A47))
                                                    .clickable {
                                                        viewModel.name = template.name
                                                        viewModel.baseUrl = template.baseUrl
                                                        viewModel.modelName = template.modelName
                                                    }
                                                    .padding(8.dp)
                                            ) {
                                                Text(template.name, color = Color.White, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = viewModel.name,
                                onValueChange = { viewModel.name = it },
                                label = { Text("Provider Name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonPurple,
                                    unfocusedBorderColor = Color(0xFF2E2A47)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = viewModel.baseUrl,
                                onValueChange = { viewModel.baseUrl = it },
                                label = { Text("Base URL") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonPurple,
                                    unfocusedBorderColor = Color(0xFF2E2A47)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = viewModel.apiKey,
                                onValueChange = { viewModel.apiKey = it },
                                label = { Text("API Key") },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonPurple,
                                    unfocusedBorderColor = Color(0xFF2E2A47)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    val description = if (passwordVisible) "Hide password" else "Show password"
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Text(if (passwordVisible) "Hide" else "Show", color = ElectricTeal, fontSize = 12.sp)
                                    }
                                }
                            )
                            Text(
                                "Stored locally on your device in Android Keystore (encrypted).",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8F88B0),
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = viewModel.modelName,
                                onValueChange = { viewModel.modelName = it },
                                label = { Text("Model Name (e.g. gemini-2.5-flash)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonPurple,
                                    unfocusedBorderColor = Color(0xFF2E2A47)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = viewModel.customHeadersText,
                                onValueChange = { viewModel.customHeadersText = it },
                                label = { Text("Custom Headers (Optional: Key: Value)") },
                                placeholder = { Text("X-Title: PocketCodeAgent\nHTTP-Referer: https://github.com") },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonPurple,
                                    unfocusedBorderColor = Color(0xFF2E2A47)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { viewModel.testConnection() },
                                    enabled = !viewModel.isTesting && viewModel.apiKey.isNotEmpty() && viewModel.baseUrl.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2A47))
                                ) {
                                    Text("Test", color = ElectricTeal)
                                }

                                Row {
                                    TextButton(
                                        onClick = {
                                            viewModel.clearForm()
                                            showForm = false
                                            editingProviderId = null
                                        }
                                    ) {
                                        Text("Cancel", color = Color.LightGray)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.saveProvider(editingProviderId ?: 0) {
                                                showForm = false
                                                editingProviderId = null
                                            }
                                        },
                                        enabled = viewModel.name.isNotEmpty() && viewModel.baseUrl.isNotEmpty() && viewModel.apiKey.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                                    ) {
                                        Text("Save", color = Color.White)
                                    }
                                }
                            }

                            viewModel.testResult?.let { result ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = result,
                                    color = if (result.startsWith("Success")) ElectricTeal else Color.Red,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Add New Button
                item {
                    Button(
                        onClick = {
                            viewModel.clearForm()
                            showForm = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add API Provider", color = Color.White)
                    }
                }
            }

            // Providers List
            item {
                Text(
                    text = "Configured Providers (${providers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            if (providers.isEmpty()) {
                item {
                    Text(
                        "No API providers configured. Add one to run AI agents.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(providers) { provider ->
                    val isActive = activeProvider?.id == provider.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) Color(0xFF1B2A36) else Color(0xFF181528)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                colors = if (isActive) listOf(ElectricTeal, NeonPurple) else listOf(Color(0xFF2E2A47), Color(0xFF2E2A47))
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProvider(provider) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = provider.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Active",
                                            tint = ElectricTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text("Model: ${provider.modelName}", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                                Text(provider.baseUrl, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isActive) {
                                    TextButton(onClick = { onSelectProvider(provider) }) {
                                        Text("Als Standard setzen", color = ElectricTeal, fontSize = 11.sp)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.loadIntoForm(provider)
                                        editingProviderId = provider.id
                                        showForm = true
                                    }
                                ) {
                                    Text("Edit", color = ElectricTeal, fontSize = 11.sp)
                                }

                                IconButton(onClick = { viewModel.deleteProvider(provider) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Next button if active provider selected
            if (activeProvider != null && !showForm) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNextClick,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Proceed with Selected Provider", color = Color(0xFF0C0A14), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Proceed", tint = Color(0xFF0C0A14))
                    }
                }
            }
        }
    }
}
