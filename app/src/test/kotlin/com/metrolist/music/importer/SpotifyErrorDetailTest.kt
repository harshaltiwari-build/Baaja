package com.metrolist.music.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyErrorDetailTest {
    @Test
    fun readsTheMessageOfAnApiError() {
        val body = """{"error":{"status":403,"message":"Active premium subscription required for the owner of the app."}}"""
        assertEquals("Active premium subscription required for the owner of the app.", SpotifyApi.errorDetail(body))
    }

    @Test
    fun readsTheDescriptionOfATokenError() {
        val body = """{"error":"invalid_client","error_description":"Invalid client secret"}"""
        assertEquals("Invalid client secret", SpotifyApi.errorDetail(body))
    }

    @Test
    fun fallsBackToTheErrorCodeWhenThereIsNoDescription() {
        assertEquals("invalid_client", SpotifyApi.errorDetail("""{"error":"invalid_client"}"""))
    }

    @Test
    fun ignoresBodiesWithoutAReason() {
        assertNull(SpotifyApi.errorDetail(null))
        assertNull(SpotifyApi.errorDetail(""))
        assertNull(SpotifyApi.errorDetail("not json"))
        assertNull(SpotifyApi.errorDetail("""{"foo":1}"""))
    }

    @Test
    fun longReasonsAreShortened() {
        val long = "x".repeat(500)
        assertEquals(200, SpotifyApi.errorDetail("""{"error":{"message":"$long"}}""")!!.length)
    }
}
