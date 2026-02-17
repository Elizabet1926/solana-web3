package com.elizabet1926.solanaweb

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.elizabet1926.solanaweb.bridge.WebViewJavascriptBridge
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * SolanaWeb - SDK for Solana blockchain operations on Android.
 * Uses WebViewJavascriptBridge to call JS (mobile.js) handlers.
 * Call [setup] or [setupAsync] before using other APIs. Call [release] when done to free WebView.
 */
class SolanaWeb(private val context: Context) {

    companion object {
        private const val TAG = "SolanaWeb-SDK"
    }

    private var webView: WebView? = null
    private var bridge: WebViewJavascriptBridge? = null

    /** True after JS has signalled ready via generateSolanaWeb3. */
    var isInitialized: Boolean = false
        private set

    /** When true, bridge and WebView logs are printed to Logcat. */
    var showLog: Boolean = false

    init {
        setupWebView()
    }

    private fun setupWebView() {
        webView = WebView(context).apply {
            @Suppress("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (showLog) Log.d(TAG, "WebView finished loading: $url")
                }
            }
        }
        bridge = WebViewJavascriptBridge(webView!!, isHookConsole = true)
        bridge?.consolePipeClosure = { msg ->
            if (showLog) Log.d("SolanaWeb-JS", msg.toString())
        }
    }

    /**
     * Initialize: register for JS ready signal (generateSolanaWeb3) then load assets.
     * Call this before using any other API.
     */
    fun setup(onCompleted: (Boolean, String?) -> Unit) {
        bridge?.register("generateSolanaWeb3") { _, callback ->
            if (showLog) Log.d(TAG, "JS Bridge ready (generateSolanaWeb3).")
            isInitialized = true
            onCompleted(true, null)
            callback?.invoke(mapOf("status" to "received"))
        }
        webView?.loadUrl("file:///android_asset/index.html")
    }

    /** Suspend until setup is complete. */
    suspend fun setupAsync(): Pair<Boolean, String?> = suspendCancellableCoroutine { cont ->
        setup { success, error ->
            cont.resume(Pair(success, error))
        }
    }

    /** Release WebView and bridge. Call from Activity/Fragment when no longer needed. */
    fun release() {
        bridge?.reset()
        bridge = null
        webView?.destroy()
        webView = null
        isInitialized = false
    }

    private fun call(handlerName: String, params: Map<String, Any?>, completion: (Map<String, Any>?) -> Unit) {
        val clean = params.mapNotNull { (k, v) -> if (v != null) k to v else null }.toMap()
        bridge?.call(handlerName, if (clean.isEmpty()) null else clean) { response ->
            @Suppress("UNCHECKED_CAST")
            completion(response as? Map<String, Any>)
        }
    }

    private suspend fun callAsync(handlerName: String, params: Map<String, Any?>): Map<String, Any>? =
        suspendCancellableCoroutine<Map<String, Any>?> { cont ->
            call(handlerName, params) { cont.resume(it) }
        }

    // --- Wallet ---
    fun createWallet(wordCount: Int = 12, language: String = "en", completion: (Map<String, Any>?) -> Unit) {
        call("createWallet", mapOf("wordCount" to wordCount, "language" to language), completion)
    }

    suspend fun createWalletAsync(wordCount: Int = 12, language: String = "en"): Map<String, Any>? =
        callAsync("createWallet", mapOf("wordCount" to wordCount, "language" to language))

    fun importAccountFromMnemonic(mnemonic: String, completion: (Map<String, Any>?) -> Unit) {
        call("importAccountFromMnemonic", mapOf("mnemonic" to mnemonic), completion)
    }

    suspend fun importAccountFromMnemonicAsync(mnemonic: String): Map<String, Any>? =
        callAsync("importAccountFromMnemonic", mapOf("mnemonic" to mnemonic))

    fun importAccountFromPrivateKey(privateKey: String, completion: (Map<String, Any>?) -> Unit) {
        call("importAccountFromPrivateKey", mapOf("privateKey" to privateKey), completion)
    }

    suspend fun importAccountFromPrivateKeyAsync(privateKey: String): Map<String, Any>? =
        callAsync("importAccountFromPrivateKey", mapOf("privateKey" to privateKey))

    // --- Balance & Token ---
    fun getSOLBalance(endpoint: String, address: String, completion: (Map<String, Any>?) -> Unit) {
        call("getSOLBalance", mapOf("endpoint" to endpoint, "address" to address), completion)
    }

    suspend fun getSOLBalanceAsync(endpoint: String, address: String): Map<String, Any>? =
        callAsync("getSOLBalance", mapOf("endpoint" to endpoint, "address" to address))

    fun getSPLTokenBalance(
        endpoint: String,
        address: String,
        SPLTokenAddress: String,
        decimalPoints: Int? = null,
        completion: (Map<String, Any>?) -> Unit
    ) {
        val p = mutableMapOf<String, Any?>("endpoint" to endpoint, "address" to address, "SPLTokenAddress" to SPLTokenAddress)
        decimalPoints?.let { p["decimalPoints"] = it }
        call("getSPLTokenBalance", p, completion)
    }

    suspend fun getSPLTokenBalanceAsync(
        endpoint: String,
        address: String,
        SPLTokenAddress: String,
        decimalPoints: Int? = null
    ): Map<String, Any>? {
        val p = mutableMapOf<String, Any?>("endpoint" to endpoint, "address" to address, "SPLTokenAddress" to SPLTokenAddress)
        decimalPoints?.let { p["decimalPoints"] = it }
        return callAsync("getSPLTokenBalance", p)
    }

    fun getTokenAccountsByOwner(endpoint: String, address: String, completion: (Map<String, Any>?) -> Unit) {
        call("getTokenAccountsByOwner", mapOf("endpoint" to endpoint, "address" to address), completion)
    }

    suspend fun getTokenAccountsByOwnerAsync(endpoint: String, address: String): Map<String, Any>? =
        callAsync("getTokenAccountsByOwner", mapOf("endpoint" to endpoint, "address" to address))

    // --- Transfer ---
    fun solanaMainTransfer(
        endpoint: String,
        secretKey: String,
        toPublicKey: String,
        amount: Number,
        completion: (Map<String, Any>?) -> Unit
    ) {
        call("solanaMainTransfer", mapOf(
            "endpoint" to endpoint,
            "secretKey" to secretKey,
            "toPublicKey" to toPublicKey,
            "amount" to amount
        ), completion)
    }

    suspend fun solanaMainTransferAsync(
        endpoint: String,
        secretKey: String,
        toPublicKey: String,
        amount: Number
    ): Map<String, Any>? = callAsync("solanaMainTransfer", mapOf(
        "endpoint" to endpoint, "secretKey" to secretKey, "toPublicKey" to toPublicKey, "amount" to amount
    ))

    fun estimatedSOLTransferCost(
        endpoint: String,
        fromPublicKey: String,
        toPublicKey: String,
        amount: Number,
        completion: (Map<String, Any>?) -> Unit
    ) {
        call("estimatedSOLTransferCost", mapOf(
            "endpoint" to endpoint, "fromPublicKey" to fromPublicKey, "toPublicKey" to toPublicKey, "amount" to amount
        ), completion)
    }

    suspend fun estimatedSOLTransferCostAsync(
        endpoint: String,
        fromPublicKey: String,
        toPublicKey: String,
        amount: Number
    ): Map<String, Any>? = callAsync("estimatedSOLTransferCost", mapOf(
        "endpoint" to endpoint, "fromPublicKey" to fromPublicKey, "toPublicKey" to toPublicKey, "amount" to amount
    ))

    fun solanaTokenTransfer(
        endpoint: String,
        secretKey: String,
        toPublicKey: String,
        mintAuthority: String,
        amount: Number,
        decimals: Int? = null,
        completion: (Map<String, Any>?) -> Unit
    ) {
        val p = mutableMapOf<String, Any?>(
            "endpoint" to endpoint, "secretKey" to secretKey, "toPublicKey" to toPublicKey,
            "mintAuthority" to mintAuthority, "amount" to amount
        )
        decimals?.let { p["decimals"] = it }
        call("solanaTokenTransfer", p, completion)
    }

    suspend fun solanaTokenTransferAsync(
        endpoint: String,
        secretKey: String,
        toPublicKey: String,
        mintAuthority: String,
        amount: Number,
        decimals: Int? = null
    ): Map<String, Any>? {
        val p = mutableMapOf<String, Any?>(
            "endpoint" to endpoint, "secretKey" to secretKey, "toPublicKey" to toPublicKey,
            "mintAuthority" to mintAuthority, "amount" to amount
        )
        decimals?.let { p["decimals"] = it }
        return callAsync("solanaTokenTransfer", p)
    }

    fun estimatedSPLTokenTransferCost(
        endpoint: String,
        toPublicKey: String,
        mint: String,
        amount: Number,
        fromPublicKey: String? = null,
        privateKey: String? = null,
        decimals: Int? = null,
        completion: (Map<String, Any>?) -> Unit
    ) {
        val p = mutableMapOf<String, Any?>("endpoint" to endpoint, "toPublicKey" to toPublicKey, "mint" to mint, "amount" to amount)
        fromPublicKey?.let { p["fromPublicKey"] = it }
        privateKey?.let { p["privateKey"] = it }
        decimals?.let { p["decimals"] = it }
        call("estimatedSPLTokenTransferCost", p, completion)
    }

    suspend fun estimatedSPLTokenTransferCostAsync(
        endpoint: String,
        toPublicKey: String,
        mint: String,
        amount: Number,
        fromPublicKey: String? = null,
        privateKey: String? = null,
        decimals: Int? = null
    ): Map<String, Any>? {
        val p = mutableMapOf<String, Any?>("endpoint" to endpoint, "toPublicKey" to toPublicKey, "mint" to mint, "amount" to amount)
        fromPublicKey?.let { p["fromPublicKey"] = it }
        privateKey?.let { p["privateKey"] = it }
        decimals?.let { p["decimals"] = it }
        return callAsync("estimatedSPLTokenTransferCost", p)
    }

    // --- Sign / Verify ---
    fun signMessage(privateKey: String, message: String, encoding: String? = null, completion: (Map<String, Any>?) -> Unit) {
        val p = mutableMapOf<String, Any?>("privateKey" to privateKey, "message" to message)
        encoding?.let { p["encoding"] = it }
        call("signMessage", p, completion)
    }

    suspend fun signMessageAsync(privateKey: String, message: String, encoding: String? = null): Map<String, Any>? {
        val p = mutableMapOf<String, Any?>("privateKey" to privateKey, "message" to message)
        encoding?.let { p["encoding"] = it }
        return callAsync("signMessage", p)
    }

    fun verifySignature(
        publicKey: String,
        message: String,
        signature: String,
        encoding: String? = null,
        completion: (Map<String, Any>?) -> Unit
    ) {
        val p = mutableMapOf<String, Any?>("publicKey" to publicKey, "message" to message, "signature" to signature)
        encoding?.let { p["encoding"] = it }
        call("verifySignature", p, completion)
    }

    suspend fun verifySignatureAsync(
        publicKey: String,
        message: String,
        signature: String,
        encoding: String? = null
    ): Map<String, Any>? {
        val p = mutableMapOf<String, Any?>("publicKey" to publicKey, "message" to message, "signature" to signature)
        encoding?.let { p["encoding"] = it }
        return callAsync("verifySignature", p)
    }

    // --- Multisig: Create ---
    fun createMultisig(
        endpoint: String,
        creatorPrivateKey: String,
        members: List<String>,
        threshold: Int,
        name: String? = null,
        completion: (Map<String, Any>?) -> Unit
    ) {
        val p = mutableMapOf<String, Any?>(
            "endpoint" to endpoint,
            "creatorPrivateKey" to creatorPrivateKey,
            "members" to members,
            "threshold" to threshold
        )
        name?.let { p["name"] = it }
        call("createMultisig", p, completion)
    }

    suspend fun createMultisigAsync(
        endpoint: String,
        creatorPrivateKey: String,
        members: List<String>,
        threshold: Int,
        name: String? = null
    ): Map<String, Any>? {
        val p = mutableMapOf<String, Any?>(
            "endpoint" to endpoint, "creatorPrivateKey" to creatorPrivateKey, "members" to members, "threshold" to threshold
        )
        name?.let { p["name"] = it }
        return callAsync("createMultisig", p)
    }

    // --- Multisig: Activate / Approve / Execute ---
    fun proposalActivate(
        endpoint: String,
        memberPrivateKey: String,
        multisigPda: String,
        transactionIndex: Long,
        completion: (Map<String, Any>?) -> Unit
    ) {
        call("proposalActivate", mapOf(
            "endpoint" to endpoint,
            "memberPrivateKey" to memberPrivateKey,
            "multisigPda" to multisigPda,
            "transactionIndex" to transactionIndex
        ), completion)
    }

    suspend fun proposalActivateAsync(
        endpoint: String,
        memberPrivateKey: String,
        multisigPda: String,
        transactionIndex: Long
    ): Map<String, Any>? = callAsync("proposalActivate", mapOf(
        "endpoint" to endpoint, "memberPrivateKey" to memberPrivateKey, "multisigPda" to multisigPda, "transactionIndex" to transactionIndex
    ))

    fun proposalApprove(
        endpoint: String,
        memberPrivateKey: String,
        multisigPda: String,
        transactionIndex: Long,
        completion: (Map<String, Any>?) -> Unit
    ) {
        call("proposalApprove", mapOf(
            "endpoint" to endpoint,
            "memberPrivateKey" to memberPrivateKey,
            "multisigPda" to multisigPda,
            "transactionIndex" to transactionIndex
        ), completion)
    }

    suspend fun proposalApproveAsync(
        endpoint: String,
        memberPrivateKey: String,
        multisigPda: String,
        transactionIndex: Long
    ): Map<String, Any>? = callAsync("proposalApprove", mapOf(
        "endpoint" to endpoint, "memberPrivateKey" to memberPrivateKey, "multisigPda" to multisigPda, "transactionIndex" to transactionIndex
    ))

    fun vaultTransactionExecute(
        endpoint: String,
        memberPrivateKey: String,
        multisigPda: String,
        transactionIndex: Long,
        vaultIndex: Int? = null,
        completion: (Map<String, Any>?) -> Unit
    ) {
        val p = mutableMapOf<String, Any?>(
            "endpoint" to endpoint,
            "memberPrivateKey" to memberPrivateKey,
            "multisigPda" to multisigPda,
            "transactionIndex" to transactionIndex
        )
        vaultIndex?.let { p["vaultIndex"] = it }
        call("vaultTransactionExecute", p, completion)
    }

    suspend fun vaultTransactionExecuteAsync(
        endpoint: String,
        memberPrivateKey: String,
        multisigPda: String,
        transactionIndex: Long,
        vaultIndex: Int? = null
    ): Map<String, Any>? {
        val p = mutableMapOf<String, Any?>(
            "endpoint" to endpoint, "memberPrivateKey" to memberPrivateKey, "multisigPda" to multisigPda, "transactionIndex" to transactionIndex
        )
        vaultIndex?.let { p["vaultIndex"] = it }
        return callAsync("vaultTransactionExecute", p)
    }

    fun getProposalStatus(
        endpoint: String,
        multisigPda: String,
        transactionIndex: Long,
        completion: (Map<String, Any>?) -> Unit
    ) {
        call("getProposalStatus", mapOf(
            "endpoint" to endpoint,
            "multisigPda" to multisigPda,
            "transactionIndex" to transactionIndex
        ), completion)
    }

    suspend fun getProposalStatusAsync(
        endpoint: String,
        multisigPda: String,
        transactionIndex: Long
    ): Map<String, Any>? = callAsync("getProposalStatus", mapOf(
        "endpoint" to endpoint, "multisigPda" to multisigPda, "transactionIndex" to transactionIndex
    ))

    // --- Multisig: Create proposal (Send SOL / SPL) ---
    fun multisigSendSOL(
        endpoint: String,
        creatorPrivateKey: String,
        multisigPda: String,
        to: String,
        amountSol: Double,
        vaultIndex: Int? = null,
        transactionIndex: Long? = null,
        completion: (Map<String, Any>?) -> Unit
    ) {
        val p = mutableMapOf<String, Any?>(
            "endpoint" to endpoint,
            "creatorPrivateKey" to creatorPrivateKey,
            "multisigPda" to multisigPda,
            "to" to to,
            "amountSol" to amountSol
        )
        vaultIndex?.let { p["vaultIndex"] = it }
        transactionIndex?.let { p["transactionIndex"] = it }
        call("multisigSendSOL", p, completion)
    }

    suspend fun multisigSendSOLAsync(
        endpoint: String,
        creatorPrivateKey: String,
        multisigPda: String,
        to: String,
        amountSol: Double,
        vaultIndex: Int? = null,
        transactionIndex: Long? = null
    ): Map<String, Any>? {
        val p = mutableMapOf<String, Any?>(
            "endpoint" to endpoint, "creatorPrivateKey" to creatorPrivateKey, "multisigPda" to multisigPda, "to" to to, "amountSol" to amountSol
        )
        vaultIndex?.let { p["vaultIndex"] = it }
        transactionIndex?.let { p["transactionIndex"] = it }
        return callAsync("multisigSendSOL", p)
    }

    fun multisigSendSPLToken(
        endpoint: String,
        creatorPrivateKey: String,
        multisigPda: String,
        mint: String,
        to: String,
        amount: Number,
        decimals: Int? = null,
        createAtaIfMissing: Boolean? = null,
        vaultIndex: Int? = null,
        transactionIndex: Long? = null,
        completion: (Map<String, Any>?) -> Unit
    ) {
        val p = mutableMapOf<String, Any?>(
            "endpoint" to endpoint,
            "creatorPrivateKey" to creatorPrivateKey,
            "multisigPda" to multisigPda,
            "mint" to mint,
            "to" to to,
            "amount" to amount
        )
        decimals?.let { p["decimals"] = it }
        createAtaIfMissing?.let { p["createAtaIfMissing"] = it }
        vaultIndex?.let { p["vaultIndex"] = it }
        transactionIndex?.let { p["transactionIndex"] = it }
        call("multisigSendSPLToken", p, completion)
    }

    suspend fun multisigSendSPLTokenAsync(
        endpoint: String,
        creatorPrivateKey: String,
        multisigPda: String,
        mint: String,
        to: String,
        amount: Number,
        decimals: Int? = null,
        createAtaIfMissing: Boolean? = null,
        vaultIndex: Int? = null,
        transactionIndex: Long? = null
    ): Map<String, Any>? {
        val p = mutableMapOf<String, Any?>(
            "endpoint" to endpoint, "creatorPrivateKey" to creatorPrivateKey, "multisigPda" to multisigPda,
            "mint" to mint, "to" to to, "amount" to amount
        )
        decimals?.let { p["decimals"] = it }
        createAtaIfMissing?.let { p["createAtaIfMissing"] = it }
        vaultIndex?.let { p["vaultIndex"] = it }
        transactionIndex?.let { p["transactionIndex"] = it }
        return callAsync("multisigSendSPLToken", p)
    }
}
