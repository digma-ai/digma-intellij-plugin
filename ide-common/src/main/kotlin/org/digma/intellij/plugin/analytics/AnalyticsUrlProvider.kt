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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

@Service(Service.Level.APP)
class AnalyticsUrlProvider : DisposableAdaptor, BaseUrlProvider {

    private val logger: Logger = Logger.getInstance(AnalyticsUrlProvider::class.java)

    private var myApiUrl = SettingsState.getInstance().apiUrl

    /*
    AnalyticsUrlProvider also provides the api token to org.digma.intellij.plugin.auth.SettingsTokenProvider.
    the reason SettingsTokenProvider doesn't take the token directly from SettingsState:
    when user changes the api url, lets say from centralized to local, they will also remove the token.
    but when changing url here in settingsChanged the first thing we want to do is logout from the old
    server. but if the token doesn't exist in SettingsState then AuthManager.logoutSynchronously will fail
    to create a LoginHandler. to create a LoginHandler we need to know if its centralized or local,and for that we need
    to call analyticsProvider.about, that will fail without a token and the logout will fail to create a LoginHandler and
    will return a NoOpLoginHandler. luckily NoOpLoginHandler knows how to logout because our logout is only deleting the account.
    but for the good order and for future needs it's better that we succeed to create LoginHandler.
    So the token is provided by AnalyticsUrlProvider and it is synced with the settings every time the settings change.
     */
    private var myApiToken = SettingsState.getInstance().apiToken

    private val myListeners = CopyOnWriteArrayList<Pair<Int, BaseUrlProvider.UrlChangedListener>>()


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
                    AuthManager.getInstance().stopAutoRefresh("on api url changed")
                    AuthManager.getInstance().logoutSynchronously()
                    ThreadPoolProviderService.getInstance().interruptAll()
                    val oldUrl = myApiUrl
                    myApiUrl = state.apiUrl
                    myApiToken = state.apiToken
                    //clients should be replaced now, this is a synchronous call that will return only after all
                    // listeners replaced their client
                    fireUrlChangedEvent(oldUrl, myApiUrl)
                    //after clients replaced do loginOrRefresh
                    AuthManager.getInstance().loginOrRefreshAsync()
                    AuthManager.getInstance().startAutoRefresh()
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

    fun apiToken(): String? {
        return myApiToken
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