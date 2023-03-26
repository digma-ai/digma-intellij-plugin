package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.psi.CanInstrumentMethodResult
import org.digma.intellij.plugin.psi.LanguageService

class MethodInstrumentationPresenter(private val project: Project) {

    private var languageService: LanguageService? = null

    private var canInstrumentMethodResult: CanInstrumentMethodResult? = null

    val canInstrumentMethod: Boolean
        get() = canInstrumentMethodResult?.wasSucceeded() ?: false

    val cannotBecauseMissingDependency: Boolean
        get() = canInstrumentMethodResult?.failureCause is CanInstrumentMethodResult.MissingDependencyCause

    val missingDependency: String?
        get() = (canInstrumentMethodResult?.failureCause as? CanInstrumentMethodResult.MissingDependencyCause)?.dependency

    fun instrumentMethod(): Boolean {
        return if(canInstrumentMethodResult != null) languageService?.instrumentMethod(canInstrumentMethodResult!!) ?: false
            else false
    }

    fun update(methodId: String?){
        languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodId)
        canInstrumentMethodResult = languageService!!.canInstrumentMethod(project, methodId)
    }

}