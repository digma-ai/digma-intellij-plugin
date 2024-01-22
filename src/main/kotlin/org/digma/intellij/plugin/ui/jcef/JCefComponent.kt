package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.Alarm
import com.intellij.util.AlarmFactory
import com.intellij.util.messages.MessageBusConnection
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefMessageRouter
import org.cef.handler.CefDownloadHandler
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil
import org.digma.intellij.plugin.jcef.common.UserRegistrationEvent
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.settings.ApplicationUISettingsChangeNotifier
import org.digma.intellij.plugin.ui.settings.SettingsChangeListener
import org.digma.intellij.plugin.ui.settings.Theme
import java.util.Objects
import javax.swing.JComponent

class JCefComponent(
    val project: Project,
    val jbCefBrowser: JBCefBrowser,
    val cefMessageRouter: CefMessageRouter,
    val messageRouterHandler: BaseMessageRouterHandler?,
    val schemeHandlerFactory: BaseSchemeHandlerFactory?,
    val lifeSpanHandler: CefLifeSpanHandlerAdapter,
) : Disposable {


    private val settingsChangeListener: SettingsChangeListener
    private val analyticsServiceConnectionEventMessageBusConnection: MessageBusConnection
    private val settingsListenerParentDisposable = Disposer.newDisposable()
    private val connectionEventAlarmParentDisposable = Disposer.newDisposable()
    private val userRegistrationParentDisposable = Disposer.newDisposable()


    init {
        val connectionEventAlarm = AlarmFactory.getInstance().create(Alarm.ThreadToUse.POOLED_THREAD, connectionEventAlarmParentDisposable)

        settingsChangeListener = object : SettingsChangeListener {
            override fun systemFontChange(fontName: String) {
                JCefBrowserUtil.sendRequestToChangeFont(fontName, jbCefBrowser)
            }

            override fun systemThemeChange(theme: Theme) {
                JCefBrowserUtil.sendRequestToChangeUiTheme(theme, jbCefBrowser)
            }

            override fun editorFontChange(fontName: String) {
                JCefBrowserUtil.sendRequestToChangeCodeFont(fontName, jbCefBrowser)
            }
        }

        ApplicationUISettingsChangeNotifier.getInstance(project).addSettingsChangeListener(settingsChangeListener)

        analyticsServiceConnectionEventMessageBusConnection = project.messageBus.connect()
        analyticsServiceConnectionEventMessageBusConnection.subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, object : AnalyticsServiceConnectionEvent {

                override fun connectionLost() {
                    //delay before reporting digma status to let all containers go down
                    connectionEventAlarm.cancelAllRequests()
                    connectionEventAlarm.addRequest({
                        try {
                            val status = service<DockerService>().getCurrentDigmaInstallationStatusOnConnectionLost()
                            updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)
                        } catch (e: Exception) {
                            ErrorReporter.getInstance().reportError("JCefComponent.connectionLost", e)
                        }
                    }, 5000)
                }

                override fun connectionGained() {
                    //delay before reporting digma status to let all containers go up
                    connectionEventAlarm.cancelAllRequests()
                    connectionEventAlarm.addRequest({
                        try {
                            val status = service<DockerService>().getCurrentDigmaInstallationStatusOnConnectionGained()
                            updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)
                        } catch (e: Exception) {
                            ErrorReporter.getInstance().reportError("JCefComponent.connectionGained", e)
                        }
                    }, 5000)
                }
            })


        SettingsState.getInstance().addChangeListener({ settings ->
            val apiUrl = settings.apiUrl
            sendApiUrl(jbCefBrowser.cefBrowser, apiUrl)
            sendIsMicrometerProject(jbCefBrowser.cefBrowser, SpringBootMicrometerConfigureDepsService.isSpringBootWithMicrometer())
        }, settingsListenerParentDisposable)


        project.messageBus.connect(userRegistrationParentDisposable).subscribe(
            UserRegistrationEvent.USER_REGISTRATION_TOPIC,
            UserRegistrationEvent { email -> sendUserEmail(jbCefBrowser.cefBrowser, email) })

    }


    override fun dispose() {
        try {
            Disposer.dispose(connectionEventAlarmParentDisposable)
            Disposer.dispose(analyticsServiceConnectionEventMessageBusConnection)
            Disposer.dispose(settingsListenerParentDisposable)
            Disposer.dispose(userRegistrationParentDisposable)
            jbCefBrowser.jbCefClient.removeLifeSpanHandler(lifeSpanHandler, jbCefBrowser.cefBrowser)
            jbCefBrowser.dispose()
            cefMessageRouter.dispose()
            ApplicationUISettingsChangeNotifier.getInstance(project).removeSettingsChangeListener(settingsChangeListener)
        } catch (e: Exception) {
            ErrorReporter.getInstance().reportError(project, "JCefComponent.dispose", e)
        }
    }

    fun getComponent(): JComponent {
        return jbCefBrowser.component
    }
}


