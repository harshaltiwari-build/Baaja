/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.deviceconnect

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.Player
import com.metrolist.music.extensions.metadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import timber.log.Timber
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.Executors

/** Let other phones on the same Wi-Fi control this one (default off). */
val DeviceConnectEnabledKey = booleanPreferencesKey("baajaDeviceConnectEnabled")

/** The 6-digit code another phone must enter to control this one. */
val DeviceConnectCodeKey = stringPreferencesKey("baajaDeviceConnectCode")

/** How phones find each other on the network, and the messages they exchange. */
object DeviceConnectProtocol {
    const val SERVICE_TYPE = "_baaja._tcp."
    val json = Json { ignoreUnknownKeys = true }

    fun newCode(): String = (SecureRandom().nextInt(900_000) + 100_000).toString()

    /** The code is never sent in the clear: the controller proves it knows it for this connection's nonce. */
    fun proof(
        code: String,
        nonce: String,
    ): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest("$nonce:$code".toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull

    fun JsonObject.long(key: String) = this[key]?.jsonPrimitive?.longOrNull

    fun BufferedWriter.send(message: JsonObject) {
        write(message.toString())
        newLine()
        flush()
    }

    private const val MAX_LINE = 64 * 1024

    /** One message; a line longer than any real message ends the connection instead of filling memory. */
    fun BufferedReader.receive(): JsonObject? {
        val line = StringBuilder()
        while (true) {
            val c = read()
            if (c == -1) return null
            if (c == '\n'.code) break
            check(line.length < MAX_LINE) { "Message too long" }
            line.append(c.toChar())
        }
        return runCatching { json.parseToJsonElement(line.toString()).jsonObject }.getOrNull()
    }
}

/**
 * Runs on the phone being controlled: advertises itself, checks the pairing code, then streams what's
 * playing and carries out play, pause, skip and seek on the main thread.
 */
class DeviceConnectServer(
    private val context: Context,
    private val player: Player,
    private val code: () -> String,
) {
    // Its own few threads, so a stuck or hostile connection can't starve the app's shared IO pool.
    private val executor = Executors.newFixedThreadPool(MAX_CLIENTS + 2)
    private val scope = CoroutineScope(SupervisorJob() + executor.asCoroutineDispatcher())
    private var serverSocket: ServerSocket? = null
    private var registration: NsdManager.RegistrationListener? = null
    private val clients = mutableSetOf<Socket>()
    private val handshake = Mutex()
    private var failedAttempts = 0
    private var lockedUntil = 0L

    fun start() {
        if (serverSocket != null) return
        val socket = ServerSocket(0, 4, null)
        serverSocket = socket
        register(socket.localPort)
        scope.launch {
            while (isActive) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                if (synchronized(clients) { clients.size } >= MAX_CLIENTS) {
                    runCatching { client.close() }
                    continue
                }
                launch { handle(client) }
            }
        }
    }

    fun stop() {
        registration?.let { listener -> runCatching { context.getSystemService<NsdManager>()?.unregisterService(listener) } }
        registration = null
        registeredName = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        synchronized(clients) { clients.forEach { runCatching { it.close() } } }
        scope.cancel()
        executor.shutdownNow()
    }

