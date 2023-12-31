package org.digma.intellij.plugin.idea.psi.discovery.endpoint

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.idea.psi.JvmLanguageService
import org.digma.intellij.plugin.idea.psi.SupportedJvmLanguages
import org.digma.intellij.plugin.psi.LanguageService

@Suppress("LightServiceMigrationCode")
class EndpointDiscoveryService(project: Project) {


    val endpointDiscoveryList = mutableListOf<EndpointDiscovery>()


    companion object {
        @JvmStatic
        fun getInstance(project: Project): EndpointDiscoveryService {
            return project.service()
        }
    }


    init {
        //each JvmLanguageService contributes a list of EndpointDiscovery framework.
        //this is to avoid trying to discover endpoint for a language or framework that is not supported
        // by the running IDE.
        //for example: kotlin plugin is a bundled plugin but a user can disable it if they don't use kotlin. in that
        // case we don't need to run endpoint discovery related to kotlin framework because they may crash. if KtorFramework
        // is executed when kotlin plugin is disabled it will crash because kotlin classes and indexes are not available.
        //kotlin language by itself will also not be registered if kotlin plugin is disabled because it depends on its
        // existence in the IDE.
        SupportedJvmLanguages.values().forEach { language ->
            val languageService = LanguageService.findLanguageServiceByName(project, language.languages.languageServiceClassName)
            if (languageService != null && languageService is JvmLanguageService) {
                this.endpointDiscoveryList.addAll(languageService.getEndpointFrameworks(project))
            }
        }
    }


}