/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService

/** Broad family of an output, used for icons and labels. */
enum class AudioOutputKind { SPEAKER, WIRED, USB, BLUETOOTH, HDMI }

/**
 * One output the user can pick.
 *
 * [key] is what gets persisted. `AudioDeviceInfo.getId()` is reassigned by the audio policy every
 * time a device reconnects (and after reboots), so it can't be stored. Type + address + product
 * name survives reconnects: the address is the Bluetooth MAC / USB card for external devices and
 * empty for built-ins, where the type alone is unique.
 */
data class AudioOutputDevice(
    val id: Int,
    val type: Int,
    val address: String,
    val productName: String,
) {
    val key: String get() = audioOutputKey(type, address, productName)
    val kind: AudioOutputKind? get() = audioOutputKind(type)
}

fun audioOutputKey(type: Int, address: String, productName: String): String =
    "$type|${address.trim()}|${productName.trim()}"

/** The product name stored inside a key, for showing a remembered device that isn't connected. */
fun audioOutputKeyName(key: String): String = key.split('|', limit = 3).getOrNull(2).orEmpty()

fun audioOutputKind(type: Int): AudioOutputKind? =
    when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioOutputKind.SPEAKER
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        -> AudioOutputKind.WIRED
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        -> AudioOutputKind.USB
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        TYPE_BLE_HEADSET,
        TYPE_BLE_SPEAKER,
        -> AudioOutputKind.BLUETOOTH
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_HDMI_ARC,
        TYPE_HDMI_EARC,
        -> AudioOutputKind.HDMI
        else -> null
    }

/**
 * Keeps only outputs that make sense for music (no earpiece, telephony, SCO, bus or remix
 * devices) and collapses duplicates: one physical device often shows up several times
 * (for example a USB DAC exposing multiple profiles).
 */
fun selectableAudioOutputs(devices: List<AudioOutputDevice>): List<AudioOutputDevice> =
    devices
        .filter { it.kind != null }
        .distinctBy { it.key }
        .sortedWith(compareBy<AudioOutputDevice> { it.kind!!.ordinal }.thenBy { it.productName })

/** The connected device matching a saved key, or null for Automatic / not connected. */
fun resolveAudioOutput(savedKey: String?, connected: List<AudioOutputDevice>): AudioOutputDevice? {
    if (savedKey.isNullOrBlank()) return null
    return connected.firstOrNull { it.key == savedKey }
}

fun AudioManager.connectedAudioOutputs(): List<AudioOutputDevice> =
    selectableAudioOutputs(
        getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { info ->
            AudioOutputDevice(
                id = info.id,
                type = info.type,
                address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.address.orEmpty() else "",
                productName = info.productName?.toString().orEmpty(),
            )
        },
    )

fun AudioManager.findOutputDeviceInfo(id: Int): AudioDeviceInfo? =
    getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.id == id }

/**
 * Watches connected outputs and reports the [AudioDeviceInfo] players should prefer, or null to
 * let Android route ("Automatic"). When the chosen device disconnects this falls back to null and
 * re-applies the choice automatically if it comes back.
 */
class AudioOutputRouter(
    context: Context,
    private val onPreferredDeviceChanged: (AudioDeviceInfo?) -> Unit,
) {
    private val audioManager = context.getSystemService<AudioManager>()!!
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    var savedKey: String? = null
        private set

    var preferredDevice: AudioDeviceInfo? = null
        private set

    private val callback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refresh()

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refresh()
        }

    fun start(initialKey: String?) {
        savedKey = initialKey
        audioManager.registerAudioDeviceCallback(callback, handler)
        refresh()
    }

    fun stop() {
        audioManager.unregisterAudioDeviceCallback(callback)
    }

    fun setSavedKey(key: String?) {
        savedKey = key?.takeIf { it.isNotBlank() }
        refresh()
    }

    private fun refresh() {
        val match = resolveAudioOutput(savedKey, audioManager.connectedAudioOutputs())
        val info = match?.let { audioManager.findOutputDeviceInfo(it.id) }
        if (info?.id == preferredDevice?.id) return
        preferredDevice = info
        onPreferredDeviceChanged(info)
    }
}

// Constants added after minSdk 26; values are fixed by the platform.
private const val TYPE_BLE_HEADSET = 26
private const val TYPE_BLE_SPEAKER = 27
private const val TYPE_HDMI_EARC = 29
