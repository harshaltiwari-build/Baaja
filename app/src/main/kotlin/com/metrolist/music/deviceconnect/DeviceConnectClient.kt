/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.deviceconnect

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import androidx.core.content.getSystemService
import com.metrolist.music.deviceconnect.DeviceConnectProtocol.long
import com.metrolist.music.deviceconnect.DeviceConnectProtocol.receive
import com.metrolist.music.deviceconnect.DeviceConnectProtocol.send
import com.metrolist.music.deviceconnect.DeviceConnectProtocol.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import java.io.BufferedWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

/** A Baaja phone found on the network. */
data class RemoteDevice(
    val name: String,
    val host: String,
    val port: Int,
)

/** What the controlled phone is playing. */
data class RemotePlayback(
    val mediaId: String,
    val title: String,
    val artist: String,
    val thumbnail: String,
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    /** When this state arrived, so the position can keep ticking between updates. */
    val receivedAt: Long = System.currentTimeMillis(),
) {
    fun currentPosition(): Long =
        if (playing) (positionMs + System.currentTimeMillis() - receivedAt).coerceAtMost(durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE) else positionMs
}

/** Finds other Baaja phones on the Wi-Fi and controls one of them. */
class DeviceConnectClient(
    context: Context,
) {
    sealed interface Connection {
        data object Idle : Connection

        data class Connecting(val device: RemoteDevice) : Connection

        data class Connected(val device: RemoteDevice) : Connection

        data object WrongCode : Connection

        data object Failed : Connection
    }

    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService<NsdManager>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _devices = MutableStateFlow<List<RemoteDevice>>(emptyList())
    val devices: StateFlow<List<RemoteDevice>> = _devices.asStateFlow()

    private val _connection = MutableStateFlow<Connection>(Connection.Idle)
    val connection: StateFlow<Connection> = _connection.asStateFlow()

    private val _playback = MutableStateFlow<RemotePlayback?>(null)
    val playback: StateFlow<RemotePlayback?> = _playback.asStateFlow()

    private var discovery: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    @Volatile private var socket: Socket? = null
    @Volatile private var writer: BufferedWriter? = null
    private var connectJob: Job? = null

    // Each connect() starts a new session; work left over from an older one must not touch the state.
    private val session = AtomicInteger()

    // NsdManager handles one resolve at a time, so found services wait their turn.
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    fun startDiscovery() {
        if (discovery != null || nsd == null) return
        multicastLock =
            appContext.getSystemService<WifiManager>()?.createMulticastLock("baaja-device-connect")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        val listener =
            object : NsdManager.DiscoveryListener {
                override fun onServiceFound(info: NsdServiceInfo) {
                    if (info.serviceName == DeviceConnectServer.registeredName) return
                    enqueueResolve(info)
                }

                override fun onServiceLost(info: NsdServiceInfo) {
                    _devices.value = _devices.value.filterNot { it.name == info.serviceName }
                }

                override fun onDiscoveryStarted(serviceType: String) = Unit

                override fun onDiscoveryStopped(serviceType: String) = Unit

                override fun onStartDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int,
                ) = Timber.tag(TAG).w("Discovery failed: %d", errorCode)

                override fun onStopDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int,
                ) = Unit
            }
        discovery = listener
        nsd.discoverServices(DeviceConnectProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun enqueueResolve(info: NsdServiceInfo) {
        val next =
            synchronized(resolveQueue) {
                if (resolveQueue.none { it.serviceName == info.serviceName }) resolveQueue.addLast(info)
                if (resolving) return
                resolving = true
                resolveQueue.removeFirst()
            }
        resolve(next)
    }

    private fun resolveNext() {
        val next =
            synchronized(resolveQueue) {
                val item = resolveQueue.removeFirstOrNull()
                resolving = item != null
                item
            } ?: return
        resolve(next)
    }

    @Suppress("DEPRECATION")
    private fun resolve(info: NsdServiceInfo) {
        val manager = nsd ?: return
        manager.resolveService(
            info,
            object : NsdManager.ResolveListener {
                override fun onServiceResolved(resolved: NsdServiceInfo) {
                    val host = resolved.host?.hostAddress
                    if (host != null && resolved.serviceName != DeviceConnectServer.registeredName) {
                        val device = RemoteDevice(resolved.serviceName, host, resolved.port)
                        _devices.value = (_devices.value.filterNot { it.name == device.name } + device).sortedBy { it.name }
                    }
                    resolveNext()
                }

                override fun onResolveFailed(
                    info: NsdServiceInfo,
                    errorCode: Int,
                ) {
                    Timber.tag(TAG).d("Resolving %s failed: %d", info.serviceName, errorCode)
                    resolveNext()
                }
            },
        )
    }

    fun stopDiscovery() {
        discovery?.let { runCatching { nsd?.stopServiceDiscovery(it) } }
        discovery = null
        synchronized(resolveQueue) {
            resolveQueue.clear()
            resolving = false
        }
        multicastLock?.let { if (it.isHeld) it.release() }
        multicastLock = null
    }

    fun connect(
        device: RemoteDevice,
        code: String,
    ) {
        disconnect()
        val id = session.incrementAndGet()
        fun current() = session.get() == id
        _connection.value = Connection.Connecting(device)
        // Kept before connecting so disconnect() can abort a connection that's still being made.
        val socket = Socket()
        this.socket = socket
        connectJob =
            scope.launch {
                try {
                    socket.connect(InetSocketAddress(device.host, device.port), 5_000)
                    val reader = socket.getInputStream().bufferedReader()
                    val out = socket.getOutputStream().bufferedWriter()
                    val challenge = reader.receive()
                    val nonce = challenge?.str("nonce") ?: error("No challenge")
                    out.send(buildJsonObject { put("type", "hello"); put("proof", DeviceConnectProtocol.proof(code.trim(), nonce)) })
                    val answer = reader.receive()
                    ensureActive()
                    if (answer?.str("type") != "welcome") {
                        if (current()) _connection.value = Connection.WrongCode
                        socket.close()
                        return@launch
                    }
                    if (!current()) return@launch
                    writer = out
                    _connection.value = Connection.Connected(device)
                    while (current()) {
                        val message = reader.receive() ?: break
                        if (message.str("type") == "state" && current()) {
                            _playback.value =
                                RemotePlayback(
                                    mediaId = message.str("mediaId").orEmpty(),
                                    title = message.str("title").orEmpty(),
                                    artist = message.str("artist").orEmpty(),
                                    thumbnail = message.str("thumbnail").orEmpty(),
                                    playing = message.str("playing") == "true",
                                    positionMs = message.long("position") ?: 0,
                                    durationMs = message.long("duration") ?: 0,
                                )
                        }
                    }
                    if (current() && _connection.value is Connection.Connected) _connection.value = Connection.Failed
                } catch (e: Exception) {
                    Timber.tag(TAG).d(e, "Connection to %s ended", device.name)
                    if (current() && _connection.value !is Connection.Idle) _connection.value = Connection.Failed
                } finally {
                    runCatching { socket.close() }
                    if (current()) {
                        writer = null
                        _playback.value = null
                    }
                }
            }
    }

    private fun command(
        type: String,
        position: Long? = null,
    ) {
        val out = writer ?: return
        scope.launch {
            // Commands can overlap (seek dragging, quick taps); one at a time keeps each line whole.
            runCatching { synchronized(out) { out.send(buildJsonObject { put("type", type); position?.let { put("position", it) } }) } }
        }
    }

    fun play() = command("play")

    fun pause() = command("pause")

    fun next() = command("next")

    fun previous() = command("previous")

    fun seekTo(positionMs: Long) = command("seek", positionMs)

    fun disconnect() {
        session.incrementAndGet()
        connectJob?.cancel()
        connectJob = null
        runCatching { socket?.close() }
        socket = null
        writer = null
        _playback.value = null
        _connection.value = Connection.Idle
    }

    fun release() {
        disconnect()
        stopDiscovery()
    }

    private companion object {
        const val TAG = "DeviceConnect"
    }
}
