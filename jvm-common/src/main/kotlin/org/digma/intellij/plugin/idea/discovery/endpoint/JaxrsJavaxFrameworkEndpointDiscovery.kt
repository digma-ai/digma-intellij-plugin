package org.digma.intellij.plugin.idea.discovery.endpoint

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.discovery.EndpointFramework

private const val JAXRS_PACKAGE_NAME = "javax.ws.rs"


class JaxrsJavaxFrameworkEndpointDiscovery(project: Project) : AbstractJaxrsFrameworkEndpointDiscover(project, JAXRS_PACKAGE_NAME) {
    override fun getName(): String {
        return "JaxrsJavax"
    }

    override fun getFramework(): EndpointFramework {
        return EndpointFramework.JaxrsJavax
    }
}
