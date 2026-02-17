package com.elizabet1926.solana.transfer

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
import androidx.compose.material3.OutlinedButton
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
import kotlin.math.pow
import kotlinx.coroutines.launch
import org.json.JSONObject

class SendSPLTokenActivity : ComponentActivity() {
    private var solanaWeb: SolanaWeb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solanaWeb = SolanaWeb(applicationContext).apply { showLog = true }
        setContent { SolanaTheme { SendSPLTokenScreen(solanaWeb!!) } }
    }

    override fun onDestroy() {
        solanaWeb?.release()
        solanaWeb = null
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun SendSPLTokenScreen(solanaWeb: SolanaWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var secretKey by remember { mutableStateOf("") }
    var fromAddress by remember { mutableStateOf("") }
    var toAddress by remember { mutableStateOf("") }
    var mint by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var decimals by remember { mutableStateOf("6") }
    var estimateText by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var lastJson by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(solanaWeb) {
        val (success, _) = solanaWeb.setupAsync()
        setupDone = success
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Private Key (Base58)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = secretKey, onValueChange = { secretKey = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, placeholder = { Text("Sender private key") })
        Text("From Address (optional)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = fromAddress, onValueChange = { fromAddress = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Leave empty to derive from key") })
        Text("To Address", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = toAddress, onValueChange = { toAddress = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Recipient address") })
        Text("Token Mint Address", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = mint, onValueChange = { mint = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Token mint (base58)") })
        Text("Amount (token units)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = amount, onValueChange = { amount = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("1.0") })
        Text("Decimals", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = decimals, onValueChange = { decimals = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("6") })
        Text("Estimated Cost (SOL)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(
            onClick = {
                val to = toAddress.trim().replace("\n", "")
                val m = mint.trim().replace("\n", "")
                val amt = amount.trim().toDoubleOrNull() ?: 0.0
                val dec = decimals.trim().toIntOrNull() ?: 6
                if (fromAddress.trim().isEmpty() && secretKey.trim().isEmpty()) { estimateText = "Enter private key or from address."; return@OutlinedButton }
                if (to.isEmpty() || m.isEmpty()) { estimateText = "Enter to address and mint."; return@OutlinedButton }
                val endpoint: String = RPCConfigManager.currentEndpoint ?: run { estimateText = "No RPC endpoint."; return@OutlinedButton }
                // JS expects amount in smallest unit (raw); convert token units to raw: amount * 10^decimals
                val amountRaw = (amt * 10.0.pow(dec)).toLong()
                estimateText = "Estimating..."
                scope.launch {
                    var fromPub = fromAddress.trim().replace("\n", "")
                    if (fromPub.isEmpty()) {
                        val r = solanaWeb.importAccountFromPrivateKeyAsync(secretKey.trim().replace("\n", ""))
                        fromPub = (r?.get("publicKey") as? String) ?: ""
                        if (fromPub.isEmpty()) { estimateText = (r?.get("error") as? String) ?: "Failed to get sender."; return@launch }
                    }
                    val r = solanaWeb.estimatedSPLTokenTransferCostAsync(endpoint, to, m, amountRaw, fromPub, secretKey.trim().replace("\n", "").takeIf { it.isNotEmpty() }, dec)
                    val err = r?.get("error") as? String
                    if (err != null) { estimateText = err; return@launch }
                    val cost = r?.get("cost") as? String
                    estimateText = if (cost != null) "Estimated SOL cost: $cost" else "No estimate."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Estimate Fee") }
        estimateText?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val sk = secretKey.trim().replace("\n", "")
                val to = toAddress.trim().replace("\n", "")
                val m = mint.trim().replace("\n", "")
                val amt = amount.trim().toDoubleOrNull() ?: 0.0
                val dec = decimals.trim().toIntOrNull() ?: 6
                if (sk.isEmpty()) { resultText = "Please enter private key."; isError = true; return@Button }
                if (to.isEmpty() || m.isEmpty()) { resultText = "Please enter to address and mint."; isError = true; return@Button }
                val endpoint: String = RPCConfigManager.currentEndpoint ?: run { resultText = "No RPC endpoint."; isError = true; return@Button }
                // JS expects amount in smallest unit (raw); convert token units to raw: amount * 10^decimals
                val amountRaw = (amt * 10.0.pow(dec)).toLong()
                resultText = null
                lastJson = null
                loading = true
                scope.launch {
                    val r = solanaWeb.solanaTokenTransferAsync(endpoint, sk, to, m, amountRaw, dec)
                    loading = false
                    val sig = r?.get("signature") as? String
                    if (sig != null) {
                        resultText = "Success.\n\nSignature:\n$sig"
                        lastJson = JSONObject().apply { put("signature", sig) }.toString(2)
                        isError = false
                    } else {
                        resultText = (r?.get("error") as? String) ?: "Failed to send SPL token"
                        lastJson = JSONObject().apply { put("error", resultText) }.toString(2)
                        isError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading
        ) { Text(if (loading) "Sending…" else "Send SPL Token") }
        if (loading) Column(Modifier.fillMaxWidth()) { CircularProgressIndicator(Modifier.padding(8.dp)) }
        resultText?.let { text ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Text(text, modifier = Modifier.padding(16.dp), color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        }
        lastJson?.let { json ->
            Button(onClick = {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Result", json))
                android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.fillMaxWidth()) { Text("Copy JSON") }
        }
    }
}
