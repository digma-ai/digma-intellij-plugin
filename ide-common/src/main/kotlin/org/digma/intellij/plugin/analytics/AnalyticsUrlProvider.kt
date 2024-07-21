package org.digma.intellij.plugin.analytics

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.digma.intellij.plugin.auth.AuthManager
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.scheduling.ThreadPoolProviderService
import org.digma.intellij.plugin.settings.SettingsState
import java.util.function.Consumer

@Service(Service.Level.APP)
class AnalyticsUrlProvider : DisposableAdaptor, BaseUrlProvider {

    private val logger: Logger = Logger.getInstance(AnalyticsUrlProvider::class.java)

    private var myApiUrl = SettingsState.getInstance().apiUrl

    private val myListeners = mutableListOf<Pair<Int, BaseUrlProvider.UrlChangedListener>>()


    companion object {
        @JvmStatic
        fun getInstance(): AnalyticsUrlProvider {
            return service<AnalyticsUrlProvider>()
        }
    }


    init {
        SettingsState.getInstance().addChangeListener({ state: SettingsState ->
            Backgroundable.executeOnPooledThread {
                Log.log(logger::trace, "got settings changed event")

                if (state.apiUrl != myApiUrl) {
                    Log.log(logger::trace, "api url changed to {}, replacing myApiUrl", state.apiUrl)
                    AuthManager.getInstance().logout()
                    ThreadPoolProviderService.getInstance().interruptAll()
                    val oldUrl = myApiUrl
                    myApiUrl = state.apiUrl
                    //clients should be replaced now, this is a synchronous call that will return only after all
                    // listeners replaced their client
                    fireUrlChangedEvent(oldUrl, myApiUrl)
                    //after clients replaced do loginOrRefresh
                    AuthManager.getInstance().loginOrRefresh()
                    doForAllProjects { project ->
                        project.messageBus.syncPublisher(ApiClientChangedEvent.API_CLIENT_CHANGED_TOPIC).apiClientChanged(myApiUrl)
                    }
                }
            }
        }, this)
    }



    override fun baseUrl(): String {
        return myApiUrl
    }


    //lower order called before higher order
    override fun addUrlChangedListener(urlChangedListener: BaseUrlProvider.UrlChangedListener, order: Int) {
        myListeners.add(Pair(order, urlChangedListener))
    }

    override fun removeUrlChangedListener(urlChangedListener: BaseUrlProvider.UrlChangedListener) {
        val toRemove = myListeners.find { it.second === urlChangedListener }
        toRemove?.let {
            myListeners.remove(it)
        }
    }

    //this event must be synchronous, all listeners should replace their client.
    //we have only 2 client, the main client used by the application and a client used by AuthManager
    private fun fireUrlChangedEvent(oldUrl: String, newUrl: String) {
        myListeners.sortedBy { it.first }.forEach {
            it.second.urlChanged(BaseUrlProvider.UrlChangedEvent(oldUrl, newUrl))
        }
    }


    private fun doForAllProjects(consumer: Consumer<Project>) {
        ProjectManager.getInstance().openProjects.forEach { project ->
            if (isProjectValid(project)) {
                consumer.accept(project)
            }
        }
    }
}