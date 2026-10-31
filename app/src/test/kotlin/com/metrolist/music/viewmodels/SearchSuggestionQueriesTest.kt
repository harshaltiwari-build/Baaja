package com.metrolist.music.viewmodels

import com.metrolist.music.db.entities.SearchHistory
import org.junit.Assert.assertEquals
import org.junit.Test

/** Suggestions are list keys on the search screen, so a repeat would crash it like Home's albums did. */
class SearchSuggestionQueriesTest {
    @Test
    fun `a suggestion YouTube sends twice is shown once`() {
        assertEquals(listOf("arijit", "arijit singh"), suggestionQueries(listOf("arijit", "arijit singh", "arijit"), emptyList()))
    }

    @Test
    fun `suggestions already in recent searches are left out`() {
        assertEquals(listOf("kk"), suggestionQueries(listOf("arijit", "kk"), listOf(SearchHistory(query = "arijit"))))
    }

    @Test
    fun `blank suggestions are dropped`() {
        assertEquals(listOf("kk"), suggestionQueries(listOf("", "  ", "kk"), emptyList()))
    }
}
