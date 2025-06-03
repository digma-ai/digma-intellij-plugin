package org.digma.intellij.plugin.navigation.codenavigation

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.digma.intellij.plugin.common.CodeObjectsUtil
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_LOW
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.navigation.CodeObjectNavigation
import org.digma.intellij.plugin.psi.LanguageServiceProvider
import org.digma.intellij.plugin.service.EditorService
import org.digma.intellij.plugin.ui.ToolWindowShower

@Service(Service.Level.PROJECT)
class CodeNavigator(val project: Project) {

    private val logger: Logger = Logger.getInstance(CodeNavigator::class.java)

    //Note: ids for navigation should not include prefix span: or method:


    companion object {
        @JvmStatic
        fun getInstance(project: Project): CodeNavigator {
            return project.service<CodeNavigator>()
        }
    }

    suspend fun maybeNavigateToSpanOrMethod(spanId: String?, methodId: String?): Boolean {
        if (maybeNavigateToSpan(spanId)) {
            return true
        }
        return maybeNavigateToMethod(methodId)
    }


    suspend fun maybeNavigateToSpan(spanId: String?): Boolean {
        if (spanId == null) {
            return false
        }

        val spanIdWithoutType = CodeObjectsUtil.stripSpanPrefix(spanId)

        for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
            val spanWorkspaceUris =
                languageService.findWorkspaceUrisForSpanIds(listOf(spanIdWithoutType))

            val pair: Pair<String, Int>? = spanWorkspaceUris[spanIdWithoutType]
            if (pair != null) {
                Log.log(logger::debug, project, "found span code location in maybeNavigateToSpan for span {}", spanIdWithoutType)
                withContext(Dispatchers.EDT) {
                    EditorService.getInstance(project).openWorkspaceFileInEditor(pair.first, pair.second)
                }
                ToolWindowShower.getInstance(project).showToolWindow()
                //if code location was found link to it and return. no need to check the other language services
                return true
            }
        }

