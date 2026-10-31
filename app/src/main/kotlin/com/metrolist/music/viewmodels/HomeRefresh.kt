package com.metrolist.music.viewmodels

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * When Home reloads by itself, like YouTube Music: after [STALE_MS] away, and when the signed-in
 * account changes. Refreshes never overlap; asking during one queues a single follow-up run, so
 * signing out and quickly back in still ends on the right account's feed.
 */
internal class HomeRefresh(private val now: () -> Long = System::currentTimeMillis) {
    private var loadedAt = 0L
    private var knownCookie: String? = null
    private var running = false
    private var again = false

    @Synchronized
    fun markLoaded() {
        loadedAt = now()
    }

    @Synchronized
    fun isStale(): Boolean = loadedAt > 0 && now() - loadedAt > STALE_MS

    /** True when [cookie] belongs to a different account (or none) than the last one seen after Home loaded. */
    @Synchronized
    fun accountChanged(cookie: String?): Boolean {
        val previous = knownCookie
        knownCookie = cookie.orEmpty()
        return loadedAt > 0 && previous != null && previous != knownCookie
    }

    /**
     * Runs [block] in [scope] unless a refresh is already running, in which case it is queued and null is
     * returned. [onStart] and [onIdle] are called under the same lock as that decision, so a "refreshing"
     * flag driven by them can't be switched off by a run that just ended while the next one starts.
     */
    fun run(
        scope: CoroutineScope,
        onStart: () -> Unit = {},
        onIdle: () -> Unit,
        onError: (Throwable) -> Unit = {},
        block: suspend () -> Unit,
    ): Job? {
        synchronized(this) {
            if (running) {
                again = true
                return null
            }
            running = true
            onStart()
        }
        return scope.launch {
            var finished = false
            try {
                do {
                    try {
                        block()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        onError(e)
                    }
                } while (continueOrStop(onIdle))
                finished = true
            } finally {
                if (!finished) stop(onIdle)
            }
        }
    }

    @Synchronized
    private fun continueOrStop(onIdle: () -> Unit): Boolean {
        if (again) {
            again = false
            return true
        }
        running = false
        onIdle()
        return false
    }

    @Synchronized
    private fun stop(onIdle: () -> Unit) {
        running = false
        again = false
        onIdle()
    }

    companion object {
        const val STALE_MS = 30 * 60 * 1000L
    }
}
