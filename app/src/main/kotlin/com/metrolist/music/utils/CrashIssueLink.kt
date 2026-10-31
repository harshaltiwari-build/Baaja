package com.metrolist.music.utils

import java.net.URLEncoder

private const val NEW_ISSUE_URL = "https://github.com/harshaltiwari-build/Baaja/issues/new"
private const val MAX_TITLE_CHARS = 120
private const val MAX_LOG_CHARS = 5_000

/** GitHub rejects longer links; kept under it after encoding, not before. */
private const val MAX_URL_CHARS = 7_500

/**
 * Link to Baaja's bug report form with the crash filled in. The form is public, so the log and title are
 * passed through [redactForPublicReport] first, and nothing is sent until the person submits it.
 */
internal fun crashIssueUrl(
    crashLog: String,
    appVersion: String,
    androidVersion: String,
): String {
    val log = redactForPublicReport(crashLog)
    val error =
        log.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.contains("Exception") || it.contains("Error:") }
            ?.take(MAX_TITLE_CHARS)
            ?: "App crash"
    val fixed =
        "$NEW_ISSUE_URL?template=bug_report.yml" +
            "&title=${encode("[Crash] $error")}" +
            "&app-version=${encode(appVersion)}" +
            "&android-version=${encode(androidVersion)}" +
            "&logs="
    var logChars = minOf(log.length, MAX_LOG_CHARS)
    var logs = encode(log.take(logChars))
    while (fixed.length + logs.length > MAX_URL_CHARS && logChars > 0) {
        logChars = logChars * 9 / 10
        logs = encode(log.take(logChars))
    }
    return fixed + logs
}

private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

private val redactions =
    listOf(
        // Google account cookies, e.g. SAPISID=…, __Secure-3PSID=…
        Regex("""\b(SAPISID|APISID|HSID|SSID|SID|SIDCC|LOGIN_INFO|VISITOR_INFO1_LIVE|VISITOR_PRIVACY_METADATA|PREF|__Secure-[\w-]+)=[^;\s]+""") to "$1=[redacted]",
        Regex("""(?i)(authorization\s*[:=]\s*)[^\n]+""") to "$1[redacted]",
        // Signed or keyed URL parameters on YouTube and Google links.
        Regex("""(?i)([?&](?:key|pot|sig|lsig|signature|n|token|access_token|refresh_token|auth|visitorData)=)[^&\s]+""") to "$1[redacted]",
        Regex("""[\w.+-]+@[\w-]+\.[\w.-]+""") to "[email]",
        // Names of the person's own files and folders.
        Regex("""(/storage/emulated/\d+|/sdcard|/storage/[\w-]+)/[^\n():]+""") to "$1/[file]",
    )

/** Blanks out account cookies, tokens, email addresses and file names so a crash log can go on a public page. */
internal fun redactForPublicReport(text: String): String =
    redactions.fold(text) { acc, (pattern, replacement) -> pattern.replace(acc, replacement) }
