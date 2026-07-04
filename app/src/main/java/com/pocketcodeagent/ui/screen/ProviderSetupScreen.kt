package com.pocketcodeagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.data.model.Provider
import com.pocketcodeagent.data.model.ProviderPresets
import com.pocketcodeagent.data.model.ProviderTestStatus
import com.pocketcodeagent.ui.theme.*
import com.pocketcodeagent.ui.viewmodel.ProviderViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSetupScreen(
    viewModel: ProviderViewModel,
    activeProvider: Provider?,
    onSelectProvider: (Provider) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    onOpenSecuritySettings: () -> Unit = {}
) {
    val providers by viewModel.providers.collectAsState()
    val isLoadingProviders by viewModel.isLoadingProvidersFlow.collectAsState()
    var editingProviderId by remember { mutableStateOf<Int?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(passwordVisible) {
        if (passwordVisible) { delay(10000); passwordVisible = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provider Settings", color = TextPrimary) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0E0E10))
            )
        },
        containerColor = Color(0xFF0E0E10)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (showForm) {
                item { ProviderForm(viewModel, editingProviderId, passwordVisible, { passwordVisible = !passwordVisible }, { viewModel.clearForm(); editingProviderId = null; showForm = false; passwordVisible = false }, { editingProviderId = null; showForm = false; passwordVisible = false }) }
            } else {
                item {
                    Button(onClick = { viewModel.clearForm(); editingProviderId = null; showForm = true }, colors = ButtonDefaults.buttonColors(containerColor = SlateBlue), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, "Add"); Spacer(Modifier.width(8.dp)); Text("Provider hinzufuegen", color = Color.White)
                    }
                }

                item { Text("Konfigurierte Provider", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }

                when {
                    isLoadingProviders -> item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SlateBlue, strokeWidth = 2.dp) } }
                    providers.isEmpty() -> item { Text("Noch kein Provider konfiguriert.", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp)) }
                    else -> {
                        item { Text("${providers.size} Provider", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp)) }
                        items(providers, key = { it.id }) { provider ->
                            ProviderListItem(provider, isActive = activeProvider?.id == provider.id, isTesting = viewModel.isTesting && viewModel.testingProviderId == provider.id, onSelect = { onSelectProvider(provider) }, onEdit = { viewModel.loadIntoForm(provider); editingProviderId = provider.id; showForm = true; passwordVisible = false }, onTest = { viewModel.testSavedProvider(provider) { updated -> if (activeProvider?.id == updated.id) onSelectProvider(updated) } }, onDelete = { viewModel.deleteProvider(provider) })
                        }
                    }
                }

                // Weiter / Demo button
                item {
                    val hasProvider = activeProvider != null
                    val isReady = activeProvider?.lastTestStatus == ProviderTestStatus.READY
                    val isError = activeProvider?.lastTestStatus == ProviderTestStatus.ERROR

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (hasProvider) {
                            Button(onClick = onNextClick, colors = ButtonDefaults.buttonColors(containerColor = if (isReady) CalmSage else SlateBlue), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Text(if (isReady) "Weiter" else "Trotzdem weiter", color = Color(0xFF0E0E10), fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.PlayArrow, "Proceed", tint = Color(0xFF0E0E10))
                            }
                            if (isError) {
                                Text("Chat mit diesem Provider wird vermutlich fehlschlagen.", color = WarmCopper, fontSize = 10.sp)
                            }
                        } else {
                            OutlinedButton(onClick = onNextClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateBlue)) {
                                Text("Demo-Modus starten", fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(onClick = onOpenSecuritySettings, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateBlue)) {
                        Text("Security & Owner Settings", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ─── Provider Form ────────────────────────────────────────────────────────────
@Composable
private fun ProviderForm(
    viewModel: ProviderViewModel,
    editingProviderId: Int?,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    val modelOptions = viewModel.currentModelOptions()

    val saveReason = buildSaveDisabledReason(viewModel)
    val testReason = buildTestDisabledReason(viewModel)
    val modelsReason = buildModelsDisabledReason(viewModel)
    val canSave = saveReason == null
    val canTest = testReason == null
    val canLoadModels = modelsReason == null

    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF16161C), RoundedCornerShape(8.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(if (editingProviderId == null) "Provider hinzufuegen" else "Provider bearbeiten", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

        // Provider preset picker
        Box {
            SettingsFieldSurface(onClick = { providerMenuExpanded = true }) {
                Column(modifier = Modifier.weight(1f)) { Text("Provider", color = TextSecondary, fontSize = 11.sp); Text(viewModel.name, color = TextPrimary, fontSize = 13.sp, maxLines = 1) }
                Icon(Icons.Default.KeyboardArrowDown, "Provider", tint = TextSecondary)
            }
            DropdownMenu(expanded = providerMenuExpanded, onDismissRequest = { providerMenuExpanded = false }, containerColor = Color(0xFF1E1E26)) {
                ProviderPresets.all.forEach { preset -> DropdownMenuItem(text = { Text(preset.displayName, color = TextPrimary) }, onClick = { viewModel.applyPreset(preset.providerType); providerMenuExpanded = false }) }
            }
        }

        OutlinedTextField(value = viewModel.name, onValueChange = { viewModel.name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
        OutlinedTextField(value = viewModel.baseUrl, onValueChange = { viewModel.baseUrl = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors())

        // Model field
        Box {
            OutlinedTextField(value = viewModel.modelName, onValueChange = { viewModel.modelName = it }, label = { Text("Modell") }, singleLine = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { modelMenuExpanded = true }) { Icon(Icons.Default.KeyboardArrowDown, "Modelle", tint = TextSecondary) } }, colors = fieldColors())
            DropdownMenu(expanded = modelMenuExpanded, onDismissRequest = { modelMenuExpanded = false }, containerColor = Color(0xFF1E1E26), modifier = Modifier.heightIn(max = 320.dp)) {
                modelOptions.forEach { model -> DropdownMenuItem(text = { Text(model, color = TextPrimary, fontSize = 12.sp) }, onClick = { viewModel.modelName = model; modelMenuExpanded = false }) }
            }
        }

        OutlinedTextField(value = viewModel.apiKey, onValueChange = { viewModel.apiKey = it; viewModel.lastTestStatus = ProviderTestStatus.NOT_CONFIGURED; viewModel.lastErrorSanitized = null }, label = { Text("API-Key") }, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = onTogglePassword) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (passwordVisible) "Hide" else "Show", tint = TextSecondary) } }, colors = fieldColors())

        OutlinedTextField(value = viewModel.customHeadersText, onValueChange = { viewModel.customHeadersText = it }, label = { Text("Custom Headers") }, minLines = 2, modifier = Modifier.fillMaxWidth(), colors = fieldColors())

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Streaming", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Switch(checked = viewModel.isStreamSupported, onCheckedChange = { viewModel.isStreamSupported = it }, colors = SwitchDefaults.colors(checkedThumbColor = CalmSage))
        }

        // Modelle + Test buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { viewModel.refreshModelList(editingProviderId ?: 0) }, enabled = canLoadModels, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateBlue)) {
                    Icon(Icons.Default.Refresh, "Load models", modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)); Text("Modelle", fontSize = 12.sp)
                }
                modelsReason?.let { Text(it, color = Color(0xFF888890), fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp)) }
            }
            Column(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { viewModel.testConnection(editingProviderId ?: 0) }, enabled = canTest, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = CalmSage)) {
                    Text(if (viewModel.isTesting) "Testing..." else "Test", fontSize = 12.sp)
                }
                testReason?.let { Text(it, color = Color(0xFF888890), fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp)) }
            }
        }

        // Model list result
        viewModel.modelListResult?.let { result -> StatusText(text = result, isError = result.contains("fehl", ignoreCase = true) || result.contains("failed", ignoreCase = true)) }

        // Test result / error card with actions
        viewModel.testResult?.let { result ->
            val isError = viewModel.lastTestStatus == ProviderTestStatus.ERROR
            if (isError) {
                ErrorActionCard(
                    errorText = result,
                    onRetry = { viewModel.testConnection(editingProviderId ?: 0) },
                    onReloadModels = { viewModel.refreshModelList(editingProviderId ?: 0) }
                )
            } else {
                StatusText(text = result, isError = false)
            }
        }

        // Save button with reason
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel", color = TextSecondary) }
                Button(onClick = { viewModel.saveProvider(editingProviderId ?: 0, onSaved) }, enabled = canSave, colors = ButtonDefaults.buttonColors(containerColor = SlateBlue), modifier = Modifier.weight(1f)) {
                    Text(if (viewModel.isSaving) "Speichern..." else "Save", color = Color.White)
                }
            }
            saveReason?.let { Text(it, color = Color(0xFF888890), fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp)) }
        }
    }
}

