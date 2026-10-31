package com.metrolist.music.viewmodels

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.plus
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** When Home reloads by itself, like YouTube Music does. */
class HomeRefreshTest {
    private var now = 1_000_000L
    private val refresh = HomeRefresh(now = { now })

    // ---- unit: staleness ----

    @Test
    fun `nothing is stale before the first load`() {
        assertFalse(refresh.isStale())
    }

    @Test
    fun `home goes stale only after thirty minutes`() {
        refresh.markLoaded()
        now += 30 * 60 * 1000L
        assertFalse(refresh.isStale())
        now += 1
        assertTrue(refresh.isStale())
        refresh.markLoaded()
        assertFalse(refresh.isStale())
    }

    // ---- unit: account changes ----

    @Test
    fun `the first account seen is not a change`() {
        assertFalse(refresh.accountChanged("SAPISID=a"))
        refresh.markLoaded()
        assertFalse(refresh.accountChanged("SAPISID=a"))
    }

    @Test
    fun `signing in, out or switching after the first load is a change`() {
        refresh.accountChanged(null)
        refresh.markLoaded()
        assertTrue("sign in", refresh.accountChanged("SAPISID=a"))
        assertTrue("switch", refresh.accountChanged("SAPISID=b"))
        assertTrue("sign out", refresh.accountChanged(null))
        assertFalse("same again", refresh.accountChanged(""))
    }

    @Test
    fun `an account change before anything loaded does not refresh`() {
        refresh.accountChanged(null)
        assertFalse(refresh.accountChanged("SAPISID=a"))
    }

    // ---- integration: refreshes running on real coroutines ----

    @Test
    fun `a refresh asked for while one runs runs right after, once`() =
        runBlocking {
            val runs = AtomicInteger()
            val firstStarted = CompletableDeferred<Unit>()
            val releaseFirst = CompletableDeferred<Unit>()
            val idle = CompletableDeferred<Unit>()
            val block: suspend () -> Unit = {
                if (runs.incrementAndGet() == 1) {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                }
            }

            val job = refresh.run(this + Dispatchers.Default, onIdle = { idle.complete(Unit) }, block = block)
            firstStarted.await()
            // Sign out and back in during the first refresh: five requests, all after it started.
            repeat(5) { assertNull(refresh.run(this + Dispatchers.Default, onIdle = {}, block = block)) }
            releaseFirst.complete(Unit)

            withTimeout(5_000) { idle.await() }
            job!!.join()
            assertEquals("one extra run picks up every request made meanwhile", 2, runs.get())
        }

    @Test
    fun `a request after a refresh finished starts a new one`() =
        runBlocking {
            val runs = AtomicInteger()
            refresh.run(this + Dispatchers.Default, onIdle = {}) { runs.incrementAndGet() }!!.join()
            refresh.run(this + Dispatchers.Default, onIdle = {}) { runs.incrementAndGet() }!!.join()
            assertEquals(2, runs.get())
        }

    @Test
    fun `a failing refresh does not block later ones`() =
        runBlocking {
            val failing = refresh.run(this + Dispatchers.Default, onIdle = {}) { error("network down") }!!
            failing.join()
            val runs = AtomicInteger()
            val next = refresh.run(this + Dispatchers.Default, onIdle = {}) { runs.incrementAndGet() }
            next!!.join()
            assertEquals(1, runs.get())
        }
}
