package org.digma.intellij.plugin.idea.execution

import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationHandler
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals

class RunConfigurationHandlersHolderTests {

    //todo: create intellij tests

    @Test
    fun testAllRegistered() {
        assertEquals(9, ServiceLoader.load(RunConfigurationInstrumentationHandler::class.java).stream().count())
    }

}