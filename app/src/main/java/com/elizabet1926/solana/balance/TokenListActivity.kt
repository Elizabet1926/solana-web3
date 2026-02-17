package com.elizabet1926.solana.balance

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlin.math.pow
import kotlinx.coroutines.launch
import org.json.JSONArray

class TokenListActivity : ComponentActivity() {
    private var solanaWeb: SolanaWeb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solanaWeb = SolanaWeb(applicationContext).apply { showLog = true }
        setContent { SolanaTheme { TokenListScreen(solanaWeb!!) } }
    }

    override fun onDestroy() {
        solanaWeb?.release()
        solanaWeb = null
        super.onDestroy()
    }
}

private data class TokenAccountItem(val mint: String, val ata: String, val amount: String, val decimals: Int, val uiAmount: Double?)

@androidx.compose.runtime.Composable
private fun TokenListScreen(solanaWeb: SolanaWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<TokenAccountItem>>(emptyList()) }
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
        Text("Address", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Enter owner address (base58)") })
        Button(
            onClick = {
                val a = address.trim().replace("\n", "")
                if (a.isEmpty()) { summary = "Please enter an address."; isError = true; items = emptyList(); return@Button }
                val endpoint: String? = RPCConfigManager.currentEndpoint
                if (endpoint == null) { summary = "No RPC endpoint. Set one in RPC Config first."; isError = true; items = emptyList(); return@Button }
                summary = null; items = emptyList(); lastJson = null; loading = true
                val ep: String = endpoint
                scope.launch {
                    val r = solanaWeb.getTokenAccountsByOwnerAsync(ep, a)
                    loading = false
                    val raw = r?.get("tokenAccounts") as? String
                    if (!raw.isNullOrEmpty()) {
                        try {
                            val arr = JSONArray(raw)
                            val list = mutableListOf<TokenAccountItem>()
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                list.add(TokenAccountItem(
                                    mint = o.optString("mint", ""), ata = o.optString("ata", ""),
                                    amount = o.optString("amount", "0"), decimals = o.optInt("decimals", 0),
                                    uiAmount = if (o.has("uiAmount")) o.optDouble("uiAmount") else null
                                ))
                            }
                            items = list
                            summary = if (list.isEmpty()) "No token accounts." else "${list.size} token account(s)."
                            lastJson = raw
                            isError = false
                        } catch (_: Exception) {
                            summary = (r?.get("error") as? String) ?: "Failed to parse result"
                            isError = true
                        }
                    } else {
                        summary = (r?.get("error") as? String) ?: "Failed to get token accounts"
                        isError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading
        ) { Text(if (loading) "Querying…" else "Query Token Accounts") }
        if (loading) Column(Modifier.fillMaxWidth()) { CircularProgressIndicator(Modifier.padding(8.dp)) }
        summary?.let { Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) }
        if (items.isNotEmpty()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(200.dp)) {
                items(items) { item ->
                    val balanceStr = item.uiAmount?.let { "%.6f".format(it) }
                        ?: "%.6f".format((item.amount.toDoubleOrNull() ?: 0.0) / 10.0.pow(item.decimals))
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        Text("Mint: ${item.mint}\nATA: ${item.ata}\nBalance: $balanceStr", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
        lastJson?.let { json ->
            Button(onClick = {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("TokenAccounts", json))
                android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.fillMaxWidth()) { Text("Copy JSON") }
        }
    }
}
