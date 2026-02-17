package com.elizabet1926.solana.signature

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.elizabet1926.solana.theme.SolanaTheme
import com.elizabet1926.solanaweb.SolanaWeb
import kotlinx.coroutines.launch
import org.json.JSONObject

class VerifySignatureActivity : ComponentActivity() {
    private var solanaWeb: SolanaWeb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solanaWeb = SolanaWeb(applicationContext).apply { showLog = true }
        setContent { SolanaTheme { VerifySignatureScreen(solanaWeb!!) } }
    }

    override fun onDestroy() {
        solanaWeb?.release()
        solanaWeb = null
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun VerifySignatureScreen(solanaWeb: SolanaWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var publicKey by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var signature by remember { mutableStateOf("") }
    var encodingIndex by remember { mutableStateOf(2) }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var lastJson by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    val encodingParam = when (encodingIndex) { 0 -> "utf8"; 1 -> "base58"; else -> "auto" }

    LaunchedEffect(solanaWeb) {
        val (success, _) = solanaWeb.setupAsync()
        setupDone = success
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Public Key (Base58)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = publicKey, onValueChange = { publicKey = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Enter signer public key (base58)") })
        Text("Original Message", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, placeholder = { Text("Enter original message") })
        Text("Signature (Base58)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = signature, onValueChange = { signature = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Enter signature (base58)") })
        Text("Message encoding", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("UTF-8", "Base58", "Auto").forEachIndexed { i, label ->
                OutlinedButton(onClick = { encodingIndex = i }, modifier = Modifier.weight(1f)) { Text(label) }
            }
        }
        Button(
            onClick = {
                val pk = publicKey.trim().replace("\n", "")
                val msg = message
                val sig = signature.trim().replace("\n", "")
                if (pk.isEmpty()) { resultText = "Please enter public key."; isError = true; return@Button }
                if (msg.isEmpty()) { resultText = "Please enter original message."; isError = true; return@Button }
                if (sig.isEmpty()) { resultText = "Please enter signature."; isError = true; return@Button }
                resultText = null
                lastJson = null
                loading = true
                scope.launch {
                    val r = solanaWeb.verifySignatureAsync(pk, msg, sig, encodingParam)
                    loading = false
                    val valid = r?.get("isValid") as? Boolean
                    if (valid != null) {
                        resultText = if (valid) "Signature is valid.\n\nThe message was signed by the provided public key."
                        else "Signature is invalid.\n\nThe message was not signed by the provided public key."
                        lastJson = JSONObject().apply { put("isValid", valid) }.toString(2)
                        isError = !valid
                    } else {
                        resultText = (r?.get("error") as? String) ?: "Failed to verify signature"
                        lastJson = JSONObject().apply { put("error", resultText) }.toString(2)
                        isError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading
        ) { Text(if (loading) "Verifying…" else "Verify Signature") }
        if (loading) Column(Modifier.fillMaxWidth()) { CircularProgressIndicator(Modifier.padding(8.dp)) }
        resultText?.let { text ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Text(text, modifier = Modifier.padding(16.dp), color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        }
        lastJson?.let { json ->
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Result", json))
                android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.fillMaxWidth()) { Text("Copy JSON") }
        }
    }
}
