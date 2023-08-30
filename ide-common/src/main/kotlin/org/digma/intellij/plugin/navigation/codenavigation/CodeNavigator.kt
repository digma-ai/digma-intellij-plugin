package org.digma.intellij.plugin.navigation.codenavigation

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.rest.navigation.CodeObjectNavigation
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.SupportedLanguages
import org.digma.intellij.plugin.service.EditorService
import org.digma.intellij.plugin.ui.ToolWindowShower

@Service(Service.Level.PROJECT)
class CodeNavigator(val project: Project) {

    private val logger: Logger = Logger.getInstance(CodeNavigator::class.java)

    //Note: ids for navigation should not include prefix span: or method:

    fun maybeNavigateToSpanOrMethod(spanId: String?, methodId: String?): Boolean {

        if (maybeNavigateToSpan(spanId)) {
            return true
        }

        return maybeNavigateToMethod(methodId)
    }


    fun maybeNavigateToSpan(spanId: String?): Boolean {
        if (spanId == null) {
            return false
        }

        val spanIdWithoutType = CodeObjectsUtil.stripSpanPrefix(spanId)

        SupportedLanguages.values().forEach { language ->
            if (language == SupportedLanguages.CSHARP) {
                return@forEach
            }
            val languageService = LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)
            if (languageService != null) {
                val spanWorkspaceUris = ReadActions.ensureReadAction<Map<String, Pair<String, Int>>> {
                    languageService.findWorkspaceUrisForSpanIds(listOf(spanIdWithoutType))
                }

                val pair: Pair<String, Int>? = spanWorkspaceUris[spanIdWithoutType]
                if (pair != null) {
                    Log.log(logger::debug, project, "found span code location in maybeNavigateToSpan for span {}", spanIdWithoutType)
                    EDT.ensureEDT {
                        project.service<EditorService>().openWorkspaceFileInEditor(pair.first, pair.second)
                    }
                    ToolWindowShower.getInstance(project).showToolWindow()
                    //if code location was found link to it and return. no need to check the other language services
                    return true
                }
            }
        }

