package org.digma.intellij.plugin.idea.psi.discovery.endpoint

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.idea.psi.JvmLanguageService
import org.digma.intellij.plugin.psi.LanguageServiceProvider

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

        //some frameworks are common to all jvm languages and some frameworks are only for a specific language,
        // for example ktor is relevant only for kotlin and should be used only if kotlin plugin is enabled

        val endpointDiscoveryList = mutableListOf<EndpointDiscovery>()
        endpointDiscoveryList.add(MicronautFrameworkEndpointDiscovery(project))
        endpointDiscoveryList.add(JaxrsJavaxFrameworkEndpointDiscovery(project))
        endpointDiscoveryList.add(JaxrsJakartaFrameworkEndpointDiscovery(project))
        endpointDiscoveryList.add(GrpcFrameworkEndpointDiscovery(project))
        endpointDiscoveryList.add(SpringBootFrameworkEndpointDiscovery(project))

        for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
            if (languageService is JvmLanguageService) {
                endpointDiscoveryList.addAll(languageService.getEndpointFrameworksRelevantOnlyForLanguage(project))
            }
        }

        return endpointDiscoveryList
    }


    fun getEndpointDiscoveryForLanguage(psiFile: PsiFile): List<EndpointDiscovery> {
        val endpointDiscoveryList = mutableListOf<EndpointDiscovery>()
        for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
            if (languageService is JvmLanguageService && languageService.isSupportedFile(psiFile)) {
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