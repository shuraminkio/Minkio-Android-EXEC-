package com.nebula.executor

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

object ScriptEngine {

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var pendingCallback: ((String) -> Unit)? = null
    private const val BRIDGE_URL = "ws://127.0.0.1:8769"

    fun connect(callback: (String) -> Unit) {
        if (isConnected) {
            callback("[INFO] Already connected to bridge")
            return
        }

        ConsoleManager.appendInfo("Attempting connection to bridge at $BRIDGE_URL…")
        callback("[INFO] Connecting to Roblox bridge…")

        val request = Request.Builder()
            .url(BRIDGE_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                ConsoleManager.appendInfo("Connected to bridge successfully")
                callback("[INFO] Bridge connection established ✓")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                ConsoleManager.appendInfo("Bridge response: $text")
                pendingCallback?.invoke(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                val msg = t.message ?: "Unknown error"
                ConsoleManager.appendWarn("Bridge connection failed: $msg")
                callback("[WARN] Bridge unavailable — using local simulation")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                ConsoleManager.appendInfo("Bridge connection closing: $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                ConsoleManager.appendInfo("Bridge disconnected")
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        isConnected = false
        ConsoleManager.appendInfo("Disconnected from bridge")
    }

    fun execute(script: String, callback: (String) -> Unit) {
        if (script.isBlank()) {
            callback("[WARN] Empty script — nothing to execute")
            ConsoleManager.appendWarn("Attempted to execute empty script")
            return
        }

        val trimmedPreview = if (script.length > 60) script.take(60) + "…" else script
        ConsoleManager.appendExec("Executing: $trimmedPreview")

        if (isConnected && webSocket != null) {
            executeRemote(script, callback)
        } else {
            executeLocal(script, callback)
        }
    }

    private fun executeRemote(script: String, callback: (String) -> Unit) {
        pendingCallback = callback
        val payload = buildString {
            append("{\"type\":\"execute\",\"script\":\"")
            append(script.replace("\"", "\\\"").replace("\n", "\\n"))
            append("\"}")
        }

        val sent = webSocket?.send(payload) ?: false
        if (sent) {
            callback("[EXEC] Script sent to bridge for execution")
            ConsoleManager.appendExec("Script dispatched to remote bridge")
        } else {
            callback("[ERROR] Failed to send script to bridge")
            ConsoleManager.appendError("WebSocket send failed — falling back to local")
            executeLocal(script, callback)
        }
    }

    private fun executeLocal(script: String, callback: (String) -> Unit) {
        callback("[INFO] Running in local simulation mode")
        ConsoleManager.appendInfo("Local simulation mode active")

        val results = mutableListOf<String>()

        // Parse and simulate common Lua patterns
        val lines = script.lines()

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("--") -> {
                    // Comment, skip
                }
                trimmed.startsWith("print(") -> {
                    val content = trimmed.removePrefix("print(").removeSuffix(")")
                        .removeSurrounding("\"").removeSurrounding("'")
                    results.add("[OUT] $content")
                }
                trimmed.startsWith("warn(") -> {
                    val content = trimmed.removePrefix("warn(").removeSuffix(")")
                        .removeSurrounding("\"").removeSurrounding("'")
                    results.add("[WARN] $content")
                }
                trimmed.contains("game:GetService") -> {
                    val service = Regex("GetService\\([\"'](.+?)[\"']\\)").find(trimmed)
                    val svcName = service?.groupValues?.getOrNull(1) ?: "Unknown"
                    results.add("[SIM] Acquired service: $svcName")
                }
                trimmed.contains("loadstring") -> {
                    results.add("[SIM] loadstring() intercepted — remote payload queued")
                }
                trimmed.contains("Instance.new") -> {
                    val cls = Regex("Instance\\.new\\([\"'](.+?)[\"']").find(trimmed)
                    val className = cls?.groupValues?.getOrNull(1) ?: "Instance"
                    results.add("[SIM] Created instance: $className")
                }
                trimmed.contains("Humanoid") && trimmed.contains("WalkSpeed") -> {
                    val speed = Regex("=\\s*(\\d+)").find(trimmed)
                    val spd = speed?.groupValues?.getOrNull(1) ?: "?"
                    results.add("[SIM] WalkSpeed set to $spd")
                }
                trimmed.contains("Humanoid") && trimmed.contains("JumpPower") -> {
                    val power = Regex("=\\s*(\\d+)").find(trimmed)
                    val pwr = power?.groupValues?.getOrNull(1) ?: "?"
                    results.add("[SIM] JumpPower set to $pwr")
                }
                trimmed.contains("Humanoid") && trimmed.contains("Health") -> {
                    results.add("[SIM] Health property modified")
                }
                trimmed.contains("CFrame") -> {
                    results.add("[SIM] CFrame transformation applied")
                }
                trimmed.contains("Transparency") -> {
                    results.add("[SIM] Transparency modified")
                }
                trimmed.contains(".Enabled") -> {
                    results.add("[SIM] UI element toggled")
                }
                trimmed.contains("BodyVelocity") || trimmed.contains("BodyGyro") -> {
                    results.add("[SIM] Physics body injected")
                }
                trimmed.contains("mouse") || trimmed.contains("Mouse") -> {
                    results.add("[SIM] Mouse input hooked")
                }
                trimmed.contains("UserInputService") -> {
                    results.add("[SIM] Input service accessed")
                }
                trimmed.contains("RunService") -> {
                    results.add("[SIM] RunService connected")
                }
                trimmed.contains("Stepped") || trimmed.contains("Heartbeat") ||
                    trimmed.contains("RenderStepped") -> {
                    results.add("[SIM] Frame loop bound")
                }
                trimmed.startsWith("local ") -> {
                    val varName = trimmed.removePrefix("local ").split("=", " ").first().trim()
                    results.add("[SIM] Declared: $varName")
                }
                trimmed.startsWith("for ") || trimmed.startsWith("while ") -> {
                    results.add("[SIM] Loop structure parsed")
                }
                trimmed.startsWith("function ") -> {
                    val funcName = trimmed.removePrefix("function ").split("(").first().trim()
                    results.add("[SIM] Function defined: $funcName")
                }
                trimmed == "end" || trimmed == ")" || trimmed.isEmpty() -> {
                    // structural, skip
                }
                else -> {
                    if (trimmed.isNotBlank()) {
                        results.add("[SIM] Processed: ${trimmed.take(50)}")
                    }
                }
            }
        }

        if (results.isEmpty()) {
            results.add("[INFO] Script parsed — no simulatable output")
        }

        results.add("[INFO] Execution completed ✓")

        for (r in results) {
            when {
                r.startsWith("[ERROR]") -> ConsoleManager.appendError(r.removePrefix("[ERROR] "))
                r.startsWith("[WARN]") -> ConsoleManager.appendWarn(r.removePrefix("[WARN] "))
                r.startsWith("[EXEC]") || r.startsWith("[SIM]") ->
                    ConsoleManager.appendExec(r.removePrefix("[EXEC] ").removePrefix("[SIM] "))
                r.startsWith("[OUT]") -> ConsoleManager.appendInfo(r.removePrefix("[OUT] "))
                else -> ConsoleManager.appendInfo(r.removePrefix("[INFO] "))
            }
        }

        callback(results.joinToString("\n"))
    }

    fun isConnected(): Boolean = isConnected
}
