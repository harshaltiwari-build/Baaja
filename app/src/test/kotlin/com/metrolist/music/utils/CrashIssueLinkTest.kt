package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.URLDecoder

/** The "Report this crash on GitHub" link: it opens a public issue form, so nothing private may ride along. */
class CrashIssueLinkTest {
    private val log =
        """
        Baaja Crash Report
        Manufacturer: motorola
        Stacktrace:
        java.lang.IllegalArgumentException: Key "a_MPREb_aGmlqkANGBv" was already used.
        	at androidx.compose.ui.layout.Something(r8:3)
        """.trimIndent()

    private fun params(url: String): Map<String, String> =
        URI(url).rawQuery.split('&').associate {
            val (k, v) = it.split('=', limit = 2)
            k to URLDecoder.decode(v, "UTF-8")
        }

    @Test
    fun `opens baaja's bug form with the crash filled in`() {
        val url = crashIssueUrl(log, appVersion = "1.3.3 (9)", androidVersion = "Android 15 (SDK 35)")
        assertTrue(url.startsWith("https://github.com/harshaltiwari-build/Baaja/issues/new?"))
        val p = params(url)
        assertEquals("bug_report.yml", p["template"])
        assertEquals("[Crash] java.lang.IllegalArgumentException: Key \"a_MPREb_aGmlqkANGBv\" was already used.", p["title"])
        assertEquals("1.3.3 (9)", p["app-version"])
        assertEquals("Android 15 (SDK 35)", p["android-version"])
        assertEquals(log, p["logs"])
    }

    @Test
    fun `long logs are cut so github accepts the link`() {
        val url = crashIssueUrl(log + "\n" + "x".repeat(20_000), "1", "A")
        assertTrue(params(url)["logs"]!!.length <= 5_000)
        assertTrue("whole link stays under GitHub's limit", url.length < 8_000)
    }

    @Test
    fun `logs that grow when encoded still fit github's limit`() {
        // Attack finding: the cut counted characters before encoding; Hindi text triples in size.
        val url = crashIssueUrl(log + "\n" + "त्रुटि ".repeat(3_000), "1", "A")
        assertTrue(url.length < 8_000)
        assertTrue(params(url)["logs"]!!.startsWith("Baaja Crash Report"))
    }

    @Test
    fun `a log without an exception line still gets a title`() {
        assertEquals("[Crash] App crash", params(crashIssueUrl("nothing useful", "1", "A"))["title"])
    }

    @Test
    fun `characters that could break the link are encoded`() {
        val url = crashIssueUrl("RuntimeException: a&title=hacked#x?y=1 %20", "1&x=2", "A")
        val p = params(url)
        assertEquals("[Crash] RuntimeException: a&title=hacked#x?y=1 %20", p["title"])
        assertEquals("1&x=2", p["app-version"])
        assertFalse(url.contains("#"))
    }

    // ---- attack: private data in a public report ----

    @Test
    fun `cookies, tokens, emails and file names are blanked out`() {
        val leaky =
            """
            java.io.IOException: HTTP 403 Cookie: SAPISID=abc123; __Secure-3PSID=xyz; VISITOR_INFO1_LIVE=vvv
            Authorization: SAPISIDHASH 1700000_deadbeef
            at https://rr3.googlevideo.com/videoplayback?expire=1&pot=SECRETPOT&sig=SIG&key=AIzaKEY&n=abc
            owner harshal.tiwari@example.com
            java.io.FileNotFoundException: /storage/emulated/0/Music/Private Voice Note.mp3 (No such file)
            """.trimIndent()
        val clean = redactForPublicReport(leaky)

        for (secret in listOf("abc123", "xyz", "vvv", "deadbeef", "SECRETPOT", "SIG", "AIzaKEY", "harshal.tiwari@example.com", "Private Voice Note")) {
            assertFalse("$secret leaked", clean.contains(secret))
        }
        assertTrue("what went wrong is still readable", clean.contains("java.io.IOException: HTTP 403"))
        assertTrue(clean.contains("FileNotFoundException"))
    }

    @Test
    fun `the link itself is redacted, title included`() {
        val url = crashIssueUrl("java.lang.RuntimeException: login failed for a@b.co with SAPISID=topsecret", "1", "A")
        assertFalse(url.contains("topsecret"))
        assertFalse(URLDecoder.decode(url, "UTF-8").contains("a@b.co"))
    }
}
