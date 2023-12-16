package org.digma.intellij.plugin.instrumentation

import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.autoimport.ProjectRefreshAction
import com.intellij.openapi.project.Project

class MethodInstrumentationPresenter(private val project: Project) {


    private var canInstrumentMethodResult: CanInstrumentMethodResult? = null

    var selectedMethodId: String = ""

    val canInstrumentMethod: Boolean
        get() = canInstrumentMethodResult?.wasSucceeded() ?: false

    val cannotBecauseMissingDependency: Boolean
        get() = canInstrumentMethodResult?.failureCause is MissingDependencyCause

    val missingDependency: String?
        get() = (canInstrumentMethodResult?.failureCause as? MissingDependencyCause)?.dependency

    fun instrumentMethod(): Boolean {
        return if (canInstrumentMethodResult != null)
            project.service<InstrumentationService>().instrumentMethod(selectedMethodId, canInstrumentMethodResult!!) ?: false
        else
            false
    }

    fun addDependencyToOtelLibAndRefresh() {
        project.service<InstrumentationService>().addDependencyToOtelLib(project, selectedMethodId!!)
        ProjectRefreshAction.refreshProject(project)
    }

    fun update(methodId: String) {
        selectedMethodId = methodId
        canInstrumentMethodResult = project.service<InstrumentationService>().canInstrumentMethod(project, methodId)
    }

}