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
import org.apache.maven.artifact.versioning.ComparableVersion
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefMessageRouter
import org.cef.handler.CefDownloadHandler
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.ApiClientChangedEvent
import org.digma.intellij.plugin.analytics.BackendInfoHolder
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.analytics.InsightStatsChangedEvent
import org.digma.intellij.plugin.analytics.getAllEnvironments
import org.digma.intellij.plugin.auth.AuthManager
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.newerThan
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
import org.digma.intellij.plugin.scope.ScopeContext
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.jcef.state.StateChangedEvent
import org.digma.intellij.plugin.ui.settings.ApplicationUISettingsChangeNotifier
import org.digma.intellij.plugin.ui.settings.SettingsChangeListener
import org.digma.intellij.plugin.ui.settings.Theme
import java.lang.ref.WeakReference
import java.util.Objects
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent


class JCefComponent
private constructor(
    private val project: Project,
    parentDisposable: Disposable,
    val name: String,
    val jbCefBrowser: JBCefBrowser
) : Disposable {

    private val logger: Logger = Logger.getInstance(JCefComponent::class.java)

    private val settingsChangeListener: SettingsChangeListener


    init {
        val connectionEventAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable)


        project.messageBus.connect(parentDisposable).subscribe(RunManagerListener.TOPIC, object : RunManagerListener {
            override fun runConfigurationSelected(settings: RunnerAndConfigurationSettings?) {
                val setRunConfigurationMessageBuilder =
                    SetRunConfigurationMessageBuilder(project, jbCefBrowser.cefBrowser, RunManager.getInstance(project).selectedConfiguration)
                setRunConfigurationMessageBuilder.sendRunConfigurationAttributes()
            }

            override fun runConfigurationChanged(settings: RunnerAndConfigurationSettings) {
                //always send the selected one not necessarily the one that changed
                val setRunConfigurationMessageBuilder =
                    SetRunConfigurationMessageBuilder(project, jbCefBrowser.cefBrowser, RunManager.getInstance(project).selectedConfiguration)
                setRunConfigurationMessageBuilder.sendRunConfigurationAttributes()
            }
        })



        project.messageBus.connect(parentDisposable)
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
                            val status = service<DockerService>().getActualRunningEngine(project)
                            updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)
                            sendBackendAboutInfo(jbCefBrowser.cefBrowser, project)
                        } catch (e: Throwable) {
                            Log.warnWithException(logger, project, e, "error in ApiClientChangedEvent")
                            ErrorReporter.getInstance().reportError(project, "JCefComponent.apiClientChanged", e)
                        }
                    }
                })


        ApplicationManager.getApplication().messageBus.connect(parentDisposable)
            .subscribe(
                UserFinishedDigmathonEvent.USER_FINISHED_DIGMATHON_TOPIC,
                UserFinishedDigmathonEvent {
                    sendUserFinishedDigmathon(jbCefBrowser.cefBrowser)
                })

        ApplicationManager.getApplication().messageBus.connect(parentDisposable)
            .subscribe(DigmathonProductKeyStateChangedEvent.PRODUCT_KEY_STATE_CHANGED_TOPIC,
                DigmathonProductKeyStateChangedEvent { productKey ->
                    sendDigmathonProductKey(productKey, jbCefBrowser.cefBrowser)
                })

        ApplicationManager.getApplication().messageBus.connect(parentDisposable)
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
                sendBackendAboutInfo(jbCefBrowser.cefBrowser, project)
            } catch (e: Throwable) {
                Log.warnWithException(logger, project, e, "error in AuthInfoChange")
                ErrorReporter.getInstance().reportError(project, "JCefComponent.AuthInfoChange", e)
            }
        }, parentDisposable)

        project.messageBus.connect(parentDisposable).subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, object : AnalyticsServiceConnectionEvent {

                override fun connectionLost() {
                    //delay before reporting digma status to let all containers go down
                    connectionEventAlarm.cancelAllRequests()
                    connectionEventAlarm.addRequest({
                        try {
                            val status = service<DockerService>().getCurrentDigmaInstallationStatusOnConnectionLost()
                            updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)
                        } catch (e: Exception) {
                            Log.warnWithException(logger, project, e, "error in connectionLost")
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
                            Log.warnWithException(logger, project, e, "error in connectionGained")
                            ErrorReporter.getInstance().reportError(project, "JCefComponent.connectionGained", e)
                        }
                    }, 2000)
                }
            })


        SettingsState.getInstance().addChangeListener({ settings ->
            //this event run on EDT
            Backgroundable.executeOnPooledThread {
                try {
                    //can not rely here on backend about info because this event may be processed before the api client was changed.
                    //the API_CLIENT_CHANGED_TOPIC is more suitable. it is handled here.
                    val apiUrl = settings.apiUrl
                    sendApiUrl(jbCefBrowser.cefBrowser, apiUrl)
                    sendIsMicrometerProject(jbCefBrowser.cefBrowser, SpringBootMicrometerConfigureDepsService.isSpringBootWithMicrometer())
                    sendIsJaegerButtonEnabledMessage(jbCefBrowser.cefBrowser)
                    sendBackendAboutInfo(jbCefBrowser.cefBrowser, project)
                    val status = service<DockerService>().getActualRunningEngine(project)
                    updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)
                } catch (e: Throwable) {
                    Log.warnWithException(logger, project, e, "error in SettingsState")
                    ErrorReporter.getInstance().reportError(project, "JCefComponent.SettingsState", e)
                }
            }
        }, parentDisposable)


        project.messageBus.connect(parentDisposable).subscribe(
            UserRegistrationEvent.USER_REGISTRATION_TOPIC, object : UserRegistrationEvent {
                override fun userRegistered(email: String) {
                    try {
                        sendUserEmail(jbCefBrowser.cefBrowser, email)
                    } catch (e: Throwable) {
                        Log.warnWithException(logger, project, e, "error in userRegistered")
                        ErrorReporter.getInstance().reportError(project, "JCefComponent.userRegistered", e)
                    }
                }
            })


        project.messageBus.connect(parentDisposable).subscribe(
            EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, object : EnvironmentChanged {
                override fun environmentChanged(newEnv: Env?) {
                    try {
                        newEnv?.let {
                            sendCurrentEnvironment(jbCefBrowser.cefBrowser, it)
                        }
                    } catch (e: Throwable) {
                        Log.warnWithException(logger, project, e, "error in environmentChanged")
                        ErrorReporter.getInstance().reportError(project, "JCefComponent.environmentChanged", e)
                    }
                }

                override fun environmentsListChanged(newEnvironments: MutableList<Env>?) {
                    try {
                        sendEnvironmentsList(
                            jbCefBrowser.cefBrowser,
                            getAllEnvironments(project)
                        )
                    } catch (e: Throwable) {
                        Log.warnWithException(logger, project, e, "error in environmentsListChanged")
                        ErrorReporter.getInstance().reportError(project, "JCefComponent.environmentsListChanged", e)
                    }
                }
            })


        project.messageBus.connect(parentDisposable).subscribe(
            ObservabilityChanged.OBSERVABILITY_CHANGED_TOPIC, ObservabilityChanged { isObservabilityEnabled ->
                try {
                    sendObservabilityEnabledMessage(
                        jbCefBrowser.cefBrowser,
                        isObservabilityEnabled
                    )
                } catch (e: Throwable) {
                    Log.warnWithException(logger, project, e, "error in observabilityChanged")
                    ErrorReporter.getInstance().reportError(project, "JCefComponent.observabilityChanged", e)
                }
            }
        )

        project.messageBus.connect(parentDisposable).subscribe(
            ScopeChangedEvent.SCOPE_CHANGED_TOPIC, object : ScopeChangedEvent {
                override fun scopeChanged(
                    scope: SpanScope?, codeLocation: CodeLocation, hasErrors: Boolean, scopeContext: ScopeContext?, environmentId: String?
                ) {
                    try {
                        val insightsStats = AnalyticsService.getInstance(project).getInsightsStats(scope?.spanCodeObjectId, null)

                        sendScopeChangedMessage(
                            jbCefBrowser.cefBrowser,
                            scope,
                            codeLocation,
                            hasErrors,
                            insightsStats.analyticsInsightsCount,
                            insightsStats.issuesInsightsCount,
                            insightsStats.unreadInsightsCount,
                            scopeContext,
                            environmentId
                        )
                    } catch (e: Throwable) {
                        Log.warnWithException(logger, project, e, "error in scopeChanged")
                        ErrorReporter.getInstance().reportError(project, "JCefComponent.scopeChanged", e)
                    }
                }
            }
        )

        project.messageBus.connect(parentDisposable).subscribe(
            StateChangedEvent.JCEF_STATE_CHANGED_TOPIC, object : StateChangedEvent {
                override fun stateChanged(state: JsonNode) {
                    try {
                        sendJcefStateMessage(jbCefBrowser.cefBrowser, state)
                    } catch (e: Throwable) {
                        Log.warnWithException(logger, project, e, "error in stateChanged")
                        ErrorReporter.getInstance().reportError(project, "JCefComponent.stateChanged", e)
                    }
                }
            }
        )

        project.messageBus.connect(parentDisposable).subscribe(
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

                        val spanEnvironments = if (scope?.at("/span/spanCodeObjectId")?.asText(null) != null) {
                            val version = BackendInfoHolder.getInstance(project).getAbout()?.applicationVersion

                            val currentBackendVersion = ComparableVersion(version)
                            val spanEnvironmentsVersion = ComparableVersion("0.3.94")
                            if (currentBackendVersion.newerThan(spanEnvironmentsVersion)) {
                                AnalyticsService.getInstance(project).getSpanEnvironmentsStats(scope.at("/span/spanCodeObjectId").asText())
                            } else {
                                listOf()
                            }
                        } else {
                            listOf()
                        }

                        sendSetInsightStatsMessage(
                            jbCefBrowser.cefBrowser,
                            scope,
                            analyticsInsightsCount,
                            issuesInsightsCount,
                            unreadInsightsCount,
                            criticalInsightsCount,
                            allIssuesCount,
                            spanEnvironments
                        )
                    } catch (e: Throwable) {
                        Log.warnWithException(logger, project, e, "error in insightStatsChanged")
                        ErrorReporter.getInstance().reportError(project, "JCefComponent.insightStatsChanged", e)
                    }
                }
            }
        )
    }

    override fun dispose() {
        try {
            jbCefBrowser.jbCefClient.dispose()
            jbCefBrowser.dispose()
            ApplicationUISettingsChangeNotifier.getInstance(project).removeSettingsChangeListener(settingsChangeListener)
        } catch (e: Exception) {
            ErrorReporter.getInstance().reportError(project, "JCefComponent.dispose", e)
        }
    }

    fun getComponent(): JComponent {
        return jbCefBrowser.component
    }


    //must provide a parentDisposable that is not the project.
    //instances of JCefComponentBuilder need to be local to a function, so they can be garbage collected
    class JCefComponentBuilder(
        private val project: Project,
        private val name: String,
        parentDisposable: Disposable,
        url: String,
        messageRouterHandler: BaseMessageRouterHandler
    ) {

        private val urlRef = WeakReference(Objects.requireNonNull(url, "url must not be null"))
        private val messageRouterHandlerRef = WeakReference(Objects.requireNonNull(messageRouterHandler, "messageRouterHandlers must not be null"))
        private val parentDisposableRef = WeakReference(parentDisposable)
        private var downloadAdapterRef: WeakReference<CefDownloadHandler>? = null

        //arguments per jcef instance, for example jaeger ui virtual file, or documentation virtual file
        private val arguments = WeakHashMap<String, Any>()


        fun build(): JCefComponent {

            val url: String = Objects.requireNonNull(urlRef.get(), "url must not be null")!!
            val messageRouterHandler = Objects.requireNonNull(messageRouterHandlerRef.get(), "messageRouterHandlers must not be null")!!
            val parentDisposable = Objects.requireNonNull(parentDisposableRef.get(), "parentDisposable must not be null")!!


            val jbCefBrowser = JBCefBrowserBuilderCreator.create()
                .setUrl(url)
                .build()

            //set properties that are used by resource handlers.
            setProject(jbCefBrowser, project)
            arguments.forEach {
                setProperty(jbCefBrowser, it.key, it.value)
            }


            val jbCefClient = jbCefBrowser.jbCefClient
            val cefMessageRouter = CefMessageRouter.create()
            cefMessageRouter.addHandler(messageRouterHandler, true)
            jbCefClient.cefClient.addMessageRouter(cefMessageRouter)

            jbCefClient.cefClient.addDisplayHandler(JCefDisplayHandler(name))

            val lifeSpanHandle = if (!LifeSpanHandle.registered.get()) {
                jbCefClient.addLifeSpanHandler(LifeSpanHandle, jbCefBrowser.cefBrowser)
                LifeSpanHandle
            } else {
                null
            }

            downloadAdapterRef?.get()?.let {
                jbCefClient.addDownloadHandler(it, jbCefBrowser.cefBrowser)
            }

            val jCefComponent =
                JCefComponent(project, parentDisposable, name, jbCefBrowser)

            //register disposable for the above components
            Disposer.register(parentDisposable) {
                cefMessageRouter.removeHandler(messageRouterHandler)
                cefMessageRouter.dispose()
                jbCefClient.cefClient.removeMessageRouter(cefMessageRouter)
                lifeSpanHandle?.let {
                    jbCefClient.removeLifeSpanHandler(it, jbCefBrowser.cefBrowser)
                }
                downloadAdapterRef?.get()?.let {
                    jbCefClient.removeDownloadHandle(it, jbCefBrowser.cefBrowser)
                }
            }

            return jCefComponent
        }


        fun withDownloadAdapter(adapter: CefDownloadHandler): JCefComponentBuilder {
            this.downloadAdapterRef = WeakReference(Objects.requireNonNull(adapter, "downloadAdapter must not be null"))
            return this
        }

        fun withArg(key: String, value: Any): JCefComponentBuilder {
            arguments[key] = value
            return this
        }
    }
}


