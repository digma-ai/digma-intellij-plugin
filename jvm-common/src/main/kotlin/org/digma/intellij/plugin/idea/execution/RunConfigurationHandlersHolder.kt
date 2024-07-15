package org.digma.intellij.plugin.idea.execution

import com.intellij.openapi.components.service
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationHandler
import java.util.ServiceLoader

@Suppress("LightServiceMigrationCode")
class RunConfigurationHandlersHolder {

    private var runConfigurationHandlers: List<RunConfigurationInstrumentationHandler>? = null

    companion object {
        fun getInstance(): RunConfigurationHandlersHolder {
            return service<RunConfigurationHandlersHolder>()
        }
    }


    //RunConfigurationInstrumentationHandler is a delegating facades to services that provide the real work.
    //the main reason is to hide gradle and maven classes from the plugin in case gradle or maven are disabled.
    //RunConfigurationInstrumentationHandler that don't need a service can do the work themselves.
    fun getRunConfigurationHandlers(): List<RunConfigurationInstrumentationHandler> {
        if (runConfigurationHandlers == null) {

            //ServiceLoader doesn't work in intellij as it is in any other java application.
            //see https://youtrack.jetbrains.com/issue/IDEA-241229
            //so changing the context class loader for loading is the workaround
            val currentClassLoader = Thread.currentThread().contextClassLoader

            runConfigurationHandlers = try {
                Thread.currentThread().setContextClassLoader(this::class.java.classLoader)
                ServiceLoader.load(RunConfigurationInstrumentationHandler::class.java).stream().map { it.get() }.toList()
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("RunConfigurationHandlersHolder.init", e)
                emptyList()
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader)
            }
        }
        return runConfigurationHandlers ?: emptyList()
    }

}