// ─── Disabled Button Reason Helpers ──────────────────────────────────────────
private fun buildSaveDisabledReason(vm: ProviderViewModel): String? {
    if (vm.isSaving) return "Wird gespeichert..."
    if (vm.isTesting) return "Test laeuft..."
    if (vm.name.isBlank()) return "Name fehlt"
    if (vm.baseUrl.isBlank()) return "Base URL fehlt"
    if (vm.apiKey.isBlank()) return "API-Key fehlt"
    if (vm.modelName.isBlank()) return "Modell fehlt"
    return null
}

private fun buildTestDisabledReason(vm: ProviderViewModel): String? {
    if (vm.isTesting) return "Test laeuft..."
    if (vm.baseUrl.isBlank()) return "Base URL fehlt"
    if (vm.apiKey.isBlank()) return "API-Key fehlt"
    if (vm.modelName.isBlank()) return "Modell fehlt"
    return null
}

private fun buildModelsDisabledReason(vm: ProviderViewModel): String? {
    if (vm.isLoadingModels) return "Wird geladen..."
    if (vm.baseUrl.isBlank()) return "Base URL fehlt"
    if (vm.apiKey.isBlank()) return "API-Key noetig, um Modelle zu laden."
    return null
}

// ─── Error Action Card ────────────────────────────────────────────────────────
@Composable
private fun ErrorActionCard(
    errorText: String,
    onRetry: () -> Unit,
    onReloadModels: () -> Unit
) {
    Surface(color = Color(0xFF1A0F0F), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, null, tint = WarmCopper, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(errorText.take(120), color = WarmCopper, fontSize = 11.sp, maxLines = 3, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("Erneut testen", fontSize = 10.sp) }
                OutlinedButton(onClick = onReloadModels, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("Modelle laden", fontSize = 10.sp) }
            }
        }
    }
}

