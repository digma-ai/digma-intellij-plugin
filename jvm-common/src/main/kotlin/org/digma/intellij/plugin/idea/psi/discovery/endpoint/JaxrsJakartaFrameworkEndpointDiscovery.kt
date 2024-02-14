package org.digma.intellij.plugin.idea.psi.discovery.endpoint

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.discovery.EndpointFramework

private const val JAXRS_PACKAGE_NAME = "jakarta.ws.rs"

class JaxrsJakartaFrameworkEndpointDiscovery(project: Project) : AbstractJaxrsFrameworkEndpointDiscover(project, JAXRS_PACKAGE_NAME) {
    override fun getName(): String {
        return "JaxrsJakarta"
    }

    override fun getFramework(): EndpointFramework {
        return EndpointFramework.JaxrsJakarta
    }
}
