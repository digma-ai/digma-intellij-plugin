package org.digma.intellij.plugin.idea.psi.discovery.endpoint

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.idea.psi.java.EndpointDiscovery
import org.digma.intellij.plugin.idea.psi.java.GrpcFramework
import org.digma.intellij.plugin.idea.psi.java.JaxrsJakartaFramework
import org.digma.intellij.plugin.idea.psi.java.JaxrsJavaxFramework
import org.digma.intellij.plugin.idea.psi.java.MicronautFramework
import org.digma.intellij.plugin.idea.psi.java.SpringBootFramework

@Service(Service.Level.PROJECT)
class EndpointDiscoveryService(project: Project) {


    val endpointDiscoveryList = mutableListOf<EndpointDiscovery>()


    companion object {
        @JvmStatic
        fun getInstance(project: Project): EndpointDiscoveryService {
            return project.service()
        }
    }


    init {
        val micronautFramework = MicronautFramework(project)
        val jaxrsJavaxFramework = JaxrsJavaxFramework(project)
        val jaxrsJakartaFramework = JaxrsJakartaFramework(project)
        val grpcFramework = GrpcFramework(project)
        val springBootFramework = SpringBootFramework(project)
        this.endpointDiscoveryList.addAll(
            listOf(
                micronautFramework,
                jaxrsJavaxFramework,
                jaxrsJakartaFramework,
                grpcFramework,
                springBootFramework
            )
        )
    }


}