package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.Scope

data class InsightsModelReact(var scope: Scope = EmptyScope()) {

    //state properties for current context, method, codeless span etc.
    private val properties: MutableMap<String, Any> = mutableMapOf()

    fun addProperty(key: String, value: Any) {
        properties[key] = value
    }

    fun getProperty(key: String): Any? {
        return properties[key]
    }

    fun clearProperties() {
        properties.clear()
    }
}
