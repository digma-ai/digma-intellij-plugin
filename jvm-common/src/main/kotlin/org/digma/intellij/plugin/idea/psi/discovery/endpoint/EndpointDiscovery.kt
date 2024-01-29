package org.digma.intellij.plugin.idea.psi.discovery.endpoint

import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.psi.PsiUtils
import java.util.Objects

abstract class EndpointDiscovery {

    //must run with read access
    //using searchScope supplier because building SearchScope needs read access
    abstract fun lookForEndpoints(searchScopeProvider: SearchScopeProvider): List<EndpointInfo>?

    // default method uses fileScope. however, in some cases logic could be bit different
    open fun lookForEndpoints(psiFile: PsiFile): List<EndpointInfo>? {
        return if (PsiUtils.isValidPsiFile(psiFile)) {
            lookForEndpoints { GlobalSearchScope.fileScope(psiFile) }
        } else {
            listOf()
        }
    }


    fun endpointDiscovery(psiFile: PsiFile, documentInfo: DocumentInfo) {
        val endpointInfos = lookForEndpoints(psiFile)

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
                    ErrorReporter.getInstance().reportError("${this::class.java}.endpointDiscovery", e)
                }
            }
        }
    }
}