package org.digma.intellij.plugin.idea.execution

import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationHandler
import java.util.ServiceLoader

object RunConfigurationHandlersHolder {

    //RunConfigurationInstrumentationHandler is a delegating facades to services that provide the real work.
    //the main reason is to hide gradle and maven classes from the plugin in case gradle or maven are disabled.
    //RunConfigurationInstrumentationHandler that don't need a service can do the work themselves.
    val runConfigurationHandlers: List<RunConfigurationInstrumentationHandler> = try {
        //ServiceLoader doesn't work in intellij as it is in any other java application.
        //see https://youtrack.jetbrains.com/issue/IDEA-241229
        //so changing the context class loader for loading is the workaround
        val currentClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().setContextClassLoader(this::class.java.classLoader)
        val handlers =
            ServiceLoader.load(RunConfigurationInstrumentationHandler::class.java).stream().map { it.get() }.toList()
        Thread.currentThread().setContextClassLoader(currentClassLoader)
        handlers
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportInternalFatalError("RunConfigurationHandlersHolder.init", e)
        listOf()
    }
}