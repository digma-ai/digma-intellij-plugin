package org.digma.intellij.plugin.insights

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.Scope

@Service(Service.Level.PROJECT)
class InsightsModelReact : Disposable {

    private val scopeListeners = mutableListOf<InsightsScopeChangeListener>()


    companion object {
        @JvmStatic
        fun getInstance(project: Project): InsightsModelReact {
            return project.service<InsightsModelReact>()
        }
    }


    var scope: Scope = EmptyScope()
        set(value) {
            field = value
            fireScopeChanged()
        }


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


    fun addScopeChangeListener(listener: InsightsScopeChangeListener, parentDisposable: Disposable) {
        scopeListeners.add(listener)

        Disposer.register(parentDisposable) {
            removeChangeListener(listener)
        }
    }

    fun removeChangeListener(listener: InsightsScopeChangeListener) {
        scopeListeners.remove(listener)
    }

    //todo: maybe change to intellij message bus
    private fun fireScopeChanged() {
        scopeListeners.forEach {
            it.scopeChanged(this.scope)
        }
    }

    override fun dispose() {
        scopeListeners.clear()
    }
}


interface InsightsScopeChangeListener {

    fun scopeChanged(scope: Scope)

}