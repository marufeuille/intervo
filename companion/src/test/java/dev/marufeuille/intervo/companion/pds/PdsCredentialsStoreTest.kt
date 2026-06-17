package dev.marufeuille.intervo.companion.pds

import org.junit.Assert.assertEquals
import org.junit.Test

class PdsCredentialsStoreTest {

    @Test
    fun blankServiceUrlStaysBlank() {
        val normalized = with(PdsCredentialsStore) { "".normalizedServiceUrl() }

        assertEquals("", normalized)
    }

    @Test
    fun serviceUrlIsTrimmedAndTrailingSlashIsRemoved() {
        val normalized = with(PdsCredentialsStore) { " https://pds.example.com/ ".normalizedServiceUrl() }

        assertEquals("https://pds.example.com", normalized)
    }
}
