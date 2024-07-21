package org.digma.intellij.plugin.analytics

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.auth.AuthManager
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.scheduling.ThreadPoolProviderService
import org.digma.intellij.plugin.settings.SettingsState

@Service(Service.Level.PROJECT)
class AnalyticsUrlProvider(private val project: Project) : DisposableAdaptor, BaseUrlProvider {

    private val logger: Logger = Logger.getInstance(AnalyticsUrlProvider::class.java)

    private var myApiUrl = SettingsState.getInstance().apiUrl


    companion object {
        @JvmStatic
        fun getInstance(project: Project): AnalyticsUrlProvider {
            return project.service<AnalyticsUrlProvider>()
        }
    }


    init {
        SettingsState.getInstance().addChangeListener({ state: SettingsState ->
            Backgroundable.executeOnPooledThread {
                Log.log(logger::trace, "got settings changed event")

                if (state.apiUrl != myApiUrl) {
                    Log.log(logger::trace, "api url changed to {}, replacing myApiUrl", state.apiUrl)
                    AuthManager.getInstance().logout()
                    AuthManager.getInstance().apiUrl = state.apiUrl
                    myApiUrl = state.apiUrl
                    ThreadPoolProviderService.getInstance().interruptAll()
                    AuthManager.getInstance().loginOrRefresh()
                    project.messageBus.syncPublisher(ApiClientChangedEvent.API_CLIENT_CHANGED_TOPIC).apiClientChanged(myApiUrl);
                }
            }
        }, this)
    }


    override fun baseUrl(): String {
        return myApiUrl
    }

}