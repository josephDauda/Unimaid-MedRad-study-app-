package com.example.data.supabase

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

class SupabaseRealtimeManager(
  private val context: Context,
  private val okHttpClient: OkHttpClient,
  private val onRealtimeEvent: (table: String, eventType: String, record: JSONObject) -> Unit
) {

  private val tag = "SupabaseRealtime"
  private var webSocket: WebSocket? = null
  private val refCounter = AtomicInteger(1)
  private val scope = CoroutineScope(Dispatchers.IO)
  private var heartbeatJob: Job? = null
  private var reconnectJob: Job? = null
  private var isExplicitlyStopped = false
  var isConnected = false
    private set

  private val tablesToListen = listOf(
    "announcements",
    "app_config",
    "study_materials",
    "course_libraries",
    "courses",
    "exams",
    "questions",
    "student_feedback"
  )

  fun start() {
    if (isConnected && webSocket != null) return

    val baseUrl = SupabaseConfig.getUrl(context)
      .replace("https://", "wss://")
      .replace("http://", "ws://")
    val apiKey = SupabaseConfig.getPublishableKey(context)
    val wsUrl = "$baseUrl/realtime/v1/websocket?apikey=$apiKey&vsn=1.0.0"

    val request = Request.Builder().url(wsUrl).build()
    webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(ws: WebSocket, response: Response) {
        Log.i(tag, "Supabase Realtime WebSocket connected to $baseUrl")
        isConnected = true
        startHeartbeat()
        subscribeToChannels(ws)
      }

      override fun onMessage(ws: WebSocket, text: String) {
        handleIncomingMessage(text)
      }

      override fun onClosing(ws: WebSocket, code: Int, reason: String) {
        isConnected = false
        Log.w(tag, "Supabase Realtime closing: $code / $reason")
      }

      override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
        isConnected = false
        Log.w(tag, "Supabase Realtime error: ${t.message}. Reconnecting in 5s...")
        stopHeartbeat()
        scheduleReconnect()
      }

      override fun onClosed(ws: WebSocket, code: Int, reason: String) {
        isConnected = false
        stopHeartbeat()
        scheduleReconnect()
      }
    })
  }

  private fun subscribeToChannels(ws: WebSocket) {
    for (table in tablesToListen) {
      val ref = refCounter.incrementAndGet().toString()
      val topic = "realtime:public:$table"

      val postgresChanges = JSONArray().apply {
        put(JSONObject().apply {
          put("event", "*")
          put("schema", "public")
          put("table", table)
        })
      }

      val payload = JSONObject().apply {
        put("config", JSONObject().apply {
          put("postgres_changes", postgresChanges)
        })
      }

      val joinMessage = JSONObject().apply {
        put("topic", topic)
        put("event", "phx_join")
        put("payload", payload)
        put("ref", ref)
      }

      ws.send(joinMessage.toString())
    }
  }

  private fun startHeartbeat() {
    stopHeartbeat()
    heartbeatJob = scope.launch {
      while (isActive && isConnected) {
        delay(25000L)
        val heartbeat = JSONObject().apply {
          put("topic", "phoenix")
          put("event", "heartbeat")
          put("payload", JSONObject())
          put("ref", refCounter.incrementAndGet().toString())
        }
        webSocket?.send(heartbeat.toString())
      }
    }
  }

  private fun stopHeartbeat() {
    heartbeatJob?.cancel()
    heartbeatJob = null
  }

  private fun scheduleReconnect() {
    scope.launch {
      delay(5000L)
      if (!isConnected) {
        Log.i(tag, "Attempting Supabase Realtime reconnect...")
        start()
      }
    }
  }

  fun dispatchRealtimeEvent(table: String, eventType: String, record: JSONObject) {
    try {
      onRealtimeEvent(table, eventType, record)
    } catch (e: Exception) {
      Log.e(tag, "Error handling local realtime event callback: ${e.message}")
    }
    scope.launch {
      try {
        if (isConnected && webSocket != null) {
          val topic = "realtime:public:$table"
          val broadcastMsg = JSONObject().apply {
            put("topic", topic)
            put("event", "broadcast")
            put("payload", JSONObject().apply {
              put("table", table)
              put("type", eventType)
              put("record", record)
            })
            put("ref", refCounter.incrementAndGet().toString())
          }
          webSocket?.send(broadcastMsg.toString())
        }
      } catch (e: Exception) {
        Log.e(tag, "Error dispatching realtime event over websocket: ${e.message}")
      }
    }
  }

  private fun handleIncomingMessage(text: String) {
    try {
      val json = JSONObject(text)
      val event = json.optString("event")
      val topic = json.optString("topic")

      if (event == "postgres_changes" || event == "broadcast") {
        val payload = json.optJSONObject("payload") ?: return
        val data = payload.optJSONObject("data") ?: payload
        var table = data.optString("table")
        if (table.isBlank() && topic.startsWith("realtime:public:")) {
          table = topic.substringAfterLast(":")
        }
        val type = data.optString("type", if (event == "broadcast") "UPDATE" else "UNKNOWN")
        val record = data.optJSONObject("record")
          ?: payload.optJSONObject("record")
          ?: data.optJSONObject("old_record")
          ?: payload.optJSONObject("old_record")
          ?: data

        if (table.isNotBlank()) {
          onRealtimeEvent(table, type, record)
        }
      }
    } catch (e: Exception) {
      Log.e(tag, "Error parsing realtime message: ${e.message}")
    }
  }

  fun stop() {
    stopHeartbeat()
    webSocket?.close(1000, "App paused or destroyed")
    webSocket = null
    isConnected = false
  }
}
