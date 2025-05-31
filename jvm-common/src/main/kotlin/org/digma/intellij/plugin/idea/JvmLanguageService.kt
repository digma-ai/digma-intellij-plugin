package org.digma.intellij.plugin.idea

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.idea.discovery.endpoint.EndpointDiscovery
import org.jetbrains.uast.UMethod

interface JvmLanguageService {

    fun getEndpointFrameworks(project: Project): Collection<EndpointDiscovery>

    fun getEndpointFrameworksRelevantOnlyForLanguage(project: Project): Collection<EndpointDiscovery>

    suspend fun findUMethodByMethodCodeObjectId(methodCodeObjectId: String?): UMethod?

}