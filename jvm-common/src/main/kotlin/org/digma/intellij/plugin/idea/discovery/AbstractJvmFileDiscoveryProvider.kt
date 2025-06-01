package org.digma.intellij.plugin.idea.discovery

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.discovery.FileDiscoveryProvider
import org.digma.intellij.plugin.discovery.model.EndpointDiscoveryInfo
import org.digma.intellij.plugin.discovery.model.FileDiscoveryInfo
import org.digma.intellij.plugin.discovery.model.MethodDiscoveryInfo
import org.digma.intellij.plugin.discovery.model.SpanDiscoveryInfo
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.psi.LanguageServiceProvider

abstract class AbstractJvmFileDiscoveryProvider: FileDiscoveryProvider {

    protected  val logger = thisLogger()

    //todo: WIP:
    //this is a new api to build file discovery.
    //currently used only in jvm navigation discovery.
    //it uses the old api of buildDocumentInfo, and it translates DocumentInfo to FileDiscoveryInfo.
    //it will be replaced with new discovery infrustructure in the future.
    //currently it enables running navigation discovery in a coroutine context, which was not possible before without a lot of refactoring.
    //in the future we should change this code to do discovery per file using UAST.
    override suspend fun discover(
        project: Project,
        file: VirtualFile
    ): FileDiscoveryInfo {
        DumbService.getInstance(project).waitForSmartMode()
        if(!isValidVirtualFile(file)){
            return FileDiscoveryInfo(file)
        }
        Log.trace(logger, project, "starting discovery for {}", file)

        val languageService = LanguageServiceProvider.getInstance(project).getLanguageService(file)
        if (languageService == null) {
            Log.warn(logger, project, "LanguageService is null for {}", file)
            return FileDiscoveryInfo(file)
        }

        val documentInfo = languageService.buildDocumentInfo(file)
        if (documentInfo == null) {
            Log.warn(logger, project, "documentInfo is null for {}", file)
            return FileDiscoveryInfo(file,languageService.getLanguage())
        }

        Log.trace(logger, project, "got document info for file {}", file)
        val fileDiscoveryInfo = FileDiscoveryInfo(file,languageService.getLanguage())
        documentInfo.methods.forEach { method ->
            fileDiscoveryInfo.methods[method.key] = toNewMethod(fileDiscoveryInfo,method.value)
        }

        Log.trace(logger, project, "finished discovery for {}", file)
        return fileDiscoveryInfo
    }

    private fun toNewMethod(fileDiscoveryInfo: FileDiscoveryInfo, method: MethodInfo): MethodDiscoveryInfo {
        val methodDiscoveryInfo = MethodDiscoveryInfo(fileDiscoveryInfo,method.id,method.name,method.containingClass,method.containingNamespace)
        method.spans.forEach { span ->
            val spanDiscoveryInfo = SpanDiscoveryInfo(methodDiscoveryInfo,span.id,span.name,span.offset)
            methodDiscoveryInfo.spans.add(spanDiscoveryInfo)
        }

        method.endpoints.forEach { endpoint ->
            val endpointDiscoveryInfo = EndpointDiscoveryInfo(methodDiscoveryInfo, endpoint.id, endpoint.textRange?.start ?: 0,endpoint.framework)
            methodDiscoveryInfo.endpoints.add(endpointDiscoveryInfo)
        }

        return methodDiscoveryInfo
    }

}