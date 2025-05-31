package org.digma.intellij.plugin.idea.discovery.endpoint

import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import java.util.Objects

abstract class EndpointDiscovery {

    abstract fun getName(): String

    abstract suspend fun lookForEndpoints(searchScopeProvider: SearchScopeProvider): List<EndpointInfo>?


    suspend fun endpointDiscovery(psiFile: PsiFile, documentInfo: DocumentInfo) {

        val endpointInfos = lookForEndpoints { GlobalSearchScope.fileScope(psiFile) }

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
                    //report the exception and continue instead of failing the whole discovery.
                    ErrorReporter.getInstance().reportError("${this::class.java}.endpointDiscovery.${getName()}", e)
                }
            }
        }
    }
}