    private fun register(port: Int) {
        val info =
            NsdServiceInfo().apply {
                serviceName = "Baaja on ${Build.MODEL}"
                serviceType = DeviceConnectProtocol.SERVICE_TYPE
                setPort(port)
            }
        val listener =
            object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(info: NsdServiceInfo) {
                    // The network may rename us (e.g. "(2)") when another phone has the same model.
                    registeredName = info.serviceName
                    Timber.tag(TAG).d("Advertising as %s", info.serviceName)
                }

                override fun onRegistrationFailed(
                    info: NsdServiceInfo,
                    errorCode: Int,
                ) = Timber.tag(TAG).w("Advertising failed: %d", errorCode)

                override fun onServiceUnregistered(info: NsdServiceInfo) = Unit

                override fun onUnregistrationFailed(
                    info: NsdServiceInfo,
                    errorCode: Int,
                ) = Unit
            }
        registration = listener
        context.getSystemService<NsdManager>()?.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private suspend fun handle(socket: Socket) {
        synchronized(clients) { clients += socket }
        try {
            // A controller has a few seconds to answer the challenge, and only one is checked at a time.
            socket.soTimeout = HANDSHAKE_TIMEOUT_MS
            val reader = socket.getInputStream().bufferedReader()
            val writer = socket.getOutputStream().bufferedWriter()
            with(DeviceConnectProtocol) {
                val accepted =
                    handshake.withLock {
                        val nonce = newCode() + System.nanoTime()
                        writer.send(buildJsonObject { put("type", "challenge"); put("nonce", nonce); put("name", "Baaja on ${Build.MODEL}") })
                        val hello = reader.receive() ?: return
                        val expected = code()
                        val ok =
                            System.currentTimeMillis() >= lockedUntil &&
                                expected.isNotBlank() &&
                                hello.str("type") == "hello" &&
                                hello.str("proof") == proof(expected, nonce)
                        if (ok) {
                            failedAttempts = 0
                        } else {
                            // Slow every wrong guess, and after a few in a row refuse all tries for a minute.
                            if (++failedAttempts >= MAX_FAILED_ATTEMPTS) {
                                lockedUntil = System.currentTimeMillis() + LOCKOUT_MS
                                failedAttempts = 0
                            }
                            delay(WRONG_CODE_DELAY_MS)
                            writer.send(buildJsonObject { put("type", "error"); put("reason", "code") })
                        }
                        ok
                    }
                if (!accepted) return
                socket.soTimeout = 0
                writer.send(buildJsonObject { put("type", "welcome") })
                val updates = scope.launch { streamState(socket, writer) }
                try {
                    while (true) {
                        val command = reader.receive() ?: break
                        withContext(Dispatchers.Main) { apply(command) }
                    }
                } finally {
                    updates.cancel()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "Controller disconnected")
        } finally {
            synchronized(clients) { clients -= socket }
            runCatching { socket.close() }
        }
    }

    private suspend fun streamState(
        socket: Socket,
        writer: BufferedWriter,
    ) {
        var last: String? = null
        while (true) {
            val state = withContext(Dispatchers.Main) { snapshot() }
            val text = state.toString()
            // Position moves every second while playing, so send then; otherwise only on changes.
            if (text != last) {
                // The controller hung up: close the socket so the command loop ends too.
                val sent = runCatching { with(DeviceConnectProtocol) { writer.send(state) } }.isSuccess
                if (!sent) {
                    runCatching { socket.close() }
                    return
                }
                last = text
            }
            delay(1_000)
        }
    }

    private fun snapshot(): JsonObject {
        val item = player.currentMediaItem
        val metadata = item?.metadata
        return buildJsonObject {
            put("type", "state")
            put("mediaId", item?.mediaId.orEmpty())
            put("title", metadata?.title.orEmpty())
            put("artist", metadata?.artists?.joinToString { it.name }.orEmpty())
            put("thumbnail", metadata?.thumbnailUrl.orEmpty())
            put("playing", player.playWhenReady)
            put("position", player.currentPosition.coerceAtLeast(0))
            put("duration", player.duration.takeIf { it > 0 } ?: 0)
        }
    }

    private fun apply(command: JsonObject) =
        with(DeviceConnectProtocol) {
            when (command.str("type")) {
                "play" -> player.play()
                "pause" -> player.pause()
                "next" -> if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                "previous" -> if (player.currentPosition > 3_000 || !player.hasPreviousMediaItem()) player.seekTo(0) else player.seekToPreviousMediaItem()
                "seek" -> command.long("position")?.let { player.seekTo(it.coerceAtLeast(0)) }
                else -> Unit
            }
        }

    companion object {
        /** The name this phone is advertised under, so it can leave itself out of the device list. */
        @Volatile
        var registeredName: String? = null
            private set

        private const val TAG = "DeviceConnect"
        private const val MAX_CLIENTS = 4
        private const val HANDSHAKE_TIMEOUT_MS = 5_000
        private const val WRONG_CODE_DELAY_MS = 1_500L
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_MS = 60_000L
    }
}
