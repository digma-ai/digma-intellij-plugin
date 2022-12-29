package org.digma.intellij.plugin.idea.psi.java

import org.digma.intellij.plugin.idea.psi.java.JavaUtils.Companion.adjustUri
import org.digma.intellij.plugin.idea.psi.java.JavaUtils.Companion.combineUri
import kotlin.test.Test
import kotlin.test.assertEquals

internal class JavaUtilsTest {

    @Test
    fun combineUri_given_empty_strings() {
        assertEquals("/", combineUri("", ""))
        assertEquals("/abc", combineUri("abc", ""))
        assertEquals("/qrs", combineUri("", "qrs"))
    }

    @Test
    fun combineUri_various_combinations() {
        assertEquals("/abc/qrs", combineUri("abc", "qrs"))
        assertEquals("/abc/qrs", combineUri("/abc", "/qrs"))
        assertEquals("/abc/qrs", combineUri("abc/", "qrs/"))
        assertEquals("/abc/qrs", combineUri("/abc/", "/qrs/"))
    }

    @Test
    fun adjustUri_various_options() {
        assertEquals("/abc/qrs", adjustUri("abc///qrs////"))
        assertEquals("/abc/qrs", adjustUri("  abc/qrs "))
    }
}