        Log.log(logger::debug, project, "could not find code location in maybeNavigateToSpan for {}", spanId)
        return false

    }

    //todo: change to navigateToMethod in languageService. need to implement a tryNavigateToMethod
    fun maybeNavigateToMethod(methodId: String?): Boolean {
        if (methodId == null) {
            return false
        }

        val methodIdWithoutType = CodeObjectsUtil.stripMethodPrefix(methodId)

        SupportedLanguages.values().forEach { language ->
            if (language == SupportedLanguages.CSHARP) {
                return@forEach
            }
            val languageService = LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)
            if (languageService != null) {
                val methodWorkspaceUris = ReadActions.ensureReadAction<Map<String, Pair<String, Int>>> {
                    languageService.findWorkspaceUrisForMethodCodeObjectIds(listOf(methodIdWithoutType))
                }

                val pair: Pair<String, Int>? = methodWorkspaceUris[methodIdWithoutType]
                if (pair != null) {
                    Log.log(logger::debug, project, "found method code location in maybeNavigateToSpan for method {}", methodIdWithoutType)
                    EDT.ensureEDT {
                        project.service<EditorService>().openWorkspaceFileInEditor(pair.first, pair.second)
                    }
                    ToolWindowShower.getInstance(project).showToolWindow()
                    //if code location was found link to it and return. no need to check the other language services
                    return true
                }
            }
        }

        Log.log(logger::debug, project, "could not find code location in maybeNavigateToMethod for {}", methodId)
        return false

    }

    fun canNavigateToSpanOrMethod(spanCodeObjectId: String, methodCodeObjectId: String?): Boolean {
        return canNavigateToSpan(spanCodeObjectId) || canNavigateToMethod(methodCodeObjectId)
    }

    fun canNavigateToMethod(methodCodeObjectId: String?): Boolean {
        if (methodCodeObjectId == null) {
            return false
        }

        val methodIdWithoutType = CodeObjectsUtil.stripMethodPrefix(methodCodeObjectId)


        SupportedLanguages.values().forEach { language ->
            if (language == SupportedLanguages.CSHARP) {
                return@forEach
            }
            val languageService = LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)
            if (languageService != null) {
                val methodWorkspaceUris = ReadActions.ensureReadAction<Map<String, Pair<String, Int>>> {
                    languageService.findWorkspaceUrisForMethodCodeObjectIds(listOf(methodIdWithoutType))
                }
                //if code location was found return. no need to check the other language services
                if (methodWorkspaceUris.containsKey(methodIdWithoutType)) {
                    return true
                }
            }
        }
        return false
    }

    fun canNavigateToSpan(spanCodeObjectId: String?): Boolean {
        if (spanCodeObjectId == null) {
            return false
        }

        val spanIdWithoutType = CodeObjectsUtil.stripSpanPrefix(spanCodeObjectId)

        SupportedLanguages.values().forEach { language ->
            if (language == SupportedLanguages.CSHARP) {
                return@forEach
            }
            val languageService = LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)
            if (languageService != null) {
                val spanWorkspaceUris = ReadActions.ensureReadAction<Map<String, Pair<String, Int>>> {
                    languageService.findWorkspaceUrisForSpanIds(listOf(spanIdWithoutType))
                }
                //if code location was found return. no need to check the other language services
                if (spanWorkspaceUris.containsKey(spanIdWithoutType)) {
                    //if code location was found link to it and return. no need to check the other language services
                    return true
                }
            }
        }

        return false
    }

    fun getMethodLocation(methodId: String): Pair<String, Int>? {

        val methodIdWithoutType = CodeObjectsUtil.stripMethodPrefix(methodId)

        SupportedLanguages.values().forEach { language ->
            if (language == SupportedLanguages.CSHARP) {
                return@forEach
            }
            val languageService = LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)
            if (languageService != null) {
                val methodWorkspaceUris = ReadActions.ensureReadAction<Map<String, Pair<String, Int>>> {
                    languageService.findWorkspaceUrisForMethodCodeObjectIds(listOf(methodIdWithoutType))
                }
                //if code location was found return. no need to check the other language services
                if (methodWorkspaceUris.containsKey(methodIdWithoutType)) {
                    return methodWorkspaceUris[methodIdWithoutType]
                }
            }
        }
        return null
    }


    fun getSpanLocation(spanId: String): Pair<String, Int>? {

        val spanIdWithoutType = CodeObjectsUtil.stripSpanPrefix(spanId)

        SupportedLanguages.values().forEach { language ->
            if (language == SupportedLanguages.CSHARP) {
                return@forEach
            }
            val languageService = LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)
            if (languageService != null) {
                val spanWorkspaceUris = ReadActions.ensureReadAction<Map<String, Pair<String, Int>>> {
                    languageService.findWorkspaceUrisForSpanIds(listOf(spanIdWithoutType))
                }
                //if code location was found return. no need to check the other language services
                if (spanWorkspaceUris.containsKey(spanIdWithoutType)) {
                    return spanWorkspaceUris[spanIdWithoutType]
                }
            }
        }
        return null
    }

    fun buildPotentialMethodIds(codeObjectNavigation: CodeObjectNavigation): List<String> {
        val candidateSet = mutableSetOf<String>()
        codeObjectNavigation.navigationEntry.spanInfo?.methodCodeObjectId?.let {
            candidateSet.add(it)
        }
        codeObjectNavigation.navigationEntry.navEndpointEntry?.methodCodeObjectId?.let {
            candidateSet.add(it)
        }

        codeObjectNavigation.navigationEntry.navEndpointEntry?.endpointCodeObjectId?.also { it ->
            val endpointId = CodeObjectsUtil.stripEndpointPrefix(it)

            SupportedLanguages.values().forEach { language ->
                if (language == SupportedLanguages.CSHARP) {
                    return@forEach
                }
                val languageService = LanguageService.findLanguageServiceByName(project, language.languageServiceClassName)
                if (languageService != null) {
                    val endpointInfos = ReadActions.ensureReadAction<Set<EndpointInfo>> {
                        languageService.lookForDiscoveredEndpoints(endpointId)
                    }

                    endpointInfos.forEach { endpointInfo ->
                        candidateSet.add(endpointInfo.containingMethodId)
                    }
                }
            }
        }

        val retList = candidateSet.filter {
            canNavigateToMethod(it)
        }

        return retList
    }

    fun canNavigateToFile(fileUri: String): Boolean {
        val file = VirtualFileManager.getInstance().findFileByUrl(fileUri)
        return file != null
    }

    fun maybeNavigateToFile(fileUri: String) {
        EDT.ensureEDT {
            project.service<EditorService>().openWorkspaceFileInEditor(fileUri, 1)
        }
    }


}