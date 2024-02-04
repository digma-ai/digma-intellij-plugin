package org.digma.intellij.plugin.idea.navigation.model

import com.intellij.openapi.progress.ProgressIndicator
import org.digma.intellij.plugin.common.SearchScopeProvider

class NavigationProcessContext(val searchScope: SearchScopeProvider, val indicator: ProgressIndicator) {

    //key is error hint to ErrorReported
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