class JCefComponentBuilder(val project: Project) {

    private var url: String? = null
    private var messageRouterHandler: BaseMessageRouterHandler? = null
    private var schemeHandlerFactory: BaseSchemeHandlerFactory? = null
    private var parentDisposable: Disposable? = null
    private var downloadAdapter: CefDownloadHandler? =null;


    fun build(): JCefComponent {

        Objects.requireNonNull(url, "url must not be null")
        Objects.requireNonNull(messageRouterHandler, "messageRouterHandler must not be null")
        Objects.requireNonNull(schemeHandlerFactory, "schemeHandlerFactory must not be null")

        //todo: move the code from JBCefBrowserBuilderCreator to here when all apps use the same infrastructure
        val jbCefBrowser = JBCefBrowserBuilderCreator.create()
            .setUrl(url)
            .build()

        val jbCefClient = jbCefBrowser.jbCefClient
        val cefMessageRouter = CefMessageRouter.create()
        cefMessageRouter.addHandler(messageRouterHandler, true)
        jbCefClient.cefClient.addMessageRouter(cefMessageRouter)

        val lifeSpanHandler: CefLifeSpanHandlerAdapter = object : CefLifeSpanHandlerAdapter() {
            override fun onAfterCreated(browser: CefBrowser) {
                registerAppSchemeHandler(schemeHandlerFactory)
            }
        }

        jbCefBrowser.jbCefClient.addLifeSpanHandler(lifeSpanHandler, jbCefBrowser.cefBrowser)

        val jCefComponent = JCefComponent(project, jbCefBrowser, cefMessageRouter, messageRouterHandler, schemeHandlerFactory, lifeSpanHandler)

        parentDisposable?.let {
            Disposer.register(it) {
                jbCefBrowser.jbCefClient.removeLifeSpanHandler(lifeSpanHandler, jbCefBrowser.cefBrowser)
                jCefComponent.dispose()
            }
        }

        downloadAdapter?.let {
            jbCefClient.cefClient.addDownloadHandler(it);
        }

        return jCefComponent
    }


    private fun registerAppSchemeHandler(schemeHandlerFactory: BaseSchemeHandlerFactory?) {

        Objects.requireNonNull(schemeHandlerFactory, "schemeHandlerFactory must not be null")

        CefApp.getInstance().registerSchemeHandlerFactory(
            schemeHandlerFactory!!.getSchema(), schemeHandlerFactory.getDomain(), schemeHandlerFactory
        )
    }


    fun url(url: String): JCefComponentBuilder {
        this.url = url
        return this
    }

    fun messageRouterHandler(messageRouterHandler: BaseMessageRouterHandler): JCefComponentBuilder {
        this.messageRouterHandler = messageRouterHandler
        return this
    }

    fun schemeHandlerFactory(schemeHandlerFactory: BaseSchemeHandlerFactory): JCefComponentBuilder {
        this.schemeHandlerFactory = schemeHandlerFactory
        return this
    }

    fun withParentDisposable(parentDisposable: Disposable): JCefComponentBuilder {
        this.parentDisposable = parentDisposable
        return this
    }

    fun withDownloadAdapter(adapter: CefDownloadHandler): JCefComponentBuilder {
        this.downloadAdapter = adapter
        return this;
    }
}