package org.digma.intellij.plugin.semanticversion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SemanticVersionUtilsTest {


    @Test
    fun `test is valid`() {
        assertTrue(SemanticVersionUtil.isValidVersion("2.0.99"))
        assertTrue(SemanticVersionUtil.isValidVersion("2.0.99+232"))
        assertTrue(SemanticVersionUtil.isValidVersion("2.0+232"))

        assertFalse(SemanticVersionUtil.isValidVersion("2.0.90.232"))
        assertFalse(SemanticVersionUtil.isValidVersion("2.0.90.+232"))

    }

    @Test
    fun `test from`() {
        var version = "2.0.99"
        assertEquals(version, SemanticVersionUtil.from(version).toString())

        version = "2.0.99+232"
        assertEquals(version, SemanticVersionUtil.from(version).toString())

        version = "2.0+232"
        assertEquals(version, SemanticVersionUtil.from(version).toString())

    }


    @Test
    fun `test remove build number`() {
        var version = "2.0.99+232"
        assertEquals("2.0.99", SemanticVersionUtil.removeBuildNumberAndPreRelease(version))

        version = "2.0.99"
        assertEquals("2.0.99", SemanticVersionUtil.removeBuildNumberAndPreRelease(version))

        version = "2.0"
        assertEquals("2.0", SemanticVersionUtil.removeBuildNumberAndPreRelease(version))

        version = "2.0+232"
        assertEquals("2.0", SemanticVersionUtil.removeBuildNumberAndPreRelease(version))

        version = "2.0.99-alpha+232"
        assertEquals("2.0.99", SemanticVersionUtil.removeBuildNumberAndPreRelease(version))
    }


}