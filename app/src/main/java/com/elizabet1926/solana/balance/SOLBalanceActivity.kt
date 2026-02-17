package com.elizabet1926.solana.balance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elizabet1926.solana.RPCConfigManager
import com.elizabet1926.solana.theme.SolanaTheme
import com.elizabet1926.solanaweb.SolanaWeb
import kotlinx.coroutines.launch

class SOLBalanceActivity : ComponentActivity() {
    private var solanaWeb: SolanaWeb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solanaWeb = SolanaWeb(applicationContext).apply { showLog = true }
        setContent {
            SolanaTheme { SOLBalanceScreen(solanaWeb!!) }
        }
    }

    override fun onDestroy() {
        solanaWeb?.release()
        solanaWeb = null
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun SOLBalanceScreen(solanaWeb: SolanaWeb) {
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(solanaWeb) {
        val (success, _) = solanaWeb.setupAsync()
        setupDone = success
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Address", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter Solana address (base58)") }
        )
        Button(
            onClick = {
                val a = address.trim().replace("\n", "")
                if (a.isEmpty()) {
                    resultText = "Please enter an address."
                    isError = true
                    return@Button
                }
                val endpoint: String? = RPCConfigManager.currentEndpoint
                if (endpoint == null) {
                    resultText = "No RPC endpoint. Set one in RPC Config first."
                    isError = true
                    return@Button
                }
                resultText = null
                loading = true
                val ep: String = endpoint
                scope.launch {
                    val r = solanaWeb.getSOLBalanceAsync(ep, a)
                    loading = false
                    if (r != null && r["balance"] != null) {
                        resultText = "Balance: ${r["balance"]} SOL"
                        isError = false
                    } else {
                        resultText = (r?.get("error") as? String) ?: "Failed to get balance"
                        isError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading
        ) { Text(if (loading) "Querying…" else "Query Balance") }
        if (loading) Column(Modifier.fillMaxWidth()) { CircularProgressIndicator(Modifier.padding(8.dp)) }
        resultText?.let { text ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Text(
                    text = text,
                    modifier = Modifier.padding(16.dp),
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
