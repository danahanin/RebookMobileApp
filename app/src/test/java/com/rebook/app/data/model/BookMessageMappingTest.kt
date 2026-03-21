package com.rebook.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookMessageMappingTest {

    @Test
    fun bookMessageFromFirestoreData_mapsValidMap() {
        val msg = bookMessageFromFirestoreData(
            id = "m1",
            data = mapOf(
                "senderId" to "u1",
                "text" to "hello",
                "createdAt" to 42L
            )
        )
        assertEquals(BookMessage(id = "m1", senderId = "u1", text = "hello", createdAt = 42L), msg)
    }

    @Test
    fun bookMessageFromFirestoreData_acceptsIntCreatedAt() {
        val msg = bookMessageFromFirestoreData(
            id = "m1",
            data = mapOf(
                "senderId" to "u1",
                "text" to "hi",
                "createdAt" to 7
            )
        )
        assertEquals(7L, msg?.createdAt)
    }

    @Test
    fun bookMessageFromFirestoreData_returnsNullWhenMissingFields() {
        assertNull(bookMessageFromFirestoreData("m1", null))
        assertNull(bookMessageFromFirestoreData("m1", emptyMap()))
        assertNull(
            bookMessageFromFirestoreData(
                "m1",
                mapOf("senderId" to "u1", "text" to "x")
            )
        )
    }
}
