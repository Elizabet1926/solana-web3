package com.elizabet1926.solanaweb.bridge

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject

/** WebViewJavascriptBridgeBase - message handling, callbacks, JSON serialization */
typealias BridgeCallback = (Any?) -> Unit
typealias BridgeHandler = (Map<String, Any>?, BridgeCallback?) -> Unit
typealias BridgeMessage = Map<String, Any>
typealias EvaluateJavascript = (String, ((String?) -> Unit)?) -> Unit

class WebViewJavascriptBridgeBase {

    private var responseCallbacks = mutableMapOf<String, BridgeCallback>()
    private var messageHandlers = mutableMapOf<String, BridgeHandler>()
    private var uniqueId = 0
    private var evaluateJavascript: EvaluateJavascript? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun setEvaluateJavascript(evaluator: EvaluateJavascript) {
        this.evaluateJavascript = evaluator
    }

    fun reset() {
        responseCallbacks.clear()
        uniqueId = 0
    }

    fun send(handlerName: String, data: Any?, callback: BridgeCallback?) {
        val message = mutableMapOf<String, Any>()
        message["handlerName"] = handlerName
        if (data != null) message["data"] = data
        if (callback != null) {
            uniqueId += 1
            val callbackID = "android_cb_$uniqueId"
            responseCallbacks[callbackID] = callback
            message["callbackId"] = callbackID
        }
        dispatch(message)
    }

    fun flush(messageQueueString: String) {
        val message = deserialize(messageQueueString) ?: return
        if (message.containsKey("responseId")) {
            val responseID = message["responseId"] as? String ?: return
            val callback = responseCallbacks[responseID] ?: return
            callback(message["responseData"])
            responseCallbacks.remove(responseID)
        } else {
            val callback: BridgeCallback? = if (message.containsKey("callbackId")) {
                val callbackID = message["callbackId"]
                { responseData ->
                    dispatch(mapOf("responseId" to callbackID!!, "responseData" to (responseData ?: JSONObject.NULL)))
                }
            } else {
                { _ -> }
            }
            val handlerName = message["handlerName"] as? String ?: return
            val handler = messageHandlers[handlerName] ?: run {
                println("NoHandlerException, No handler for message from JS: $message")
                return
            }
            handler(message["data"] as? Map<String, Any>, callback)
        }
    }

    private fun dispatch(message: BridgeMessage) {
        val messageJSON = serialize(message, false) ?: return
        val escapedJSON = messageJSON
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\u000C", "\\f")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")
        val javascriptCommand = "WebViewJavascriptBridge.handleMessageFromNative('$escapedJSON');"
        if (Looper.myLooper() == Looper.getMainLooper()) {
            evaluateJavascript?.invoke(javascriptCommand, null)
        } else {
            mainHandler.post { evaluateJavascript?.invoke(javascriptCommand, null) }
        }
    }

    fun register(handlerName: String, handler: BridgeHandler) {
        messageHandlers[handlerName] = handler
    }

    fun remove(handlerName: String): BridgeHandler? = messageHandlers.remove(handlerName)

    private fun serialize(message: BridgeMessage, pretty: Boolean): String? = try {
        val jsonObject = JSONObject()
        message.forEach { (key, value) -> jsonObject.put(key, toJSONValue(value)) }
        jsonObject.toString(if (pretty) 2 else 0)
    } catch (e: Exception) {
        println("Serialization error: $e")
        null
    }

    private fun deserialize(messageJSON: String): BridgeMessage? = try {
        val jsonObject = JSONObject(messageJSON)
        val map = mutableMapOf<String, Any>()
        jsonObject.keys().forEach { key ->
            val value = fromJSONValue(jsonObject.get(key))
            map[key] = value ?: JSONObject.NULL
        }
        map
    } catch (e: Exception) {
        println("Deserialization error: $e")
        null
    }

    private fun toJSONValue(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Boolean, is Int, is Long, is Double, is Float, is String -> value
        is Map<*, *> -> JSONObject().apply { (value as Map<String, *>).forEach { (k, v) -> put(k, toJSONValue(v)) } }
        is List<*> -> JSONArray().apply { value.forEach { put(toJSONValue(it)) } }
        is Array<*> -> JSONArray().apply { for (item in value) put(toJSONValue(item)) }
        else -> value.toString()
    }

    private fun fromJSONValue(value: Any?): Any? = when (value) {
        JSONObject.NULL -> null
        is JSONObject -> mutableMapOf<String, Any>().apply {
            value.keys().forEach { key ->
                put(key, fromJSONValue(value.get(key)) ?: JSONObject.NULL)
            }
        }
        is JSONArray -> mutableListOf<Any>().apply {
            for (i in 0 until value.length()) add(fromJSONValue(value.get(i)) ?: JSONObject.NULL)
        }
        else -> value ?: Unit
    }
}
