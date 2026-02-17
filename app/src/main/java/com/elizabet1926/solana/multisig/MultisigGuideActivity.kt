package com.elizabet1926.solana.multisig

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.elizabet1926.solana.theme.SolanaTheme

/**
 * Shows the Multisig Flow Guide (explain.html) in a WebView.
 * Opened from "View Guide" on multisig screens.
 */
class MultisigGuideActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SolanaTheme {
                MultisigGuideScreen(onDone = { finish() })
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultisigGuideScreen(onDone: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multisig Guide") },
                actions = {
                    TextButton(onClick = onDone) {
                        Text("Done")
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
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.domStorageEnabled = true
                    loadUrl("file:///android_asset/explain.html")
                }
            }
        )
    }
}