object LifeSpanHandle : CefLifeSpanHandlerAdapter() {

    val registered = AtomicBoolean(false)

    private val registrationLock = ReentrantLock(true)

    override fun onAfterCreated(browser: CefBrowser?) {

        //register only one CefSchemeHandlerFactory for every type.
        //CefSchemeHandlerFactory can be registered only on browser thread, onAfterCreated after the first jcef browser
        // is created and at this stage it is possible to register CefSchemeHandlerFactory.
        //it may be that this LifeSpanHandle will be registered to more than one client, but it will still register
        // only one CefSchemeHandlerFactory for type. it is removed from the client in dispose.

        try {
            registrationLock.lock()
            if (registered.get()) {
                return
            }

            allSchemaHandlerFactories().forEach {
                CefApp.getInstance().registerSchemeHandlerFactory(
                    it.getSchema(), it.getDomain(), it
                )
            }

            val mailtoSchemaHandlerFactory = MailtoSchemaHandlerFactory()
            CefApp.getInstance().registerSchemeHandlerFactory(
                mailtoSchemaHandlerFactory.getSchema(), mailtoSchemaHandlerFactory.getDomain(), mailtoSchemaHandlerFactory
            )

            registered.set(true)

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("LifeSpanHandle.onAfterCreated", e)
        } finally {
            if (registrationLock.isHeldByCurrentThread) {
                registrationLock.unlock()
            }
        }
    }
}
