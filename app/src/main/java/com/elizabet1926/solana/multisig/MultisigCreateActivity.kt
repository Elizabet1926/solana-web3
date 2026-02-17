package com.elizabet1926.solana.multisig

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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

class MultisigCreateActivity : ComponentActivity() {
    private var solanaWeb: SolanaWeb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solanaWeb = SolanaWeb(applicationContext).apply { showLog = true }
        setContent { SolanaTheme { MultisigCreateScreen(solanaWeb!!) } }
    }

    override fun onDestroy() {
        solanaWeb?.release()
        solanaWeb = null
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MultisigCreateScreen(solanaWeb: SolanaWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var creatorKey by remember { mutableStateOf("") }
    var membersText by remember { mutableStateOf("") }
    var thresholdInt by remember { mutableStateOf(2) }
    var name by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var lastJson by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val memberList = remember(membersText) {
        membersText.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }
    val memberCount = memberList.size.coerceAtLeast(2)
    LaunchedEffect(memberCount) { if (thresholdInt > memberCount) thresholdInt = memberCount }

    LaunchedEffect(solanaWeb) {
        val (success, _) = solanaWeb.setupAsync()
        setupDone = success
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Create Multisig") },
                actions = {
                    TextButton(onClick = { context.startActivity(Intent(context, MultisigGuideActivity::class.java)) }) {
                        Text("View Guide")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        Text("Creator Private Key (Base58)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = creatorKey, onValueChange = { creatorKey = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, placeholder = { Text("Enter creator private key") })
        Text("Member Public Keys (one per line, ≥2)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = membersText, onValueChange = { membersText = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, placeholder = { Text("Public key 1\nPublic key 2\n...") })
        Text("Threshold: $thresholdInt / $memberCount", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = thresholdInt.toFloat(), onValueChange = { thresholdInt = it.toInt().coerceIn(1, memberCount) }, valueRange = 1f..memberCount.toFloat(), steps = (memberCount - 2).coerceAtLeast(0))
        Text("Name (optional)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("e.g. Team Multisig") })
        Button(
            onClick = {
                val endpoint: String? = RPCConfigManager.currentEndpoint
                if (endpoint.isNullOrBlank()) {
                    resultText = "No RPC endpoint. Set one in RPC Config first."
                    isError = true
                    return@Button
                }
                val ep: String = endpoint
                val pk = creatorKey.trim().replace("\n", "")
                if (pk.isEmpty()) { resultText = "Enter creator private key."; isError = true; return@Button }
                if (memberList.size < 2) { resultText = "Enter at least 2 member public keys (one per line)."; isError = true; return@Button }
                if (thresholdInt !in 1..memberCount) { resultText = "Threshold must be between 1 and $memberCount."; isError = true; return@Button }
                resultText = null
                lastJson = null
                loading = true
                scope.launch {
                    val r = solanaWeb.createMultisigAsync(ep, pk, memberList, thresholdInt, name.trim().takeIf { it.isNotEmpty() })
                    loading = false
                    val state = r?.get("state") as? Boolean
                    if (state == true) {
                        val multisigPda = r["multisigPda"] as? String ?: ""
                        val vaultPda = r["vaultPda"] as? String ?: ""
                        val createKeyStr = r["createKey"] as? String ?: ""
                        val createKeyPrivateKey = r["createKeyPrivateKey"] as? String ?: ""
                        val signature = r["signature"] as? String ?: ""
                        resultText = "multisigPda: $multisigPda\nvaultPda: $vaultPda\ncreateKey: $createKeyStr\ncreateKeyPrivateKey: $createKeyPrivateKey\nsignature: $signature"
                        lastJson = JSONObject().apply {
                            put("multisigPda", multisigPda)
                            put("vaultPda", vaultPda)
                            put("createKey", createKeyStr)
                            put("createKeyPrivateKey", createKeyPrivateKey)
                            put("signature", signature)
                        }.toString(2)
                        isError = false
                    } else {
                        resultText = (r?.get("error") as? String) ?: "Failed to create multisig"
                        lastJson = JSONObject().apply { put("error", resultText) }.toString(2)
                        isError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading
        ) { Text(if (loading) "Creating…" else "Create Multisig") }
        if (loading) Column(Modifier.fillMaxWidth()) { CircularProgressIndicator(Modifier.padding(8.dp)) }
        resultText?.let { text ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(Modifier.padding(16.dp)) {
                    if (!isError) {
                        Text("⚠ Save the CreateKey Private Key for recovery only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Text("Fund vaultPda with SOL before executing SOL transfers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⚠️ You must send SOL to vaultPda (address above) before any SOL transfer can be executed. Minimum: more than the rent (~0.0025 SOL). Recommended: at least twice the rent (e.g. ≥0.005 SOL) to cover rent and fees. Otherwise proposals will fail at execution.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "⚠️ On mainnet, the creator and every member address must have some SOL (e.g. ≥0.001–0.005 SOL each). If any of these addresses has zero balance, execution will fail with \"insufficient funds for rent\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(text, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
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
}
