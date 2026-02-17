package com.elizabet1926.solana.config

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elizabet1926.solana.RPCConfigManager
import com.elizabet1926.solana.theme.SolanaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RPCConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RPCConfigManager.init(this)
        setContent {
            SolanaTheme {
                RPCConfigScreen()
            }
        }
    }
}

@Composable
private fun RPCConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var presetIndex by remember {
        mutableStateOf(RPCConfigManager.selectedPresetIndex.coerceIn(0, 2))
    }
    var customUrl by remember {
        mutableStateOf(
            RPCConfigManager.customURL?.takeIf { it.isNotBlank() }
                ?: RPCConfigManager.DEFAULT_CUSTOM_PLACEHOLDER
        )
    }
    var statusText by remember { mutableStateOf(initialStatus(context)) }
    var statusColor by remember { mutableStateOf(StatusColor.SECONDARY) }
    var applyEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Network",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                listOf(
                    "Devnet" to RPCConfigManager.PRESET_DEVNET,
                    "Mainnet Beta" to RPCConfigManager.PRESET_MAINNET_BETA,
                    "Custom" to RPCConfigManager.PRESET_CUSTOM
                ).forEach { (label, index) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = presetIndex == index,
                            onClick = {
                                presetIndex = index
                                if (index == RPCConfigManager.PRESET_CUSTOM && customUrl.isBlank()) {
                                    customUrl = RPCConfigManager.DEFAULT_CUSTOM_PLACEHOLDER
                                }
                            }
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        if (presetIndex == RPCConfigManager.PRESET_CUSTOM) {
            Text(
                "Custom RPC URL",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = customUrl,
                onValueChange = { customUrl = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("https://your-rpc.com or https://mainnet.helius-rpc.com/?api-key=KEY") }
            )
            Text(
                "For mainnet, use a custom RPC (e.g. Helius, QuickNode, Alchemy) with an API key.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (presetIndex == RPCConfigManager.PRESET_CUSTOM && customUrl.trim().isBlank()) {
                    statusText = "Enter a custom RPC URL"
                    statusColor = StatusColor.WARNING
                    return@Button
                }
                val custom = if (presetIndex == RPCConfigManager.PRESET_CUSTOM) customUrl.trim().takeIf { it.isNotBlank() } else null
                RPCConfigManager.save(presetIndex, custom)
                val endpoint = RPCConfigManager.currentEndpoint
                if (endpoint == null) {
                    statusText = "No endpoint"
                    statusColor = StatusColor.ERROR
                    return@Button
                }
                statusText = "Connecting..."
                statusColor = StatusColor.SECONDARY
                applyEnabled = false
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        RPCConfigManager.checkConnection(endpoint)
                    }
                    withContext(Dispatchers.Main) {
                        applyEnabled = true
                        result.fold(
                            onSuccess = { version ->
                                statusText = "Connected ($endpoint)\nVersion: $version"
                                statusColor = StatusColor.SUCCESS
                            },
                            onFailure = { e ->
                                statusText = e.message ?: "Connection failed"
                                statusColor = StatusColor.ERROR
                            }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = applyEnabled
        ) {
            Text("Apply RPC")
        }

        Text(
            text = "Status: $statusText",
            style = MaterialTheme.typography.bodySmall,
            color = when (statusColor) {
                StatusColor.SUCCESS -> MaterialTheme.colorScheme.primary
                StatusColor.WARNING -> MaterialTheme.colorScheme.tertiary
                StatusColor.ERROR -> MaterialTheme.colorScheme.error
                StatusColor.SECONDARY -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

private fun initialStatus(context: android.content.Context): String {
    RPCConfigManager.init(context)
    val endpoint = RPCConfigManager.currentEndpoint ?: return "Not connected"
    return "Endpoint set to $endpoint. Tap Apply to verify."
}

private enum class StatusColor { SUCCESS, WARNING, ERROR, SECONDARY }
