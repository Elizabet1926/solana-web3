package com.elizabet1926.solana.balance

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elizabet1926.solana.RPCConfigManager
import com.elizabet1926.solana.theme.SolanaTheme
import com.elizabet1926.solanaweb.SolanaWeb
import kotlinx.coroutines.launch
import org.json.JSONObject

class TokenBalanceActivity : ComponentActivity() {
    private var solanaWeb: SolanaWeb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solanaWeb = SolanaWeb(applicationContext).apply { showLog = true }
        setContent {
            SolanaTheme { TokenBalanceScreen(solanaWeb!!) }
        }
    }

    override fun onDestroy() {
        solanaWeb?.release()
        solanaWeb = null
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun TokenBalanceScreen(solanaWeb: SolanaWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var mint by remember { mutableStateOf("") }
    var decimals by remember { mutableStateOf("6") }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var lastJson by remember { mutableStateOf<String?>(null) }
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
        Text("Owner Address", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Enter owner address (base58)") })
        Text("Token Mint Address", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = mint, onValueChange = { mint = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Enter SPL token mint (base58)") })
        Text("Decimals (optional)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = decimals, onValueChange = { decimals = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("6") })
        Button(
            onClick = {
                val a = address.trim().replace("\n", "")
                val m = mint.trim().replace("\n", "")
                if (a.isEmpty()) {
                    resultText = "Please enter owner address."
                    isError = true
                    return@Button
                }
                if (m.isEmpty()) {
                    resultText = "Please enter token mint address."
                    isError = true
                    return@Button
                }
                val endpoint: String? = RPCConfigManager.currentEndpoint
                if (endpoint == null) {
                    resultText = "No RPC endpoint. Set one in RPC Config first."
                    isError = true
                    return@Button
                }
                val dec = decimals.trim().toIntOrNull() ?: 6
                resultText = null
                lastJson = null
                loading = true
                val ep: String = endpoint
                scope.launch {
                    val r = solanaWeb.getSPLTokenBalanceAsync(ep, a, m, dec)
                    loading = false
                    if (r != null && r["balance"] != null) {
                        resultText = "Balance: ${r["balance"]}"
                        lastJson = JSONObject().apply { put("balance", r["balance"]) }.toString(2)
                        isError = false
                    } else {
                        resultText = (r?.get("error") as? String) ?: "Failed to get token balance"
                        lastJson = JSONObject().apply { put("error", resultText) }.toString(2)
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
                Text(text, modifier = Modifier.padding(16.dp), color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        }
        lastJson?.let { json ->
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Balance", json))
                    android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Copy JSON") }
        }
    }
}
