package org.digma.intellij.plugin.process

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_MEDIUM
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
import org.digma.intellij.plugin.log.Log


open class ProcessContext(val processName: String) {

    /**
     * indicator needs to be a new one every time a process starts.
     * maybe the indicator instance created here on initialization will not be used
     * because the process manager will replace it with a new one for every retry.
     * it is initialized mainly for avoiding a nullable var for comfort in code.
     */
    var indicator: ProgressIndicator = EmptyProgressIndicator()


    //key is error hint to send to org.digma.intellij.plugin.errorreporting.ErrorReporter
    private val errors = mutableMapOf<String, MutableList<Throwable>>()


    fun addError(hint: String, e: Throwable) {
        errors.computeIfAbsent(hint) { mutableListOf() }.add(e)
    }

    fun clearErrors(hint: String) {
        errors.computeIfAbsent(hint) { mutableListOf() }.clear()
    }

    fun hasErrors(): Boolean {
        return errors.filter { entry -> entry.value.isNotEmpty() }.isNotEmpty()
    }

    //returns read only map
    fun errorsList(): Map<String, List<Throwable>> {
        return errors.toMap().mapValues { entry -> entry.value.toList() }
    }


    fun logErrors(logger: Logger, project: Project) {
        if (hasErrors()) {
            errorsList().forEach { entry ->
                val hint = entry.key
                val errors = entry.value
                errors.forEach { err ->
                    Log.warnWithException(logger, err, "Exception in $processName")
                    ErrorReporter.getInstance().reportError(
                        project, "$processName.$hint", err, mapOf(SEVERITY_PROP_NAME to SEVERITY_MEDIUM)
                    )
                }
            }
        }
    }

}