package org.digma.intellij.plugin.idea.psi.discovery.endpoint

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

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