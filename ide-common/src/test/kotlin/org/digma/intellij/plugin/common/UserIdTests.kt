package org.digma.intellij.plugin.common

import kotlin.test.Test
import kotlin.test.assertEquals

class UserIdTests {

    @Test
    fun testUniqueness() {
        val myId = generateUniqueUserId()
        repeat(10) {
            val id = generateUniqueUserId()
            assertEquals(myId, id)
        }
    }

    @Test
    fun testUniquenessWithSalt() {
        val myId = generateUniqueUserId("digma")
        repeat(10) {
            val id = generateUniqueUserId("digma")
            assertEquals(myId, id)
        }
    }

}