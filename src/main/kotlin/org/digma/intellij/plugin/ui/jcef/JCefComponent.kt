package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
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
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.env.Env
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.observability.ObservabilityChanged
import org.digma.intellij.plugin.scope.ScopeChangedEvent
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.jcef.model.BackendInfoMessage
import org.digma.intellij.plugin.ui.jcef.state.StateChangedEvent
import org.digma.intellij.plugin.ui.settings.ApplicationUISettingsChangeNotifier
import org.digma.intellij.plugin.ui.settings.SettingsChangeListener
import org.digma.intellij.plugin.ui.settings.Theme
import java.util.Objects
import javax.swing.JComponent

class JCefComponent
private constructor(
    val project: Project,
    val name: String,
    val jbCefBrowser: JBCefBrowser,
    private val cefMessageRouter: CefMessageRouter,
    val messageRouterHandlers: MutableList<BaseMessageRouterHandler>,
    val schemeHandlerFactory: BaseSchemeHandlerFactory?,
    private val lifeSpanHandler: CefLifeSpanHandlerAdapter,
) : Disposable {


    private val settingsChangeListener: SettingsChangeListener
    private val analyticsServiceConnectionEventMessageBusConnection: MessageBusConnection
    private val settingsListenerParentDisposable = Disposer.newDisposable()
    private val connectionEventAlarmParentDisposable = Disposer.newDisposable()
    private val userRegistrationParentDisposable = Disposer.newDisposable()
    private val environmentChangeParentDisposable = Disposer.newDisposable()
    private val observabilityChangeParentDisposable = Disposer.newDisposable()
    private val scopeChangeParentDisposable = Disposer.newDisposable()
    private val stateChangeParentDisposable = Disposer.newDisposable()


    init {
        val connectionEventAlarm = AlarmFactory.getInstance().create(Alarm.ThreadToUse.POOLED_THREAD, connectionEventAlarmParentDisposable)

        settingsChangeListener = object : SettingsChangeListener {
            override fun systemFontChange(fontName: String) {
                sendRequestToChangeFont(fontName, jbCefBrowser)
            }

            override fun systemThemeChange(theme: Theme) {
                sendRequestToChangeUiTheme(theme, jbCefBrowser)
            }

            override fun editorFontChange(fontName: String) {
                sendRequestToChangeCodeFont(fontName, jbCefBrowser)
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
                            ErrorReporter.getInstance().reportError(project, "JCefComponent.connectionLost", e)
                        }
                    }, 2000)
                }

                override fun connectionGained() {
                    //delay before reporting digma status to let all containers go up
                    connectionEventAlarm.cancelAllRequests()
                    connectionEventAlarm.addRequest({
                        try {
                            val status = service<DockerService>().getCurrentDigmaInstallationStatusOnConnectionGained()
                            updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)

                            val about = AnalyticsService.getInstance(project).about
                            val message = BackendInfoMessage(about)
                            serializeAndExecuteWindowPostMessageJavaScript(jbCefBrowser.cefBrowser, message)

                        } catch (e: Exception) {
                            ErrorReporter.getInstance().reportError(project, "JCefComponent.connectionGained", e)
                        }
                    }, 2000)
                }
            })


        SettingsState.getInstance().addChangeListener({ settings ->
            val apiUrl = settings.apiUrl
            sendApiUrl(jbCefBrowser.cefBrowser, apiUrl)
            sendIsMicrometerProject(jbCefBrowser.cefBrowser, SpringBootMicrometerConfigureDepsService.isSpringBootWithMicrometer())
            sendIsJaegerButtonEnabledMessage(jbCefBrowser.cefBrowser)
        }, settingsListenerParentDisposable)


        project.messageBus.connect(userRegistrationParentDisposable).subscribe(
            UserRegistrationEvent.USER_REGISTRATION_TOPIC, object : UserRegistrationEvent {
                override fun userRegistered(email: String) {
                    sendUserEmail(jbCefBrowser.cefBrowser, email)
                }
            })


        project.messageBus.connect(environmentChangeParentDisposable).subscribe(
            EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, object : EnvironmentChanged {
                override fun environmentChanged(newEnv: Env) {
                    sendCurrentEnvironment(jbCefBrowser.cefBrowser, newEnv)
                }

                override fun environmentsListChanged(newEnvironments: MutableList<Env>?) {
                    sendEnvironmentsList(
                        jbCefBrowser.cefBrowser,
                        AnalyticsService.getInstance(project).environment.getEnvironments()
                    )
                }
            })


        project.messageBus.connect(observabilityChangeParentDisposable).subscribe(
            ObservabilityChanged.OBSERVABILITY_CHANGED_TOPIC, object : ObservabilityChanged {
                override fun observabilityChanged(isObservabilityEnabled: Boolean) {
                    sendObservabilityEnabledMessage(
                        jbCefBrowser.cefBrowser,
                        isObservabilityEnabled
                    )
                }
            }
        )

        project.messageBus.connect(scopeChangeParentDisposable).subscribe(
            ScopeChangedEvent.SCOPE_CHANGED_TOPIC, object : ScopeChangedEvent {
                override fun scopeChanged(
                    scope: SpanScope?, codeLocation: CodeLocation, hasErrors: Boolean,
                ) {
                    sendScopeChangedMessage(jbCefBrowser.cefBrowser, scope, codeLocation, hasErrors)
                }
            }
        )

        project.messageBus.connect(stateChangeParentDisposable).subscribe(
            StateChangedEvent.JCEF_STATE_CHANGED_TOPIC, object : StateChangedEvent {
                override fun stateChanged(state: JsonNode) {
                    sendJcefStateMessage(jbCefBrowser.cefBrowser, state)
                }
            }
        )

    }


    override fun dispose() {
        try {
            Disposer.dispose(connectionEventAlarmParentDisposable)
            Disposer.dispose(analyticsServiceConnectionEventMessageBusConnection)
            Disposer.dispose(settingsListenerParentDisposable)
            Disposer.dispose(userRegistrationParentDisposable)
            Disposer.dispose(environmentChangeParentDisposable)
            Disposer.dispose(observabilityChangeParentDisposable)
            Disposer.dispose(scopeChangeParentDisposable)
            Disposer.dispose(stateChangeParentDisposable)
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


    //must provide a parentDisposable that is not the project
    class JCefComponentBuilder(private val project: Project, private val name: String, private var parentDisposable: Disposable) {

        private var url: String? = null
        private var messageRouterHandlers: MutableList<BaseMessageRouterHandler> = mutableListOf()
        private var schemeHandlerFactory: BaseSchemeHandlerFactory? = null
        private var downloadAdapter: CefDownloadHandler? = null


        fun build(): JCefComponent {

            Objects.requireNonNull(url, "url must not be null")
            Objects.requireNonNull(messageRouterHandlers, "messageRouterHandlers must not be null")
            Objects.requireNonNull(schemeHandlerFactory, "schemeHandlerFactory must not be null")

            val jbCefBrowser = JBCefBrowserBuilderCreator.create()
                .setUrl(url)
                .build()

            val jbCefClient = jbCefBrowser.jbCefClient
            val cefMessageRouter = CefMessageRouter.create()

            messageRouterHandlers.forEach {
                cefMessageRouter.addHandler(it, true)
            }

            jbCefClient.cefClient.addMessageRouter(cefMessageRouter)

            jbCefClient.addDisplayHandler(JCefDisplayHandler(name), jbCefBrowser.cefBrowser)

            val lifeSpanHandler: CefLifeSpanHandlerAdapter = object : CefLifeSpanHandlerAdapter() {
                override fun onAfterCreated(browser: CefBrowser) {
                    registerAppSchemeHandler(schemeHandlerFactory!!) //schemeHandlerFactory must not be null here
                }
            }

            jbCefBrowser.jbCefClient.addLifeSpanHandler(lifeSpanHandler, jbCefBrowser.cefBrowser)

            val jCefComponent =
                JCefComponent(project, name, jbCefBrowser, cefMessageRouter, messageRouterHandlers, schemeHandlerFactory, lifeSpanHandler)

            parentDisposable.let {
                Disposer.register(it) {
                    jbCefBrowser.jbCefClient.removeLifeSpanHandler(lifeSpanHandler, jbCefBrowser.cefBrowser)
                    jCefComponent.dispose()
                }
            }

            downloadAdapter?.let {
                jbCefClient.cefClient.addDownloadHandler(it)
            }

            return jCefComponent
        }


        private fun registerAppSchemeHandler(schemeHandlerFactory: BaseSchemeHandlerFactory) {
            CefApp.getInstance().registerSchemeHandlerFactory(
                schemeHandlerFactory.getSchema(), schemeHandlerFactory.getDomain(), schemeHandlerFactory
            )
        }


        fun url(url: String): JCefComponentBuilder {
            this.url = url
            return this
        }

        fun addMessageRouterHandler(messageRouterHandler: BaseMessageRouterHandler): JCefComponentBuilder {
            this.messageRouterHandlers.add(messageRouterHandler)
            return this
        }

        fun schemeHandlerFactory(schemeHandlerFactory: BaseSchemeHandlerFactory): JCefComponentBuilder {
            this.schemeHandlerFactory = schemeHandlerFactory
            return this
        }

        fun withDownloadAdapter(adapter: CefDownloadHandler): JCefComponentBuilder {
            this.downloadAdapter = adapter
            return this
        }
    }
}