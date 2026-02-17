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

class MultisigSendSOLActivity : ComponentActivity() {
    private var solanaWeb: SolanaWeb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solanaWeb = SolanaWeb(applicationContext).apply { showLog = true }
        setContent { SolanaTheme { MultisigSendSOLScreen(solanaWeb!!) } }
    }

    override fun onDestroy() {
        solanaWeb?.release()
        solanaWeb = null
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MultisigSendSOLScreen(solanaWeb: SolanaWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var creatorKey by remember { mutableStateOf("") }
    var multisigPda by remember { mutableStateOf("") }
    var toAddress by remember { mutableStateOf("") }
    var amountSol by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var lastJson by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(solanaWeb) {
        val (success, _) = solanaWeb.setupAsync()
        setupDone = success
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Send SOL Proposal") },
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
        Text(
            "Ensure the multisig Vault (vaultPda) has sufficient SOL before creating or executing this proposal; otherwise execution will fail.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        Text("Creator Private Key (Base58)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = creatorKey, onValueChange = { creatorKey = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, placeholder = { Text("Creator private key") })
        Text("Multisig PDA", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = multisigPda, onValueChange = { multisigPda = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Multisig PDA address") })
        Text("To Address", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = toAddress, onValueChange = { toAddress = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Recipient SOL address") })
        Text("Amount (SOL)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = amountSol, onValueChange = { amountSol = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0.1") })
        Button(
            onClick = {
                val endpoint: String? = RPCConfigManager.currentEndpoint
                if (endpoint.isNullOrBlank()) { resultText = "No RPC endpoint. Set in RPC Config first."; isError = true; return@Button }
                val ep: String = endpoint
                val pk = creatorKey.trim().replace("\n", "")
                val multi = multisigPda.trim().replace("\n", "")
                val to = toAddress.trim().replace("\n", "")
                val amount = amountSol.trim().toDoubleOrNull()
                if (pk.isEmpty()) { resultText = "Enter creator private key."; isError = true; return@Button }
                if (multi.isEmpty()) { resultText = "Enter multisig PDA."; isError = true; return@Button }
                if (to.isEmpty()) { resultText = "Enter to address."; isError = true; return@Button }
                if (amount == null || amount <= 0) { resultText = "Enter valid SOL amount."; isError = true; return@Button }
                resultText = null
                lastJson = null
                loading = true
                scope.launch {
                    val r = solanaWeb.multisigSendSOLAsync(
                        endpoint = ep,
                        creatorPrivateKey = pk,
                        multisigPda = multi,
                        to = to,
                        amountSol = amount
                    )
                    loading = false
                    val state = r?.get("state") as? Boolean
                    val err = r?.get("error") as? String
                    if (state == true) {
                        val txIndex = r?.get("transactionIndex")?.toString() ?: ""
                        val vaultTxPda = r?.get("vaultTransactionPda") as? String ?: ""
                        val proposalPda = r?.get("proposalPda") as? String ?: ""
                        val vaultTxSig = r?.get("vaultTransactionSignature") as? String ?: ""
                        val proposalSig = r?.get("proposalSignature") as? String ?: ""
                        val vaultPda = r?.get("vaultPda") as? String ?: ""
                        resultText = "Proposal created.\ntransactionIndex: $txIndex\nvaultTransactionPda: $vaultTxPda\nproposalPda: $proposalPda\nvaultTransactionSignature: $vaultTxSig\nproposalSignature: $proposalSig\nvaultPda: $vaultPda"
                        lastJson = JSONObject().apply {
                            put("transactionIndex", txIndex)
                            put("vaultTransactionPda", vaultTxPda)
                            put("proposalPda", proposalPda)
                            put("vaultTransactionSignature", vaultTxSig)
                            put("proposalSignature", proposalSig)
                            put("vaultPda", vaultPda)
                        }.toString(2)
                        isError = false
                    } else {
                        resultText = err ?: "Failed to create multisig Send SOL proposal"
                        lastJson = JSONObject().apply { put("error", resultText) }.toString(2)
                        isError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading
        ) { Text(if (loading) "Creating proposal…" else "Create Send SOL Proposal") }
        if (loading) Column(Modifier.fillMaxWidth()) { CircularProgressIndicator(Modifier.padding(8.dp)) }
        resultText?.let { text ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(Modifier.padding(16.dp)) {
                    Text(text, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    if (!isError) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Next: go to Multisig - Activate and enter the Transaction Index above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
