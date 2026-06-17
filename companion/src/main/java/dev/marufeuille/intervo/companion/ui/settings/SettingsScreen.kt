package dev.marufeuille.intervo.companion.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marufeuille.intervo.companion.di.companionViewModel
import dev.marufeuille.intervo.companion.health.HealthConnectWriter
import dev.marufeuille.intervo.companion.ui.components.ChipKind
import dev.marufeuille.intervo.companion.ui.components.CompanionCard
import dev.marufeuille.intervo.companion.ui.components.SectionHeader
import dev.marufeuille.intervo.companion.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val vm = companionViewModel { SettingsViewModel(it.repository) }
    val permitted by vm.healthConnectPermitted.collectAsStateWithLifecycle()
    val status by vm.healthConnectStatus.collectAsStateWithLifecycle()
    val pdsStatus by vm.pdsStatus.collectAsStateWithLifecycle()
    val pdsSettings by vm.pdsSettings.collectAsStateWithLifecycle()
    val pendingPds by vm.pendingPdsCount.collectAsStateWithLifecycle()

    val hcPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { vm.onHealthConnectPermissionResult() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = { TopAppBar(title = { Text("設定") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader("連携")
            HealthConnectCard(
                available = vm.healthConnectAvailable,
                permitted = permitted,
                status = status,
                onConnect = { hcPermissionLauncher.launch(HealthConnectWriter.PERMISSIONS) },
            )
            PdsCard(
                serviceUrl = pdsSettings.serviceUrl,
                identifier = pdsSettings.identifier,
                hasAppPassword = pdsSettings.hasAppPassword,
                configured = pdsSettings.isConfigured,
                pendingCount = pendingPds,
                status = pdsStatus,
                onSave = vm::savePdsSettings,
                onClear = vm::clearPdsSettings,
                onSync = vm::retryPds,
            )
        }
    }
}

@Composable
private fun HealthConnectCard(
    available: Boolean,
    permitted: Boolean,
    status: String,
    onConnect: () -> Unit,
) {
    CompanionCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("Health Connect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            StatusChip(
                text = if (permitted) "連携済み" else "未連携",
                kind = if (permitted) ChipKind.Done else ChipKind.Pending,
            )
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = onConnect, enabled = available) {
                Text(if (permitted) "再連携" else "連携する")
            }
        }
    }
}

@Composable
private fun PdsCard(
    serviceUrl: String,
    identifier: String,
    hasAppPassword: Boolean,
    configured: Boolean,
    pendingCount: Int,
    status: String,
    onSave: (serviceUrl: String, identifier: String, appPassword: String) -> Unit,
    onClear: () -> Unit,
    onSync: () -> Unit,
) {
    var editedServiceUrl by rememberSaveable(serviceUrl) { mutableStateOf(serviceUrl) }
    var editedIdentifier by rememberSaveable(identifier) { mutableStateOf(identifier) }
    var appPassword by rememberSaveable(hasAppPassword) { mutableStateOf("") }
    val canSave = editedServiceUrl.isNotBlank() && editedIdentifier.isNotBlank() &&
        (appPassword.isNotBlank() || hasAppPassword)

    CompanionCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Bluesky / PDS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            StatusChip(
                text = if (configured) "設定済み" else "未設定",
                kind = if (configured) ChipKind.Pds else ChipKind.Pending,
            )
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = editedServiceUrl,
            onValueChange = { editedServiceUrl = it },
            label = { Text("PDS URL") },
            placeholder = { Text("https://pds.example.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = editedIdentifier,
            onValueChange = { editedIdentifier = it },
            label = { Text("ハンドル") },
            placeholder = { Text("you.example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = appPassword,
            onValueChange = { appPassword = it },
            label = { Text(if (hasAppPassword) "App Password（保存済み）" else "App Password") },
            placeholder = { Text("xxxx-xxxx-xxxx-xxxx") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        if (pendingCount > 0) {
            Text(
                text = "${pendingCount}件が PDS 未同期です",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = onClear, enabled = configured || editedIdentifier.isNotBlank()) {
                Text("削除")
            }
            Row(horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = { onSave(editedServiceUrl, editedIdentifier, appPassword) },
                    enabled = canSave,
                ) {
                    Text("保存")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSync, enabled = configured) {
                    Text("再同期")
                }
            }
        }
    }
}
