package com.metrolist.music.scrobble

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal class ScrobbleHttpException(
    val code: Int,
    message: String,
) : IOException(message) {
    /** True when retrying later may succeed (network trouble, rate limits, server errors). */
    val retryable: Boolean
        get() = code == -1 || code == 408 || code == 425 || code == 429 || code >= 500
}

internal object ScrobbleHttp {
    private const val TIMEOUT_MS = 15_000

    fun postForm(
        url: String,
        params: Map<String, String>,
    ): String {
        val body =
            params.entries.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }
        return request(url, "POST", body, "application/x-www-form-urlencoded", emptyMap())
    }

    fun request(
        url: String,
        method: String,
        body: String?,
        contentType: String?,
        headers: Map<String, String>,
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
            if (body != null) {
                connection.doOutput = true
                contentType?.let { connection.setRequestProperty("Content-Type", it) }
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code =
                try {
                    connection.responseCode
                } catch (e: IOException) {
                    throw ScrobbleHttpException(-1, e.message ?: "Network error")
                }
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw ScrobbleHttpException(code, text.take(300))
            return text
        } finally {
            connection.disconnect()
        }
    }
}
