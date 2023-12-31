package org.digma.intellij.plugin.idea.psi

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscovery

interface JvmLanguageService {

    fun getEndpointFrameworks(project: Project): Collection<EndpointDiscovery>

}