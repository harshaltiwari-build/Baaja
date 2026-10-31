package com.metrolist.innertube

import com.metrolist.innertube.models.YouTubeClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Not interested" is YouTube Music's thumbs down; this is the request YouTube receives for it. */
class DislikeRequestTest {
    @Test
    fun `dislike posts the song to like-dislike on the signed in account`() =
        runBlocking {
            val engine = MockEngine { respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json")) }
            HttpClient(engine) { install(ContentNegotiation) { json() } }.use { client ->
                val innerTube =
                    InnerTube(client).apply {
                        cookie = "SAPISID=session"
                        dataSyncId = "channel-sync-id"
                    }

                innerTube.dislikeVideo(YouTubeClient.WEB_REMIX, "kJQP7kiw5Fk")

                val request = engine.requestHistory.single()
                assertEquals(HttpMethod.Post, request.method)
                assertTrue(request.url.encodedPath, request.url.encodedPath.endsWith("/like/dislike"))
                val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                assertEquals("kJQP7kiw5Fk", body["target"]!!.jsonObject["videoId"]!!.jsonPrimitive.content)
                assertNotNull("signed request", request.headers[HttpHeaders.Authorization])
                assertTrue(request.headers[HttpHeaders.Cookie].orEmpty().contains("SAPISID=session"))
            }
        }

    @Test
    fun `a video id with request syntax stays a plain value`() =
        runBlocking {
            // Attack: ids come from YouTube responses and links; they must not be able to change the request.
            val engine = MockEngine { respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json")) }
            HttpClient(engine) { install(ContentNegotiation) { json() } }.use { client ->
                val innerTube = InnerTube(client).apply { cookie = "SAPISID=session" }
                val hostile = "x\",\"target\":{\"videoId\":\"other\"},\"a\":\"/../../like/like?"

                innerTube.dislikeVideo(YouTubeClient.WEB_REMIX, hostile)

                val request = engine.requestHistory.single()
                assertTrue(request.url.encodedPath.endsWith("/like/dislike"))
                val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                assertEquals(hostile, body["target"]!!.jsonObject["videoId"]!!.jsonPrimitive.content)
            }
        }
}
