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

class MultisigProposalStatusActivity : ComponentActivity() {
    private var solanaWeb: SolanaWeb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solanaWeb = SolanaWeb(applicationContext).apply { showLog = true }
        setContent { SolanaTheme { MultisigProposalStatusScreen(solanaWeb!!) } }
    }

    override fun onDestroy() {
        solanaWeb?.release()
        solanaWeb = null
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MultisigProposalStatusScreen(solanaWeb: SolanaWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupDone by remember { mutableStateOf(false) }
    var multisigPda by remember { mutableStateOf("") }
    var transactionIndex by remember { mutableStateOf("") }
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
                title = { Text("Proposal Status") },
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
        Text("Multisig PDA", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = multisigPda, onValueChange = { multisigPda = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Multisig PDA address") })
        Text("Transaction Index", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = transactionIndex, onValueChange = { transactionIndex = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("e.g. 0") })
        Button(
            onClick = {
                val endpoint: String? = RPCConfigManager.currentEndpoint
                if (endpoint.isNullOrBlank()) { resultText = "No RPC endpoint. Set in RPC Config first."; isError = true; return@Button }
                val ep: String = endpoint
                val multi = multisigPda.trim().replace("\n", "")
                val txIndex = transactionIndex.trim().toLongOrNull()
                if (multi.isEmpty()) { resultText = "Enter multisig PDA."; isError = true; return@Button }
                if (txIndex == null || txIndex < 0) { resultText = "Enter valid transaction index."; isError = true; return@Button }
                resultText = null
                lastJson = null
                loading = true
                scope.launch {
                    val r = solanaWeb.getProposalStatusAsync(ep, multi, txIndex)
                    loading = false
                    val err = r?.get("error") as? String
                    if (err != null) {
                        resultText = err
                        lastJson = JSONObject().apply { put("error", err) }.toString(2)
                        isError = true
                    } else if (r != null) {
                        val proposalPda = r["proposalPda"] as? String ?: ""
                        val status = r["status"]?.toString() ?: ""
                        val approvedCount = (r["approvedCount"] as? Number)?.toInt() ?: 0
                        val rejectedCount = (r["rejectedCount"] as? Number)?.toInt() ?: 0
                        val threshold = (r["threshold"] as? Number)?.toInt()
                        resultText = "proposalPda: $proposalPda\nstatus: $status\napprovedCount: $approvedCount\nrejectedCount: $rejectedCount\nthreshold: $threshold"
                        lastJson = JSONObject().apply {
                            put("proposalPda", proposalPda)
                            put("status", status)
                            put("approvedCount", approvedCount)
                            put("rejectedCount", rejectedCount)
                            if (threshold != null) put("threshold", threshold)
                        }.toString(2)
                        isError = false
                    } else {
                        resultText = "No data returned"
                        isError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading
        ) { Text(if (loading) "Querying…" else "Query Proposal Status") }
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
}