        Log.log(logger::debug, project, "could not find code location in maybeNavigateToSpan for {}", spanId)
        return false

    }

    //todo: change to navigateToMethod in languageService. need to implement a tryNavigateToMethod
    suspend fun maybeNavigateToMethod(methodId: String?): Boolean {
        if (methodId == null) {
            return false
        }

        val methodIdWithoutType = CodeObjectsUtil.stripMethodPrefix(methodId)

        for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
            val methodWorkspaceUris =
                languageService.findWorkspaceUrisForMethodCodeObjectIds(listOf(methodIdWithoutType))

            val pair: Pair<String, Int>? = methodWorkspaceUris[methodIdWithoutType]
            if (pair != null) {
                Log.log(logger::debug, project, "found method code location in maybeNavigateToSpan for method {}", methodIdWithoutType)
                withContext(Dispatchers.EDT) {
                    EditorService.getInstance(project).openWorkspaceFileInEditor(pair.first, pair.second)
                }
                ToolWindowShower.getInstance(project).showToolWindow()
                //if code location was found link to it and return. no need to check the other language services
                return true
            }
        }

        Log.log(logger::debug, project, "could not find code location in maybeNavigateToMethod for {}", methodId)
        return false

    }

    suspend fun canNavigateToSpanOrMethod(spanCodeObjectId: String, methodCodeObjectId: String?): Boolean {
        return canNavigateToSpan(spanCodeObjectId) || canNavigateToMethod(methodCodeObjectId)
    }

    suspend fun canNavigateToMethod(methodCodeObjectId: String?): Boolean {
        if (methodCodeObjectId == null) {
            return false
        }

        val methodIdWithoutType = CodeObjectsUtil.stripMethodPrefix(methodCodeObjectId)

        for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
            val methodWorkspaceUris = try {
                languageService.findWorkspaceUrisForMethodCodeObjectIds(listOf(methodIdWithoutType))
            } catch (e: IndexNotReadyException) {
                //this error will happen sometimes, especially on startup when indexing still in process, severity is low because we can't do anything about it but retry
                ErrorReporter.getInstance().reportError("CodeNavigator.canNavigateToMethod", e, mapOf(SEVERITY_PROP_NAME to SEVERITY_LOW))
                mapOf()
            }
            //if code location was found return. no need to check the other language services
            if (methodWorkspaceUris.containsKey(methodIdWithoutType)) {
                return true
            }
        }
        return false
    }


    suspend fun canNavigateToSpan(spanCodeObjectId: String?): Boolean {
        if (spanCodeObjectId == null) {
            return false
        }

        val spanIdWithoutType = CodeObjectsUtil.stripSpanPrefix(spanCodeObjectId)

        for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
            val spanWorkspaceUris = languageService.findWorkspaceUrisForSpanIds(listOf(spanIdWithoutType))
            //if code location was found return. no need to check the other language services
            if (spanWorkspaceUris.containsKey(spanIdWithoutType)) {
                //if code location was found link to it and return. no need to check the other language services
                return true
            }
        }

        return false
    }

    suspend fun findMethodCodeObjectId(spanCodeObjectId: String): String? {
        for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
            val methodCodeObjectId =
                languageService.detectMethodBySpan(project, CodeObjectsUtil.stripSpanPrefix(spanCodeObjectId))
            if (methodCodeObjectId != null) {
                return methodCodeObjectId
            }
        }
        return null
    }

    suspend fun getMethodLocation(methodId: String): Pair<String, Int>? {

        val methodIdWithoutType = CodeObjectsUtil.stripMethodPrefix(methodId)

        for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
            val methodWorkspaceUris =
                languageService.findWorkspaceUrisForMethodCodeObjectIds(listOf(methodIdWithoutType))
            //if code location was found return. no need to check the other language services
            if (methodWorkspaceUris.containsKey(methodIdWithoutType)) {
                return methodWorkspaceUris[methodIdWithoutType]
            }
        }
        return null
    }


    suspend fun getSpanLocation(spanId: String): Pair<String, Int>? {

        val spanIdWithoutType = CodeObjectsUtil.stripSpanPrefix(spanId)

        for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
            val spanWorkspaceUris =
                languageService.findWorkspaceUrisForSpanIds(listOf(spanIdWithoutType))
            //if code location was found return. no need to check the other language services
            if (spanWorkspaceUris.containsKey(spanIdWithoutType)) {
                return spanWorkspaceUris[spanIdWithoutType]
            }
        }
        return null
    }

    suspend fun buildPotentialMethodIds(codeObjectNavigation: CodeObjectNavigation): List<String> {
        val candidateSet = mutableSetOf<String>()
        codeObjectNavigation.navigationEntry.spanInfo?.methodCodeObjectId?.let {
            candidateSet.add(it)
        }
        codeObjectNavigation.navigationEntry.navEndpointEntry?.methodCodeObjectId?.let {
            candidateSet.add(it)
        }

        codeObjectNavigation.navigationEntry.navEndpointEntry?.endpointCodeObjectId?.also { it ->
            val endpointId = CodeObjectsUtil.stripEndpointPrefix(it)

            for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
                val endpointInfos = languageService.lookForDiscoveredEndpoints(endpointId)

                endpointInfos.forEach { endpointInfo ->
                    candidateSet.add(endpointInfo.methodCodeObjectId)
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
            EditorService.getInstance(project).openWorkspaceFileInEditor(fileUri, 1)
        }
    }


    fun canNavigateToEndpoint(endpointId: String?): Boolean {
        if (endpointId == null) {
            return false
        }

        val endpointIdWithoutType = CodeObjectsUtil.stripEndpointPrefix(endpointId)

        for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
            val endpointInfos = languageService.lookForDiscoveredEndpoints(endpointIdWithoutType)
            //if code location was found return. no need to check the other language services
            if (endpointInfos.isNotEmpty()) {
                //if code location was found link to it and return. no need to check the other language services
                return true
            }
        }

        return false
    }


    fun maybeNavigateToEndpointBySpan(spanId: String): Boolean {
        val endpointId = convertSpanIdToEndpointId(spanId)
        return endpointId?.let {
            tryNavigateToEndpointById(it)
        } ?: false
    }

    fun tryNavigateToEndpointById(endpointId: String): Boolean {

        val endpointIdWithoutType = CodeObjectsUtil.stripEndpointPrefix(endpointId)

        for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
            val endpointInfos = languageService.lookForDiscoveredEndpoints(endpointIdWithoutType)

            endpointInfos.firstOrNull()?.let { endpointInf ->
                endpointInf.file?.url?.let { url ->
                    EDT.ensureEDT {
                        EditorService.getInstance(project).openWorkspaceFileInEditor(url, endpointInf.offset)
                    }
                }
                ToolWindowShower.getInstance(project).showToolWindow()
                return true
            }
        }
        return false
    }


    private fun convertSpanIdToEndpointId(spanId: String): String? {
        if (spanId.contains("\$_$")) {
            val name = spanId.substring(spanId.indexOf("\$_$") + 3)
            return "epHTTP:$name"
        }
        return null
    }


}