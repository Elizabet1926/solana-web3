package com.elizabet1926.solana.wallet

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
import androidx.compose.runtime.Composable
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

class CreateWalletActivity : ComponentActivity() {

    private var solanaWeb: SolanaWeb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solanaWeb = SolanaWeb(applicationContext).apply { showLog = true }
        setContent {
            SolanaTheme {
                CreateWalletScreen(solanaWeb = solanaWeb!!)
            }
        }
    }

    override fun onDestroy() {
        solanaWeb?.release()
        solanaWeb = null
        super.onDestroy()
    }
}

private val WORD_COUNTS = listOf(12, 15, 18, 21, 24)
private val LANGUAGES = listOf(
    "en" to "English",
    "zh-CN" to "Simplified Chinese",
    "zh-TW" to "Traditional Chinese",
    "ja" to "Japanese",
    "ko" to "Korean",
    "fr" to "French",
    "es" to "Spanish",
    "it" to "Italian"
)

@Composable
private fun CreateWalletScreen(solanaWeb: SolanaWeb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var setupDone by remember { mutableStateOf(false) }
    var selectedWordCount by remember { mutableStateOf(12) }
    var selectedLanguageCode by remember { mutableStateOf("en") }
    var selectedLanguageName by remember { mutableStateOf("English") }
    var loading by remember { mutableStateOf(false) }
    var resultJson by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    LaunchedEffect(solanaWeb) {
        val (success, _) = solanaWeb.setupAsync()
        setupDone = success
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Word count",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WORD_COUNTS.forEach { count ->
                val selected = selectedWordCount == count
                if (selected) {
                    Button(
                        onClick = { selectedWordCount = count },
                        modifier = Modifier.weight(1f)
                    ) { Text("$count") }
                } else {
                    OutlinedButton(
                        onClick = { selectedWordCount = count },
                        modifier = Modifier.weight(1f)
                    ) { Text("$count") }
                }
            }
        }

        Text(
            "Mnemonic language",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = { showLanguageMenu = !showLanguageMenu },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$selectedLanguageName ▼")
        }
        if (showLanguageMenu) {
            LANGUAGES.forEach { (code, name) ->
                OutlinedButton(
                    onClick = {
                        selectedLanguageCode = code
                        selectedLanguageName = name
                        showLanguageMenu = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(name) }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                errorMessage = null
                resultJson = null
                loading = true
                scope.launch {
                    val response = solanaWeb.createWalletAsync(
                        wordCount = selectedWordCount,
                        language = selectedLanguageCode
                    )
                    loading = false
                    val state = response?.get("state") as? Boolean
                    if (state == true) {
                        val json = JSONObject().apply {
                            put("publicKey", response["publicKey"] ?: "")
                            put("privateKey", response["privateKey"] ?: "")
                            put("mnemonic", response["mnemonic"] ?: "")
                            put("wordCount", (response["wordCount"] as? Number)?.toInt() ?: selectedWordCount)
                            put("language", response["language"] ?: selectedLanguageCode)
                        }
                        resultJson = json.toString(2)
                    } else {
                        errorMessage = (response?.get("error") as? String) ?: "Failed to create wallet"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = setupDone && !loading
        ) {
            Text(if (loading) "Creating…" else "Create Wallet")
        }

        if (loading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

        errorMessage?.let { msg ->
            Text(
                "Error: $msg",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        resultJson?.let { json ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = json,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Wallet JSON", json))
                            android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy JSON")
                    }
                }
            }
        }
    }
}
