package org.digma.intellij.plugin.model.discovery

import org.digma.intellij.plugin.model.discovery.CodeObjectInfo.Companion.extractFqnClassName
import org.digma.intellij.plugin.model.discovery.CodeObjectInfo.Companion.extractMethodName
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CodeObjectInfoTest {

    @Test
    fun ableToExtractMethodName() {
        assertEquals("JustDoIt", extractMethodName("p1.p2.Class\$_\$JustDoIt"))
    }

    @Test
    fun ableToExtractFqnClassName() {
        assertEquals("p1.p2.Class", extractFqnClassName("p1.p2.Class\$_\$JustDoIt"))
    }
}