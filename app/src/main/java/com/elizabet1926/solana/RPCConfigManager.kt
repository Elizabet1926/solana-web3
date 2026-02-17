package com.elizabet1926.solana

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Persists RPC endpoint and provides connection check. Preset index: 0=Devnet, 1=Mainnet Beta, 2=Custom.
 */
object RPCConfigManager {

    private const val PREFS_NAME = "solana_rpc_config"
    private const val KEY_PRESET = "solana.rpc.preset"
    private const val KEY_CUSTOM_URL = "solana.rpc.customURL"

    const val PRESET_DEVNET = 0
    const val PRESET_MAINNET_BETA = 1
    const val PRESET_CUSTOM = 2

    const val DEFAULT_DEVNET = "https://api.devnet.solana.com"
    const val DEFAULT_MAINNET = "https://api.mainnet-beta.solana.com"
    const val DEFAULT_CUSTOM_PLACEHOLDER = "https://solana.maiziqianbao.net"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun requirePrefs(): SharedPreferences = prefs ?: error("Call RPCConfigManager.init(context) first")

    var selectedPresetIndex: Int
        get() = requirePrefs().getInt(KEY_PRESET, PRESET_DEVNET).coerceIn(PRESET_DEVNET, PRESET_CUSTOM)
        set(value) = requirePrefs().edit().putInt(KEY_PRESET, value).apply()

    var customURL: String?
        get() {
            val s = requirePrefs().getString(KEY_CUSTOM_URL, null)?.trim()
            return if (s.isNullOrEmpty()) null else s
        }
        set(value) = requirePrefs().edit().putString(KEY_CUSTOM_URL, value).apply()

    val currentEndpoint: String?
        get() {
            when (selectedPresetIndex) {
                PRESET_DEVNET -> return DEFAULT_DEVNET
                PRESET_MAINNET_BETA -> return DEFAULT_MAINNET
                PRESET_CUSTOM -> return customURL
                else -> {
                    selectedPresetIndex = PRESET_DEVNET
                    return DEFAULT_DEVNET
                }
            }
        }

    fun save(presetIndex: Int, customUrl: String?) {
        selectedPresetIndex = presetIndex.coerceIn(PRESET_DEVNET, PRESET_CUSTOM)
        customURL = customUrl
    }

    suspend fun checkConnection(endpoint: String): Result<String> = kotlin.runCatching {
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        try {
            conn.outputStream.use { os ->
                os.write("""{"jsonrpc":"2.0","id":1,"method":"getVersion"}""".toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            if (code == 403) throw RPCException("Access forbidden (403). For mainnet, use a custom RPC (e.g. Helius, QuickNode).")
            if (code != 200) throw RPCException("HTTP $code")
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            if (json.has("error")) {
                val err = json.optJSONObject("error")
                throw RPCException(err?.optString("message", "Unknown RPC error") ?: "Unknown RPC error")
            }
            val result = json.optJSONObject("result") ?: throw RPCException("Invalid RPC response")
            result.optString("solana-core", result.optString("solanaCore", "?"))
        } finally {
            conn.disconnect()
        }
    }

    class RPCException(message: String) : Exception(message)
}