// ─── Provider List Item ───────────────────────────────────────────────────────
@Composable
private fun ProviderListItem(
    provider: Provider, isActive: Boolean, isTesting: Boolean,
    onSelect: () -> Unit, onEdit: () -> Unit, onTest: () -> Unit, onDelete: () -> Unit
) {
    val status = if (isTesting) ProviderTestStatus.TESTING else provider.lastTestStatus
    Surface(color = if (isActive) Color(0xFF17232E) else Color(0xFF16161C), shape = RoundedCornerShape(8.dp), border = ButtonDefaults.outlinedButtonBorder, modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(provider.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        if (isActive) { Spacer(Modifier.width(6.dp)); Icon(Icons.Default.Check, "Active", tint = CalmSage, modifier = Modifier.size(15.dp)) }
                    }
                    Text(provider.modelName, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                    Text(provider.baseUrl, color = Color(0xFF777783), fontSize = 11.sp, maxLines = 1)
                }
                ProviderStatusBadge(status = status)
            }
            provider.lastErrorSanitized?.takeIf { status == ProviderTestStatus.ERROR }?.let { Text(it, color = WarmCopper, fontSize = 11.sp, maxLines = 3) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onTest, enabled = !isTesting, modifier = Modifier.weight(1f)) { Text("Test", fontSize = 11.sp) }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit", fontSize = 11.sp) }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Delete, "Delete", tint = WarmCopper, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsFieldSurface(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Surface(onClick = onClick, color = Color(0xFF1E1E26), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
private fun ProviderStatusBadge(status: ProviderTestStatus) {
    val color = when (status) { ProviderTestStatus.READY -> CalmSage; ProviderTestStatus.TESTING -> SlateBlue; ProviderTestStatus.ERROR -> WarmCopper; ProviderTestStatus.NOT_CONFIGURED -> Color(0xFF777783) }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
        Text(status.label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
    }
}

@Composable
private fun StatusText(text: String, isError: Boolean) {
    Text(text = text, color = if (isError) WarmCopper else CalmSage, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().background(Color(0xFF101014), RoundedCornerShape(6.dp)).padding(8.dp))
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = SlateBlue, unfocusedBorderColor = BorderGrey, focusedLabelColor = TextSecondary, unfocusedLabelColor = TextSecondary, cursorColor = SlateBlue)
