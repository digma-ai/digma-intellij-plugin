package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.Alarm
import com.intellij.util.messages.MessageBusConnection
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefMessageRouter
import org.cef.handler.CefDownloadHandler
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.ApiClientChangedEvent
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.analytics.InsightStatsChangedEvent
import org.digma.intellij.plugin.analytics.getAllEnvironments
import org.digma.intellij.plugin.auth.AuthManager
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.digmathon.DigmathonActivationEvent
import org.digma.intellij.plugin.digmathon.DigmathonProductKeyStateChangedEvent
import org.digma.intellij.plugin.digmathon.UserFinishedDigmathonEvent
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.environment.Env
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.observability.ObservabilityChanged
import org.digma.intellij.plugin.scope.ScopeChangedEvent
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.settings.SettingsState
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

    private val logger: Logger = Logger.getInstance(JCefComponent::class.java)

    private val settingsChangeListener: SettingsChangeListener
    private val analyticsServiceConnectionEventMessageBusConnection: MessageBusConnection
    private val settingsListenerParentDisposable = Disposer.newDisposable()
    private val authInfoChangeListenerParentDisposable = Disposer.newDisposable()
    private val connectionEventAlarmParentDisposable = Disposer.newDisposable()
    private val userRegistrationParentDisposable = Disposer.newDisposable()
    private val environmentChangeParentDisposable = Disposer.newDisposable()
    private val observabilityChangeParentDisposable = Disposer.newDisposable()
    private val scopeChangeParentDisposable = Disposer.newDisposable()
    private val stateChangeParentDisposable = Disposer.newDisposable()
    private val insightStatsChangeParentDisposable = Disposer.newDisposable()
    private val digmathonActivatedParentDisposable = Disposer.newDisposable()
    private val productKeyAddedParentDisposable = Disposer.newDisposable()
    private val userFinishedDigmathonParentDisposable = Disposer.newDisposable()
    private val apiClientChangedParentDisposable = Disposer.newDisposable()
    private val runConfigurationChangedParentDisposable = Disposer.newDisposable()


    init {
        val connectionEventAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, connectionEventAlarmParentDisposable)


        project.messageBus.connect(runConfigurationChangedParentDisposable).subscribe(RunManagerListener.TOPIC, object : RunManagerListener {
            override fun runConfigurationSelected(settings: RunnerAndConfigurationSettings?) {
                val setRunConfigurationMessageBuilder =
                    SetRunConfigurationMessageBuilder(jbCefBrowser.cefBrowser, RunManager.getInstance(project).selectedConfiguration)
                setRunConfigurationMessageBuilder.sendRunConfigurationAttributes()
            }

            override fun runConfigurationChanged(settings: RunnerAndConfigurationSettings) {
                //always send the selected one not necessarily the one that changed
                val setRunConfigurationMessageBuilder =
                    SetRunConfigurationMessageBuilder(jbCefBrowser.cefBrowser, RunManager.getInstance(project).selectedConfiguration)
                setRunConfigurationMessageBuilder.sendRunConfigurationAttributes()
            }
        })



        ApplicationManager.getApplication().messageBus.connect(apiClientChangedParentDisposable)
            .subscribe(ApiClientChangedEvent.API_CLIENT_CHANGED_TOPIC,
                ApiClientChangedEvent {

                    //there are multiple events fired when changing api url , or when changing login info, some run in background threads,
                    // for example every time user id changes the UI calls the INITIALIZE message to reset the view, settingChanged event is fired,
                    // authInfoChanged is fired. all these may run in background threads. in some cases there is a race condition and th UI
                    // will not receive the correct backend info if the message was sent before the api client was changed. this may case issues
                    // and may cause the UI not to show login screen when necessary.
                    //ApiClientChangedEvent is certain to be fired after the api client was changed and surely must set the correct info to the UI.
                    Backgroundable.executeOnPooledThread {
                        try {
                            sendUserInfoMessage(jbCefBrowser.cefBrowser, DigmaDefaultAccountHolder.getInstance().account?.userId, project)
                            val status = service<DockerService>().getCurrentDigmaInstallationStatusOnConnectionLost()
                            updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)
                            sendBackendAboutInfo(jbCefBrowser.cefBrowser, project)
                        } catch (e: Throwable) {
                            ErrorReporter.getInstance().reportError("JCefComponent.apiClientChanged", e)
                        }
                    }
                })


        ApplicationManager.getApplication().messageBus.connect(userFinishedDigmathonParentDisposable)
            .subscribe(
                UserFinishedDigmathonEvent.USER_FINISHED_DIGMATHON_TOPIC,
                UserFinishedDigmathonEvent {
                    sendUserFinishedDigmathon(jbCefBrowser.cefBrowser)
                })

        ApplicationManager.getApplication().messageBus.connect(productKeyAddedParentDisposable)
            .subscribe(DigmathonProductKeyStateChangedEvent.PRODUCT_KEY_STATE_CHANGED_TOPIC,
                DigmathonProductKeyStateChangedEvent { productKey ->
                    sendDigmathonProductKey(productKey, jbCefBrowser.cefBrowser)
                })

        ApplicationManager.getApplication().messageBus.connect(digmathonActivatedParentDisposable)
            .subscribe(DigmathonActivationEvent.DIGMATHON_ACTIVATION_TOPIC,
                DigmathonActivationEvent { isActive -> sendDigmathonState(isActive, jbCefBrowser.cefBrowser) })


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
        AuthManager.getInstance().addAuthInfoChangeListener({ authInfo ->
            try {
                if (logger.isTraceEnabled) {
                    Log.log(logger::trace, "got authInfoChanged for app {}, project {}, user id {}", name, project.name, authInfo.userId)
                }
                sendUserInfoMessage(jbCefBrowser.cefBrowser, authInfo.userId, project)
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("JCefComponent.userChanged", e)
            }
        }, authInfoChangeListenerParentDisposable)

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

                            sendBackendAboutInfo(jbCefBrowser.cefBrowser, project)

                        } catch (e: Exception) {
                            ErrorReporter.getInstance().reportError(project, "JCefComponent.connectionGained", e)
                        }
                    }, 2000)
                }
            })


        SettingsState.getInstance().addChangeListener({ settings ->
            //this event run on EDT
            Backgroundable.executeOnPooledThread {
                try {
                    val apiUrl = settings.apiUrl
                    sendApiUrl(jbCefBrowser.cefBrowser, apiUrl)
                    sendIsMicrometerProject(jbCefBrowser.cefBrowser, SpringBootMicrometerConfigureDepsService.isSpringBootWithMicrometer())
                    sendIsJaegerButtonEnabledMessage(jbCefBrowser.cefBrowser)
                    sendBackendAboutInfo(jbCefBrowser.cefBrowser, project)
                    val status = service<DockerService>().getCurrentDigmaInstallationStatusOnConnectionLost()
                    updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError("JCefComponent.settingsChanged", e)
                }
            }
        }, settingsListenerParentDisposable)


        project.messageBus.connect(userRegistrationParentDisposable).subscribe(
            UserRegistrationEvent.USER_REGISTRATION_TOPIC, object : UserRegistrationEvent {
                override fun userRegistered(email: String) {
                    try {
                        sendUserEmail(jbCefBrowser.cefBrowser, email)
                    } catch (e: Throwable) {
                        ErrorReporter.getInstance().reportError("JCefComponent.userRegistered", e)
                    }
                }
            })


        project.messageBus.connect(environmentChangeParentDisposable).subscribe(
            EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, object : EnvironmentChanged {
                override fun environmentChanged(newEnv: Env) {
                    try {
                        sendCurrentEnvironment(jbCefBrowser.cefBrowser, newEnv)
                    } catch (e: Throwable) {
                        ErrorReporter.getInstance().reportError("JCefComponent.environmentChanged", e)
                    }
                }

                override fun environmentsListChanged(newEnvironments: MutableList<Env>?) {
                    try {
                        sendEnvironmentsList(
                            jbCefBrowser.cefBrowser,
                            getAllEnvironments(project)
                        )
                    } catch (e: Throwable) {
                        ErrorReporter.getInstance().reportError("JCefComponent.environmentsListChanged", e)
                    }
                }
            })


        project.messageBus.connect(observabilityChangeParentDisposable).subscribe(
            ObservabilityChanged.OBSERVABILITY_CHANGED_TOPIC, object : ObservabilityChanged {
                override fun observabilityChanged(isObservabilityEnabled: Boolean) {
                    try {
                        sendObservabilityEnabledMessage(
                            jbCefBrowser.cefBrowser,
                            isObservabilityEnabled
                        )
                    } catch (e: Throwable) {
                        ErrorReporter.getInstance().reportError("JCefComponent.observabilityChanged", e)
                    }
                }
            }
        )

        project.messageBus.connect(scopeChangeParentDisposable).subscribe(
            ScopeChangedEvent.SCOPE_CHANGED_TOPIC, object : ScopeChangedEvent {
                override fun scopeChanged(
                    scope: SpanScope?, codeLocation: CodeLocation, hasErrors: Boolean,
                ) {
                    try {
                        val insightsStats = AnalyticsService.getInstance(project).getInsightsStats(scope?.spanCodeObjectId)
                        sendScopeChangedMessage(
                            jbCefBrowser.cefBrowser,
                            scope,
                            codeLocation,
                            hasErrors,
                            insightsStats?.analyticsInsightsCount ?: 0,
                            insightsStats?.issuesInsightsCount ?: 0,
                            insightsStats?.unreadInsightsCount ?: 0
                        )
                    } catch (e: Throwable) {
                        ErrorReporter.getInstance().reportError("JCefComponent.scopeChanged", e)
                    }
                }
            }
        )

        project.messageBus.connect(stateChangeParentDisposable).subscribe(
            StateChangedEvent.JCEF_STATE_CHANGED_TOPIC, object : StateChangedEvent {
                override fun stateChanged(state: JsonNode) {
                    try {
                        sendJcefStateMessage(jbCefBrowser.cefBrowser, state)
                    } catch (e: Throwable) {
                        ErrorReporter.getInstance().reportError("JCefComponent.stateChanged", e)
                    }
                }
            }
        )

        project.messageBus.connect(insightStatsChangeParentDisposable).subscribe(
            InsightStatsChangedEvent.INSIGHT_STATS_CHANGED_TOPIC, object : InsightStatsChangedEvent {
                override fun insightStatsChanged(
                    scope: JsonNode?,
                    analyticsInsightsCount: Int,
                    issuesInsightsCount: Int,
                    unreadInsightsCount: Int,
                    criticalInsightsCount: Int,
                    allIssuesCount: Int
                ) {
                    try {
                        sendSetInsightStatsMessage(
                            jbCefBrowser.cefBrowser,
                            scope,
                            analyticsInsightsCount,
                            issuesInsightsCount,
                            unreadInsightsCount,
                            criticalInsightsCount,
                            allIssuesCount
                        )
                    } catch (e: Throwable) {
                        ErrorReporter.getInstance().reportError("JCefComponent.insightStatsChanged", e)
                    }
                }
            }
        )
    }

    override fun dispose() {
        try {
            Disposer.dispose(runConfigurationChangedParentDisposable)
            Disposer.dispose(apiClientChangedParentDisposable)
            Disposer.dispose(userFinishedDigmathonParentDisposable)
            Disposer.dispose(productKeyAddedParentDisposable)
            Disposer.dispose(digmathonActivatedParentDisposable)
            Disposer.dispose(connectionEventAlarmParentDisposable)
            Disposer.dispose(analyticsServiceConnectionEventMessageBusConnection)
            Disposer.dispose(settingsListenerParentDisposable)
            Disposer.dispose(userRegistrationParentDisposable)
            Disposer.dispose(environmentChangeParentDisposable)
            Disposer.dispose(observabilityChangeParentDisposable)
            Disposer.dispose(scopeChangeParentDisposable)
            Disposer.dispose(stateChangeParentDisposable)
            Disposer.dispose(insightStatsChangeParentDisposable)
            Disposer.dispose(authInfoChangeListenerParentDisposable)
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

            jbCefClient.cefClient.addDisplayHandler(JCefDisplayHandler(name))

            val lifeSpanHandler: CefLifeSpanHandlerAdapter = object : CefLifeSpanHandlerAdapter() {
                override fun onAfterCreated(browser: CefBrowser) {
                    //schemeHandlerFactory must not be null here!
                    registerAppSchemeHandler(schemeHandlerFactory!!)
                    registerMailtoSchemeHandler(MailtoSchemaHandlerFactory())
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

        private fun registerMailtoSchemeHandler(schemeHandlerFactory: MailtoSchemaHandlerFactory) {
            CefApp.getInstance().registerSchemeHandlerFactory(
                schemeHandlerFactory.getSchema(), null, schemeHandlerFactory
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