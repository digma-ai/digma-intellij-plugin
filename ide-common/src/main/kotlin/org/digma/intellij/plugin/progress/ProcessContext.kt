package org.digma.intellij.plugin.progress

import com.intellij.openapi.progress.ProgressIndicator

/**
 * Used to keep track on errors in processes
 */
open class ProcessContext(val indicator: ProgressIndicator) {

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
}