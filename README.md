# SolanaWeb

**SolanaWeb** is an Android toolbelt for interacting with the Solana network.

![language](https://img.shields.io/badge/Language-Kotlin-green)
[![jitpack](https://img.shields.io/badge/support-jitpack-green)]

It uses a WebView and JavaScript Bridge to expose Solana capabilities from Kotlin on Android, with support for both callback-based and coroutine-based async APIs.

## JitPack.io

We recommend using [JitPack](https://jitpack.io) to add the library:

```groovy
repositories {
    ...
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.Elizabet1926:solana-web3:1.0.1'
}
```

## 1. Setup & Initialization

Create a `SolanaWeb` instance and call `setup` (or `setupAsync`) before using any other APIs. The setup loads the embedded WebView and JS environment. When you are done, call `release()` to free resources.

```kotlin
val solanaWeb = SolanaWeb(context)
solanaWeb.showLog = true  // Optional: enable Bridge / WebView logs

// Callback style
solanaWeb.setup { success, error ->
    if (success) {
        Log.d("SolanaWeb", "Initialization successful")
    } else {
        Log.e("SolanaWeb", "Initialization failed: $error")
    }
}

// Or with Coroutines
lifecycleScope.launch {
    val (success, error) = solanaWeb.setupAsync()
    if (success) {
        Log.d("SolanaWeb", "Initialization successful")
    } else {
        Log.e("SolanaWeb", "Initialization failed: $error")
    }
}

// Release when no longer needed
solanaWeb.release()
```

## 2. Wallet Management

### Create Random Wallet

```kotlin
val response = solanaWeb.createWalletAsync(wordCount = 12, language = "en")
// Returns mnemonic, privateKey, publicKey, address, etc.
```

### Import Account from Mnemonic

```kotlin
val mnemonic = "word1 word2 ..."
val response = solanaWeb.importAccountFromMnemonicAsync(mnemonic)
```

### Import Account from Private Key

```kotlin
val privateKey = "your-private-key"
val response = solanaWeb.importAccountFromPrivateKeyAsync(privateKey)
```

## 3. Balance & Tokens

### Get SOL Balance

```kotlin
val response = solanaWeb.getSOLBalanceAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    address = "YourPublicKey"
)
// response["balance"] holds the SOL balance
```

### Get SPL Token Balance

```kotlin
val response = solanaWeb.getSPLTokenBalanceAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    address = "YourPublicKey",
    SPLTokenAddress = "TokenMintAddress",
    decimalPoints = 6  // optional
)
```

### Get Token Accounts by Owner

```kotlin
val response = solanaWeb.getTokenAccountsByOwnerAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    address = "YourPublicKey"
)
```

## 4. Transfers

### SOL Transfer

```kotlin
val response = solanaWeb.solanaMainTransferAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    secretKey = "SenderPrivateKey",
    toPublicKey = "ReceiverPublicKey",
    amount = 0.5
)
```

### Estimate SOL Transfer Cost

```kotlin
val response = solanaWeb.estimatedSOLTransferCostAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    fromPublicKey = "FromPublicKey",
    toPublicKey = "ToPublicKey",
    amount = 0.5
)
```

### SPL Token Transfer

```kotlin
val response = solanaWeb.solanaTokenTransferAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    secretKey = "SenderPrivateKey",
    toPublicKey = "ReceiverPublicKey",
    mintAuthority = "TokenMintAddress",
    amount = 100,
    decimals = 6  // optional
)
```

### Estimate SPL Token Transfer Cost

```kotlin
val response = solanaWeb.estimatedSPLTokenTransferCostAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    toPublicKey = "ToPublicKey",
    mint = "TokenMintAddress",
    amount = 100,
    fromPublicKey = "FromPublicKey",  // optional
    privateKey = "PrivateKey",        // optional
    decimals = 6                      // optional
)
```

## 5. Message Signing & Verification

### Sign Message

```kotlin
val response = solanaWeb.signMessageAsync(
    privateKey = "YourPrivateKey",
    message = "Hello Solana",
    encoding = "utf8"  // optional
)
// Returns signature result
```

### Verify Signature

```kotlin
val response = solanaWeb.verifySignatureAsync(
    publicKey = "ExpectedPublicKey",
    message = "Hello Solana",
    signature = "SignatureHex",
    encoding = "utf8"  // optional
)
// Returns whether verification passed
```

## 6. Multisig

### Create Multisig Account

```kotlin
val response = solanaWeb.createMultisigAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    creatorPrivateKey = "CreatorPrivateKey",
    members = listOf("Member1PublicKey", "Member2PublicKey"),
    threshold = 2,
    name = "MyMultisig"  // optional
)
```

### Proposal Activate

```kotlin
val response = solanaWeb.proposalActivateAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    memberPrivateKey = "MemberPrivateKey",
    multisigPda = "MultisigPdaAddress",
    transactionIndex = 0L
)
```

### Proposal Approve

```kotlin
val response = solanaWeb.proposalApproveAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    memberPrivateKey = "MemberPrivateKey",
    multisigPda = "MultisigPdaAddress",
    transactionIndex = 0L
)
```

### Execute Vault Transaction

```kotlin
val response = solanaWeb.vaultTransactionExecuteAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    memberPrivateKey = "MemberPrivateKey",
    multisigPda = "MultisigPdaAddress",
    transactionIndex = 0L,
    vaultIndex = 0  // optional
)
```

### Get Proposal Status

```kotlin
val response = solanaWeb.getProposalStatusAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    multisigPda = "MultisigPdaAddress",
    transactionIndex = 0L
)
```

### Multisig Send SOL

```kotlin
val response = solanaWeb.multisigSendSOLAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    creatorPrivateKey = "CreatorPrivateKey",
    multisigPda = "MultisigPdaAddress",
    to = "ReceiverPublicKey",
    amountSol = 1.0,
    vaultIndex = 0,           // optional
    transactionIndex = null   // optional; pass null when assigned by backend
)
```

### Multisig Send SPL Token

```kotlin
val response = solanaWeb.multisigSendSPLTokenAsync(
    endpoint = "https://api.mainnet-beta.solana.com",
    creatorPrivateKey = "CreatorPrivateKey",
    multisigPda = "MultisigPdaAddress",
    mint = "TokenMintAddress",
    to = "ReceiverPublicKey",
    amount = 100,
    decimals = 6,              // optional
    createAtaIfMissing = true, // optional
    vaultIndex = 0,            // optional
    transactionIndex = null    // optional
)
```

---

## Error Handling

All async methods return `Map<String, Any>?`. Check the `state` or `error` fields to determine success or failure:

```kotlin
val response = solanaWeb.getSOLBalanceAsync(endpoint, address)
if (response != null) {
    val state = response["state"] as? Boolean ?: false
    if (state) {
        val balance = response["balance"]
        Log.d("SolanaWeb", "Balance: $balance")
    } else {
        val error = response["error"] as? String ?: "Unknown error"
        Log.e("SolanaWeb", "Error: $error")
    }
}
```

## License

SolanaWeb is open source under the MIT license. See [LICENSE](LICENSE) for details.
