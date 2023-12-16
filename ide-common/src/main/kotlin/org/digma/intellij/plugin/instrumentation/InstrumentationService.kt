package org.digma.intellij.plugin.instrumentation

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.psi.LanguageService


@Service(Service.Level.PROJECT)
class InstrumentationService(private val project: Project) {


    fun canInstrumentMethod(project: Project, methodId: String?): CanInstrumentMethodResult? {
        val languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodId)
        return languageService.canInstrumentMethod(project, methodId)
    }

    fun instrumentMethod(methodId: String, result: CanInstrumentMethodResult): Boolean {
        val languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodId)
        return languageService.instrumentMethod(result)
    }

    // addDependencyToOtelLib so could manually instrument (so canInstrumentMethod would return true)
    fun addDependencyToOtelLib(project: Project, methodId: String) {
        val languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodId)
        return languageService.addDependencyToOtelLib(project, methodId)
    }


}