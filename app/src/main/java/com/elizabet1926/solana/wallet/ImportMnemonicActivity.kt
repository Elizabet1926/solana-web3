package com.elizabet1926.solana.wallet

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elizabet1926.solana.theme.SolanaTheme
import com.elizabet1926.solanaweb.SolanaWeb
import kotlinx.coroutines.launch
import org.json.JSONObject

class ImportMnemonicActivity : ComponentActivity() {
    private var solanaWeb: SolanaWeb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solanaWeb = SolanaWeb(applicationContext).apply { showLog = true }
        setContent {
            SolanaTheme { ImportMnemonicScreen(solanaWeb!!) }
        }
    }

    override fun onDestroy() {
        solanaWeb?.release()
        solanaWeb = null
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun ImportMnemonicScreen(solanaWeb: SolanaWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var mnemonic by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var resultJson by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

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
        Text("Mnemonic (space-separated)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = mnemonic,
            onValueChange = { mnemonic = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            placeholder = { Text("Enter mnemonic, space-separated. Language will be auto-detected.") }
        )
        Button(
            onClick = {
                val m = mnemonic.trim()
                if (m.isEmpty()) {
                    error = "Please enter your mnemonic phrase."
                    return@Button
                }
                error = null
                resultJson = null
                loading = true
                scope.launch {
                    val r = solanaWeb.importAccountFromMnemonicAsync(m)
                    loading = false
                    if (r != null && r["state"] == true) {
                        val json = JSONObject().apply {
                            put("publicKey", r["publicKey"] ?: "")
                            put("privateKey", r["privateKey"] ?: "")
                            put("mnemonic", r["mnemonic"] ?: "")
                            put("wordCount", (r["wordCount"] as? Number)?.toInt() ?: 0)
                            put("language", r["language"] ?: "")
                        }
                        resultJson = json.toString(2)
                    } else {
                        error = (r?.get("error") as? String) ?: "Failed to import account"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading
        ) { Text(if (loading) "Importing…" else "Import Account") }
        if (loading) Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator() }
        error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        resultJson?.let { json ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(value = json, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().height(180.dp), textStyle = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Wallet", json))
                        android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Copy JSON") }
                }
            }
        }
    }
}
