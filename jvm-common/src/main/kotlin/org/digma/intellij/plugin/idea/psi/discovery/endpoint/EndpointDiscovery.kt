package org.digma.intellij.plugin.idea.psi.discovery.endpoint

import com.intellij.psi.search.GlobalSearchScope
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.progress.ProcessContext
import org.digma.intellij.plugin.psi.BuildDocumentInfoProcessContext
import org.digma.intellij.plugin.psi.PsiFileCachedValueWithUri
import org.digma.intellij.plugin.psi.PsiUtils
import java.util.Objects

abstract class EndpointDiscovery {

    abstract fun getName(): String

    abstract fun lookForEndpoints(searchScopeProvider: SearchScopeProvider, context: ProcessContext): List<EndpointInfo>?


    fun endpointDiscovery(psiFileCachedValue: PsiFileCachedValueWithUri, documentInfo: DocumentInfo, context: BuildDocumentInfoProcessContext) {

        val psiFile = psiFileCachedValue.value ?: return
        if (!PsiUtils.isValidPsiFile(psiFile)) {
            return
        }

        val endpointInfos = lookForEndpoints({ GlobalSearchScope.fileScope(psiFile) }, context)

        endpointInfos?.let { infos ->
            for (endpointInfo in infos) {
                val methodId = endpointInfo.containingMethodId
                val methodInfo = documentInfo.methods[methodId]

                try {
                    //this method must exist in the document info
                    Objects.requireNonNull(methodInfo) {
                        val methodIds = documentInfo.methods.map { it.key }.joinToString(",")
                        "method info $methodId must exist in DocumentInfo for ${documentInfo.fileUri} , methods: [$methodIds]"
                    }
                    methodInfo?.addEndpoint(endpointInfo)
                } catch (e: Exception) {
                    ErrorReporter.getInstance().reportError("${this::class.java}.endpointDiscovery.${getName()}", e)
                }
            }
        }
    }
}