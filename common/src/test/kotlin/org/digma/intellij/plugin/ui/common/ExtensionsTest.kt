package org.digma.intellij.plugin.ui.common

import org.digma.intellij.plugin.ui.common.Extensions.Companion.trimLastChar
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ExtensionsTest {

    @Test
    fun trimLastCharShouldWorkGivenNormalString() {
        val trimmed = "abcdefg".trimLastChar()
        assertEquals("abcdef", trimmed)
    }

    @Test
    fun trimLastCharShouldReturn1CharGiven2Chars() {
        val trimmed = "ZW".trimLastChar()
        assertEquals("Z", trimmed)
    }

    @Test
    fun trimLastCharShouldReturn0CharGiven1Char() {
        val trimmed = "Z".trimLastChar()
        assertEquals("", trimmed)
    }

    @Test
    fun trimLastCharShouldReturnEmptyStringGivenEmptyString() {
        val trimmed = "".trimLastChar()
        assertEquals("", trimmed)
    }

}