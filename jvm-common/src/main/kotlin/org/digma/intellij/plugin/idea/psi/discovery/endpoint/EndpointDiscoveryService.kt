package org.digma.intellij.plugin.idea.psi.discovery.endpoint

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.idea.psi.JvmLanguageService
import org.digma.intellij.plugin.idea.psi.SupportedJvmLanguages
import org.digma.intellij.plugin.psi.LanguageService

//Do not change to light service because it will always register.
// we want it to register only in Idea.
// see: org.digma.intellij-with-jvm.xml
@Suppress("LightServiceMigrationCode")
class EndpointDiscoveryService(private val project: Project) {


    companion object {
        @JvmStatic
        fun getInstance(project: Project): EndpointDiscoveryService {
            return project.service()
        }
    }


    /**
     * returns all possible EndpointDiscovery, used when building endpoint navigation when we don't
     * know what files are in the project, may be java and kotlin files mixed.
     */
    fun getAllEndpointDiscovery(): List<EndpointDiscovery> {

        val micronautFramework = MicronautFrameworkEndpointDiscovery(project)
        val jaxrsJavaxFramework = JaxrsJavaxFrameworkEndpointDiscovery(project)
        val jaxrsJakartaFramework = JaxrsJakartaFrameworkEndpointDiscovery(project)
        val grpcFramework = GrpcFrameworkEndpointDiscovery(project)
        val springBootFramework = SpringBootFrameworkEndpointDiscovery(project)
        val ktorFramework = KtorFrameworkEndpointDiscovery(project)
        return listOf(
            micronautFramework,
            jaxrsJavaxFramework,
            jaxrsJakartaFramework,
            grpcFramework,
            springBootFramework,
            ktorFramework
        )
    }


    fun getEndpointDiscoveryForLanguage(psiFile: PsiFile): List<EndpointDiscovery> {

        val endpointDiscoveryList = mutableListOf<EndpointDiscovery>()
        SupportedJvmLanguages.values().forEach { lang ->
            val languageService = LanguageService.findLanguageServiceByName(project, lang.language.languageServiceClassName)
            if (languageService != null &&
                languageService is JvmLanguageService &&
                languageService.isSupportedFile(psiFile)
            ) {

                endpointDiscoveryList.addAll(languageService.getEndpointFrameworks(project))
            }
        }
        return endpointDiscoveryList
    }

    //todo: this method requires read access because languageService.isSupportedFile(project,virtualFile)
    // requires read access. keep here as example and reminder
//    fun getEndpointDiscoveryForLanguage(virtualFile: VirtualFile):List<EndpointDiscovery>{
//
//        val endpointDiscoveryList = mutableListOf<EndpointDiscovery>()
//        SupportedJvmLanguages.values().forEach { lang ->
//            val languageService = LanguageService.findLanguageServiceByName(project, lang.languages.languageServiceClassName)
//            if (languageService != null &&
//                languageService is JvmLanguageService &&
//                languageService.isSupportedFile(project,virtualFile)) {
//
//                endpointDiscoveryList.addAll(languageService.getEndpointFrameworks(project))
//            }
//        }
//        return endpointDiscoveryList
//    }


}