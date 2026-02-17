package com.elizabet1926.solana

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.elizabet1926.solana.balance.SOLBalanceActivity
import com.elizabet1926.solana.balance.TokenBalanceActivity
import com.elizabet1926.solana.balance.TokenListActivity
import com.elizabet1926.solana.config.RPCConfigActivity
import com.elizabet1926.solana.multisig.MultisigActivateActivity
import com.elizabet1926.solana.multisig.MultisigApproveActivity
import com.elizabet1926.solana.multisig.MultisigCreateActivity
import com.elizabet1926.solana.multisig.MultisigExecuteActivity
import com.elizabet1926.solana.multisig.MultisigProposalStatusActivity
import com.elizabet1926.solana.multisig.MultisigSendSOLActivity
import com.elizabet1926.solana.multisig.MultisigSendSPLTokenActivity
import com.elizabet1926.solana.signature.SignMessageActivity
import com.elizabet1926.solana.signature.VerifySignatureActivity
import com.elizabet1926.solana.transfer.SendSOLActivity
import com.elizabet1926.solana.transfer.SendSPLTokenActivity
import com.elizabet1926.solana.theme.SolanaTheme
import com.elizabet1926.solana.wallet.CreateWalletActivity
import com.elizabet1926.solana.wallet.ImportMnemonicActivity
import com.elizabet1926.solana.wallet.ImportPrivateKeyActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SolanaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SolanaWalletMenu(
                        modifier = Modifier.padding(innerPadding),
                        onItemClick = { activityClass ->
                            startActivity(Intent(this, activityClass))
                        }
                    )
                }
            }
        }
    }
}

private data class MenuSection(
    val title: String,
    val items: List<Pair<String, Class<out ComponentActivity>>>
)

@Composable
private fun SolanaWalletMenu(
    modifier: Modifier = Modifier,
    onItemClick: (Class<out ComponentActivity>) -> Unit
) {
    val sections = listOf(
        MenuSection("Config", listOf(
            "RPC Config" to RPCConfigActivity::class.java
        )),
        MenuSection("Wallet", listOf(
            "Create Wallet" to CreateWalletActivity::class.java,
            "Import Account (Mnemonic)" to ImportMnemonicActivity::class.java,
            "Import Account (Private Key)" to ImportPrivateKeyActivity::class.java
        )),
        MenuSection("Balance", listOf(
            "SOL Balance" to SOLBalanceActivity::class.java,
            "Token Account List" to TokenListActivity::class.java,
            "SPL Token Balance" to TokenBalanceActivity::class.java
        )),
        MenuSection("Transfer", listOf(
            "Send SOL" to SendSOLActivity::class.java,
            "Send SPL Token" to SendSPLTokenActivity::class.java
        )),
        MenuSection("Multisig", listOf(
            "Multisig - Create" to MultisigCreateActivity::class.java,
            "Multisig - Send SOL" to MultisigSendSOLActivity::class.java,
            "Multisig - Send SPL Token" to MultisigSendSPLTokenActivity::class.java,
            "Multisig - Activate" to MultisigActivateActivity::class.java,
            "Multisig - Approve" to MultisigApproveActivity::class.java,
            "Multisig - Execute" to MultisigExecuteActivity::class.java,
            "Multisig - Proposal Status" to MultisigProposalStatusActivity::class.java
        )),
        MenuSection("Signature", listOf(
            "Sign Message" to SignMessageActivity::class.java,
            "Verify Signature" to VerifySignatureActivity::class.java
        ))
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sections.forEach { section ->
            item {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            items(section.items) { (title, activityClass) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(activityClass) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SolanaWalletMenuPreview() {
    SolanaTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SolanaWalletMenu(onItemClick = {})
        }
    }
}
