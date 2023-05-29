package org.digma.intellij.plugin.navigation.codenavigation

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.SupportedLanguages
import org.digma.intellij.plugin.service.EditorService
import org.digma.intellij.plugin.ui.ToolWindowShower

@Service(Service.Level.PROJECT)
class CodeNavigator(val project: Project) {

    private val logger: Logger = Logger.getInstance(CodeNavigator::class.java)


    fun maybeNavigateToSpan(instLibrary: String, spanName: String, functionNamespace: String?, functionName: String?):Boolean {

        val spanId = CodeObjectsUtil.createSpanId(instLibrary, spanName)
        if (maybeNavigateToSpan(spanId)){
            return true
        }

        if (functionNamespace != null && functionName != null) {
            val methodId = CodeObjectsUtil.createMethodCodeObjectId(functionNamespace, functionName)
            if (maybeNavigateToMethod(methodId)){
                return true
            }
        }

        Log.log(logger::debug,project,"could not find code location in maybeNavigateToSpan for {},{},{},{}",instLibrary,spanName,functionNamespace,functionName)
        return false
    }


    fun maybeNavigateToSpan(spanId: String?, methodId: String?): Boolean {

        if (spanId != null && maybeNavigateToSpan(spanId)) {
            return true
        }

        return methodId != null && maybeNavigateToMethod(methodId)
    }





    fun maybeNavigateToSpan(spanId: String?):Boolean {
        if (spanId == null){
            return false
        }

        SupportedLanguages.values().forEach { language ->
            val languageService = LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)
            if (languageService != null) {
                val spanWorkspaceUris = ReadActions.ensureReadAction<Map<String, Pair<String, Int>>> {
                    languageService.findWorkspaceUrisForSpanIds(listOf(spanId))
                }
                if (spanWorkspaceUris.containsKey(spanId)) {
                    Log.log(logger::debug,project,"found span code location in maybeNavigateToSpan for span {}",spanId)
                    val (first, second) = spanWorkspaceUris[spanId]!!
                    EDT.ensureEDT {
                        project.service<EditorService>().openWorkspaceFileInEditor(first, second)
                    }
                    ToolWindowShower.getInstance(project).showToolWindow()
                    //if code location was found link to it and return. no need to check the other language services
                    return true
                }
            }
        }

        Log.log(logger::debug,project,"could not find code location in maybeNavigateToSpan for {}",spanId)
        return false

    }

    fun maybeNavigateToMethod(methodId: String?): Boolean {

        if (methodId == null) {
            return false
        }

        SupportedLanguages.values().forEach { language ->
            val languageService = LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)
            if (languageService != null) {
                val methodWorkspaceUris = ReadActions.ensureReadAction<Map<String, Pair<String, Int>>> {
                    languageService.findWorkspaceUrisForMethodCodeObjectIds(listOf(methodId))
                }
                if (methodWorkspaceUris.containsKey(methodId)) {
                    Log.log(logger::debug,project,"found method code location in maybeNavigateToSpan for method {}",methodId)
                    val (first, second) = methodWorkspaceUris[methodId]!!
                    EDT.ensureEDT {
                        project.service<EditorService>().openWorkspaceFileInEditor(first, second)
                    }
                    ToolWindowShower.getInstance(project).showToolWindow()
                    //if code location was found link to it and return. no need to check the other language services
                    return true
                }
            }
        }


        Log.log(logger::debug,project,"could not find code location in maybeNavigateToMethod for {}",methodId)
        return false

    }

    fun canNavigateToSpanOrMethod(spanCodeObjectId: String, methodCodeObjectId: String?): Boolean {
        return canNavigateToSpan(spanCodeObjectId) || canNavigateToMethod(methodCodeObjectId)
    }

    fun canNavigateToMethod(methodCodeObjectId: String?): Boolean {

        if (methodCodeObjectId == null){
            return false
        }

        SupportedLanguages.values().forEach { language ->
            val languageService = LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)
            if (languageService != null) {
                val methodWorkspaceUris = ReadActions.ensureReadAction<Map<String, Pair<String, Int>>> {
                    languageService.findWorkspaceUrisForMethodCodeObjectIds(listOf(methodCodeObjectId))
                }
                if (methodWorkspaceUris.containsKey(methodCodeObjectId)) {
                    return true
                }
            }
        }
        return false
    }

    fun canNavigateToSpan(spanCodeObjectId: String?): Boolean {
        if (spanCodeObjectId == null){
            return false
        }

        SupportedLanguages.values().forEach { language ->
            val languageService = LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)
            if (languageService != null) {
                val spanWorkspaceUris = ReadActions.ensureReadAction<Map<String, Pair<String, Int>>> {
                    languageService.findWorkspaceUrisForSpanIds(listOf(spanCodeObjectId))
                }
                if (spanWorkspaceUris.containsKey(spanCodeObjectId)) {
                    //if code location was found link to it and return. no need to check the other language services
                    return true
                }
            }
        }

        return false
    }


}