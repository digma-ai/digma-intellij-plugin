package org.digma.intellij.plugin.ui.wizard

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.analytics.refreshEnvironmentsNowOnBackground
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.UniqueGeneratedUserId
import org.digma.intellij.plugin.common.createObjectMapper
import org.digma.intellij.plugin.digmathon.DigmathonActivationEvent
import org.digma.intellij.plugin.digmathon.DigmathonProductKeyStateChangedEvent
import org.digma.intellij.plugin.digmathon.DigmathonService
import org.digma.intellij.plugin.digmathon.UserFinishedDigmathonEvent
import org.digma.intellij.plugin.docker.DigmaInstallationStatus
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.docker.LocalInstallationFacade
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.jcef.common.CustomSchemeHandlerFactory
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.recentactivity.RecentActivityToolWindowShower
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.common.isJaegerButtonEnabled
import org.digma.intellij.plugin.ui.common.updateObservabilityValue
import org.digma.intellij.plugin.ui.jcef.DIGMATHON_ENABLED
import org.digma.intellij.plugin.ui.jcef.DIGMATHON_PRODUCT_KEY
import org.digma.intellij.plugin.ui.jcef.IS_LOGGING_ENABLED
import org.digma.intellij.plugin.ui.jcef.JBCefBrowserBuilderCreator
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import org.digma.intellij.plugin.ui.jcef.USER_FINISHED_DIGMATHON
import org.digma.intellij.plugin.ui.jcef.USER_ID
import org.digma.intellij.plugin.ui.jcef.getIsLoggingEnabledSystemProperty
import org.digma.intellij.plugin.ui.jcef.jsonToObject
import org.digma.intellij.plugin.ui.jcef.model.OpenInDefaultBrowserRequest
import org.digma.intellij.plugin.ui.jcef.model.SendTrackingEventRequest
import org.digma.intellij.plugin.ui.jcef.sendDigmathonProductKey
import org.digma.intellij.plugin.ui.jcef.sendDigmathonState
import org.digma.intellij.plugin.ui.jcef.sendRequestToChangeCodeFont
import org.digma.intellij.plugin.ui.jcef.sendRequestToChangeFont
import org.digma.intellij.plugin.ui.jcef.sendRequestToChangeUiTheme
import org.digma.intellij.plugin.ui.jcef.sendUserFinishedDigmathon
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.jcef.updateDigmaEngineStatus
import org.digma.intellij.plugin.ui.notificationcenter.AppNotificationCenter
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import org.digma.intellij.plugin.ui.settings.ApplicationUISettingsChangeNotifier
import org.digma.intellij.plugin.ui.settings.SettingsChangeListener
import org.digma.intellij.plugin.ui.settings.Theme
import org.digma.intellij.plugin.ui.wizard.model.ConnectionCheckMessagePayload
import org.digma.intellij.plugin.ui.wizard.model.ConnectionCheckMessageRequest
import org.digma.intellij.plugin.ui.wizard.model.ConnectionCheckResult
import org.digma.intellij.plugin.ui.wizard.model.FinishRequest
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDigmaEngineInstalledPayload
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDigmaEngineInstalledRequest
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDigmaEngineRunningPayload
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDigmaEngineRunningRequest
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDockerComposeInstalledPayload
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDockerComposeInstalledRequest
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDockerInstalledPayload
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDockerInstalledRequest
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerResultPayload
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerResultRequest
import org.digma.intellij.plugin.ui.wizard.model.SetObservabilityRequest
import java.awt.BorderLayout
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel
import kotlin.time.Duration.Companion.seconds

private const val DOMAIN_NAME = "installationwizard"
private const val RESOURCE_FOLDER_NAME = "installation-wizard"
private const val TEMPLATE_FOLDER_NAME = "installationwizard"
private const val ENV_VARIABLE_IDE: String = "ide"
private const val USER_EMAIL_VARIABLE: String = "userEmail"
private const val IS_OBSERVABILITY_ENABLED_VARIABLE: String = "isObservabilityEnabled"
private const val IS_DOCKER_INSTALLED: String = "isDockerInstalled"
private const val IS_DOCKER_COMPOSE_INSTALLED: String = "isDockerComposeInstalled"
private const val IS_DIGMA_ENGINE_INSTALLED: String = "isDigmaEngineInstalled"
private const val IS_DIGMA_ENGINE_RUNNING: String = "isDigmaEngineRunning"
private const val IS_WIZARD_FIRST_LAUNCH: String = "wizardFirstLaunch"
private const val IS_JAEGER_ENABLED: String = "isJaegerEnabled"
private const val IS_WIZARD_SKIP_INSTALLATION_STEP: String = "wizardSkipInstallationStep"

private val logger: Logger =
    Logger.getInstance("org.digma.intellij.plugin.ui.common.InstallationWizard")


fun createInstallationWizardSidePanelWindowPanel(project: Project, wizardSkipInstallationStep: Boolean): DisposablePanel? {

    val objectMapper = createObjectMapper()

    val digmaStatusUpdater = DigmaStatusUpdater()


    val localEngineOperationRunning = AtomicBoolean(false)

    if (!JBCefApp.isSupported()) {
        // Fallback to an alternative browser-less solution
        return null
    }

    val jbCefBrowser = JBCefBrowserBuilderCreator.create()
        .setUrl("https://$DOMAIN_NAME/$RESOURCE_FOLDER_NAME/index.html")
        .build()
    val indexTemplateData = mutableMapOf<String, Any>(
        ENV_VARIABLE_IDE to ApplicationNamesInfo.getInstance().productName, //Available values: "IDEA", "Rider", "PyCharm"
        USER_EMAIL_VARIABLE to (PersistenceService.getInstance().getUserEmail() ?: ""),
        IS_OBSERVABILITY_ENABLED_VARIABLE to PersistenceService.getInstance().isObservabilityEnabled(),
        IS_DOCKER_INSTALLED to service<DockerService>().isDockerInstalled(),
        IS_DOCKER_COMPOSE_INSTALLED to service<DockerService>().isDockerComposeInstalled(),
        IS_DIGMA_ENGINE_INSTALLED to service<LocalInstallationFacade>().isLocalEngineInstalled(),
        IS_DIGMA_ENGINE_RUNNING to service<LocalInstallationFacade>().isLocalEngineRunning(project),
        IS_WIZARD_FIRST_LAUNCH to PersistenceService.getInstance().isFirstWizardLaunch(),
        IS_JAEGER_ENABLED to isJaegerButtonEnabled(),
        IS_WIZARD_SKIP_INSTALLATION_STEP to wizardSkipInstallationStep,
        DIGMATHON_ENABLED to DigmathonService.getInstance().getDigmathonState().isActive(),
        DIGMATHON_PRODUCT_KEY to DigmathonService.getInstance().getProductKey().orEmpty(),
        USER_ID to UniqueGeneratedUserId.userId,
        USER_FINISHED_DIGMATHON to DigmathonService.getInstance().isUserFinishedDigmathon,
        IS_LOGGING_ENABLED to getIsLoggingEnabledSystemProperty()

    )


    val lifeSpanHandler: CefLifeSpanHandlerAdapter = object : CefLifeSpanHandlerAdapter() {
        override fun onAfterCreated(browser: CefBrowser) {
            CefApp.getInstance()
                .registerSchemeHandlerFactory(
                    "https",
                    DOMAIN_NAME,
                    CustomSchemeHandlerFactory(DOMAIN_NAME, TEMPLATE_FOLDER_NAME,RESOURCE_FOLDER_NAME, indexTemplateData)
                )
        }
    }

    jbCefBrowser.jbCefClient.addLifeSpanHandler(lifeSpanHandler, jbCefBrowser.cefBrowser)

    Disposer.register(jbCefBrowser) {
        jbCefBrowser.jbCefClient.removeLifeSpanHandler(lifeSpanHandler, jbCefBrowser.cefBrowser)
    }

    val jbCefClient = jbCefBrowser.jbCefClient

    val msgRouter = CefMessageRouter.create()

    InstallationWizardService.getInstance(project).setJcefBrowser(jbCefBrowser)


    msgRouter.addHandler(object : CefMessageRouterHandlerAdapter() {

        override fun onQuery(
            browser: CefBrowser,
            frame: CefFrame,
            queryId: Long,
            request: String,
            persistent: Boolean,
            callback: CefQueryCallback,
        ): Boolean {
            Log.log(logger::debug, "request: {}", request)

            val requestJsonNode = objectMapper.readTree(request)
            val action: String = requestJsonNode["action"].asText()

            if ("INSTALLATION_WIZARD/INITIALIZE".equals(action, ignoreCase = true)) {
                digmaStatusUpdater.start(project, jbCefBrowser)
            }
            if ("INSTALLATION_WIZARD/CLOSE".equals(action, ignoreCase = true)) {
                EDT.ensureEDT {
                    digmaStatusUpdater.stop()
                    MainToolWindowCardsController.getInstance(project).wizardFinished()
                }
            }
            if (JCEFGlobalConstants.GLOBAL_SEND_TRACKING_EVENT.equals(action, ignoreCase = true)) {
                val (_, payload) = jsonToObject(request, SendTrackingEventRequest::class.java)
                if (payload != null) {
                    if (payload.data == null) {
                        ActivityMonitor.getInstance(project).registerCustomEvent(payload.eventName)
                    } else {
                        ActivityMonitor.getInstance(project).registerCustomEvent(payload.eventName, payload.data)
                    }
                }
            }
            if (JCEFGlobalConstants.INSTALLATION_WIZARD_SET_OBSERVABILITY.equals(action, ignoreCase = true)) {
                val (_, payload) = jsonToObject(request, SetObservabilityRequest::class.java)
                if (payload != null) {
                    updateObservabilityValue(project, payload.isObservabilityEnabled)
                }
            }
            if (JCEFGlobalConstants.INSTALLATION_WIZARD_FINISH.equals(action, ignoreCase = true)) {
                digmaStatusUpdater.stop()
                val (_, payload) = jsonToObject(request, FinishRequest::class.java)
                val email = payload?.email
                PersistenceService.getInstance().setUserEmail(email)
                if (email != null) {
                    ActivityMonitor.getInstance(project).registerEmail(email)
                }

                payload?.productKey?.let {
                    DigmathonService.getInstance().setProductKey(it)
                }

                PersistenceService.getInstance().firstWizardLaunchDone()
                ActivityMonitor.getInstance(project).registerCustomEvent("installation-wizard-finished")

                EDT.ensureEDT {
                    ToolWindowShower.getInstance(project).showToolWindow()
                    MainToolWindowCardsController.getInstance(project).wizardFinished()
                    project.service<RecentActivityToolWindowShower>().showToolWindow()
                }
            }
            if (JCEFGlobalConstants.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER.equals(action, ignoreCase = true)) {
                val (_, payload) = jsonToObject(request, OpenInDefaultBrowserRequest::class.java)
                if (payload != null) {
                    ApplicationManager.getApplication().invokeLater {
                        BrowserUtil.browse(
                            payload.url
                        )
                    }
                }
            }
            if (JCEFGlobalConstants.INSTALLATION_WIZARD_CHECK_CONNECTION.equals(action, ignoreCase = true)) {
                val connectionCheckMessagePayload: ConnectionCheckMessagePayload =
                    if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
                        ConnectionCheckMessagePayload(ConnectionCheckResult.SUCCESS.value)
                    } else {
                        refreshEnvironmentsNowOnBackground(project)
                        ConnectionCheckMessagePayload(ConnectionCheckResult.FAILURE.value)
                    }
                val message = ConnectionCheckMessageRequest(connectionCheckMessagePayload)
                serializeAndExecuteWindowPostMessageJavaScript(jbCefBrowser.cefBrowser, message)
            }
            if (JCEFGlobalConstants.INSTALLATION_WIZARD_INSTALL_DIGMA_ENGINE.equals(action, ignoreCase = true)) {

                localEngineOperationRunning.set(true)

                service<LocalInstallationFacade>().installEngine(project) { exitValue ->

                    EDT.assertNonDispatchThread()

                    if (exitValue == DockerService.NO_DOCKER_COMPOSE_COMMAND) {
                        sendIsDockerInstalled(false, jbCefBrowser)
                        sendIsDockerComposeInstalled(false, jbCefBrowser)
                    } else {
                        sendIsDockerInstalled(true, jbCefBrowser)
                        sendIsDockerComposeInstalled(true, jbCefBrowser)
                    }


                    val success = exitValue == "0"

                    if (success) {
                        //wait up to two minutes for connection, sometimes it takes more than a minute before connection is available
                        var i = 0
                        while (!BackendConnectionMonitor.getInstance(project).isConnectionOk() && i < 24) {
                            Log.log(logger::warn, "waiting for connection")
                            refreshEnvironmentsNowOnBackground(project)
                            try {
                                Thread.sleep(5000)
                            } catch (e: InterruptedException) {
                                //ignore
                            }
                            i++
                        }
                    }


                    val connectionOk = BackendConnectionMonitor.getInstance(project).isConnectionOk()
                    if (!connectionOk) {
                        Log.log(logger::warn, "no connection after engine installation")
                        if (success) {
                            val log = DockerService.getInstance().collectDigmaContainerLog()
                            ActivityMonitor.getInstance(project)
                                .registerDigmaEngineEventError(
                                    "installEngine", "No connection 2 minutes after successful engine install",
                                    mapOf(
                                        "docker log" to log
                                    )
                                )
                        }
                    }
                    val isEngineUp = connectionOk && success
                    if (isEngineUp) {
                        PersistenceService.getInstance().setLocalEngineInstalled(true)
                        sendDockerResult(
                            ConnectionCheckResult.SUCCESS.value,
                            "",
                            jbCefBrowser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_INSTALL_DIGMA_ENGINE_RESULT
                        )
                        sendIsDigmaEngineInstalled(true, jbCefBrowser)
                        sendIsDigmaEngineRunning(true, jbCefBrowser)

                        service<AppNotificationCenter>().showInstallationFinishedNotification(project)
                    } else {
                        Log.log(logger::warn, "error installing engine, {}", exitValue)

                        sendDockerResult(
                            ConnectionCheckResult.FAILURE.value,
                            "Could not install engine",
                            jbCefBrowser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_INSTALL_DIGMA_ENGINE_RESULT
                        )
                        sendIsDigmaEngineInstalled(false, jbCefBrowser)
                        sendIsDigmaEngineRunning(false, jbCefBrowser)

                        //if install failed remove the engine. wait a second to let the installEngine finish and reports
                        // the installEngine.end to posthog before removeEngine.start
                        //also the LocalInstallationFacade needs to completely finish the install operation before it will allow
                        // another operation
                        Backgroundable.executeOnPooledThread {
                            try {
                                Thread.sleep(2000)
                            } catch (e: Exception) {
                                //ignore
                            }
                            Log.log(logger::warn, "removing engine after installation failed")
                            service<LocalInstallationFacade>().removeEngine(project) { exitValue ->
                                updateDigmaEngineStatus(project, jbCefBrowser.cefBrowser)
                                if (exitValue != "0") {
                                    Log.log(logger::warn, "error removing engine after failure {}", exitValue)
                                }
                            }
                        }

                    }

                    updateDigmaEngineStatus(project, jbCefBrowser.cefBrowser)
                    localEngineOperationRunning.set(false)

                }
            }
            if (JCEFGlobalConstants.INSTALLATION_WIZARD_UNINSTALL_DIGMA_ENGINE.equals(action, ignoreCase = true)) {
                localEngineOperationRunning.set(true)
                service<LocalInstallationFacade>().removeEngine(project) { exitValue ->

                    EDT.assertNonDispatchThread()

                    if (exitValue == DockerService.NO_DOCKER_COMPOSE_COMMAND) {
                        sendIsDockerInstalled(false, jbCefBrowser)
                        sendIsDockerComposeInstalled(false, jbCefBrowser)
                    } else {
                        sendIsDockerInstalled(true, jbCefBrowser)
                        sendIsDockerComposeInstalled(true, jbCefBrowser)
                    }

                    val success = exitValue == "0"
                    if (success) {
                        sendDockerResult(
                            ConnectionCheckResult.SUCCESS.value,
                            "",
                            jbCefBrowser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_UNINSTALL_DIGMA_ENGINE_RESULT
                        )
                        sendIsDigmaEngineRunning(false, jbCefBrowser)
                        sendIsDigmaEngineInstalled(false, jbCefBrowser)

                    } else {
                        Log.log(logger::warn, "error uninstalling engine {}", exitValue)
                        sendDockerResult(
                            ConnectionCheckResult.FAILURE.value,
                            "Could not uninstall engine",
                            jbCefBrowser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_UNINSTALL_DIGMA_ENGINE_RESULT
                        )
                    }

                    updateDigmaEngineStatus(project, jbCefBrowser.cefBrowser)
                    localEngineOperationRunning.set(false)
                }
            }
            if (JCEFGlobalConstants.INSTALLATION_WIZARD_START_DIGMA_ENGINE.equals(action, ignoreCase = true)) {
                localEngineOperationRunning.set(true)
                service<LocalInstallationFacade>().startEngine(project) { exitValue ->

                    EDT.assertNonDispatchThread()

                    if (exitValue == DockerService.NO_DOCKER_COMPOSE_COMMAND) {
                        sendIsDockerInstalled(false, jbCefBrowser)
                        sendIsDockerComposeInstalled(false, jbCefBrowser)
                    } else {
                        sendIsDockerInstalled(true, jbCefBrowser)
                        sendIsDockerComposeInstalled(true, jbCefBrowser)
                    }


                    val success = exitValue == "0"

                    if (success) {
                        //wait up to two minutes for connection, sometimes it takes more than a minute before connection is available
                        var i = 0
                        while (!BackendConnectionMonitor.getInstance(project).isConnectionOk() && i < 24) {
                            Log.log(logger::warn, "waiting for connection")
                            refreshEnvironmentsNowOnBackground(project)
                            try {
                                Thread.sleep(5000)
                            } catch (e: InterruptedException) {
                                //ignore
                            }
                            i++
                        }
                    }

                    val connectionOk = BackendConnectionMonitor.getInstance(project).isConnectionOk()
                    if (!connectionOk) {
                        Log.log(logger::warn, "no connection after engine start")
                        if (success) {
                            val log = DockerService.getInstance().collectDigmaContainerLog()
                            ActivityMonitor.getInstance(project)
                                .registerDigmaEngineEventError(
                                    "startEngine", "No connection 2 minutes after successful engine start",
                                    mapOf(
                                        "docker log" to log
                                    )
                                )
                        }
                    }

                    val isEngineUp = connectionOk && success
                    if (isEngineUp) {
                        sendDockerResult(
                            ConnectionCheckResult.SUCCESS.value,
                            "",
                            jbCefBrowser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_START_DIGMA_ENGINE_RESULT
                        )
                        sendIsDigmaEngineInstalled(true, jbCefBrowser)
                        sendIsDigmaEngineRunning(true, jbCefBrowser)
                    } else {
                        Log.log(logger::warn, "error starting engine {}", exitValue)

                        sendDockerResult(
                            ConnectionCheckResult.FAILURE.value,
                            "Could not start engine",
                            jbCefBrowser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_START_DIGMA_ENGINE_RESULT
                        )
                        sendIsDigmaEngineInstalled(true, jbCefBrowser)
                        sendIsDigmaEngineRunning(false, jbCefBrowser)
                    }


                    updateDigmaEngineStatus(project, jbCefBrowser.cefBrowser)
                    localEngineOperationRunning.set(false)

                }
            }
            if (JCEFGlobalConstants.INSTALLATION_WIZARD_STOP_DIGMA_ENGINE.equals(action, ignoreCase = true)) {
                localEngineOperationRunning.set(true)
                service<LocalInstallationFacade>().stopEngine(project) { exitValue ->

                    EDT.assertNonDispatchThread()

                    if (exitValue == DockerService.NO_DOCKER_COMPOSE_COMMAND) {
                        sendIsDockerInstalled(false, jbCefBrowser)
                        sendIsDockerComposeInstalled(false, jbCefBrowser)
                    } else {
                        sendIsDockerInstalled(true, jbCefBrowser)
                        sendIsDockerComposeInstalled(true, jbCefBrowser)
                    }

                    val success = exitValue == "0"
                    if (success) {
                        sendDockerResult(
                            ConnectionCheckResult.SUCCESS.value,
                            "",
                            jbCefBrowser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_STOP_DIGMA_ENGINE_RESULT
                        )
                        sendIsDigmaEngineRunning(false, jbCefBrowser)
                    } else {
                        Log.log(logger::warn, "error stopping engine {}", exitValue)
                        sendDockerResult(
                            ConnectionCheckResult.FAILURE.value,
                            "Could not stop engine",
                            jbCefBrowser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_STOP_DIGMA_ENGINE_RESULT
                        )
                    }

                    updateDigmaEngineStatus(project, jbCefBrowser.cefBrowser)
                    localEngineOperationRunning.set(false)
                }
            }
            callback.success("")
            return true
        }

    }, true)

    jbCefClient.cefClient.addMessageRouter(msgRouter)

    val browserPanel = JPanel()
    browserPanel.layout = BorderLayout()
    browserPanel.add(jbCefBrowser.component, BorderLayout.CENTER)


    val jcefDigmaPanel = object : DisposablePanel() {
        override fun dispose() {
            digmaStatusUpdater.stop()
            jbCefBrowser.dispose()
            jbCefClient.dispose()
            msgRouter.dispose()
        }
    }
    jcefDigmaPanel.layout = BorderLayout()
    jcefDigmaPanel.add(browserPanel, BorderLayout.CENTER)


    ApplicationUISettingsChangeNotifier.getInstance(project).addSettingsChangeListener(object : SettingsChangeListener {
        override fun systemFontChange(fontName: String) {
            sendRequestToChangeFont(fontName, jbCefBrowser)
        }

        override fun systemThemeChange(theme: Theme) {
            sendRequestToChangeUiTheme(theme, jbCefBrowser)
        }

        override fun editorFontChange(fontName: String) {
            sendRequestToChangeCodeFont(fontName, jbCefBrowser)
        }
    }, jbCefBrowser)

    project.messageBus.connect(jbCefBrowser)
        .subscribe(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, object : AnalyticsServiceConnectionEvent {
            override fun connectionLost() {
                if (localEngineOperationRunning.get()) {
                    return
                }

                try {
                    val status = service<LocalInstallationFacade>().getCurrentDigmaInstallationStatusOnConnectionLost()
                    updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)
                } catch (e: Exception) {
                    ErrorReporter.getInstance().reportError(project, "createInstallationWizardSidePanelWindowPanel.connectionLost", e)
                }
            }

            override fun connectionGained() {
                if (localEngineOperationRunning.get()) {
                    return
                }
                try {
                    val status = service<LocalInstallationFacade>().getCurrentDigmaInstallationStatusOnConnectionGained()
                    updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)
                } catch (e: Exception) {
                    ErrorReporter.getInstance().reportError(project, "createInstallationWizardSidePanelWindowPanel.connectionGained", e)
                }
            }
        })


    ApplicationManager.getApplication().messageBus.connect(jbCefBrowser)
        .subscribe(
            UserFinishedDigmathonEvent.USER_FINISHED_DIGMATHON_TOPIC,
            UserFinishedDigmathonEvent {
                sendUserFinishedDigmathon(jbCefBrowser.cefBrowser)
            })


    ApplicationManager.getApplication().messageBus.connect(jbCefBrowser)
        .subscribe(
            DigmathonProductKeyStateChangedEvent.PRODUCT_KEY_STATE_CHANGED_TOPIC,
            DigmathonProductKeyStateChangedEvent { productKey ->
                sendDigmathonProductKey(productKey, jbCefBrowser.cefBrowser)
            })


    //dispose in InstallationWizardService dispose
    ApplicationManager.getApplication().messageBus.connect(jbCefBrowser)
        .subscribe(
            DigmathonActivationEvent.DIGMATHON_ACTIVATION_TOPIC,
            DigmathonActivationEvent { isActive ->
                sendDigmathonState(isActive, jbCefBrowser.cefBrowser)
            })




    return jcefDigmaPanel
}


private fun sendDockerResult(result: String, errorMsg: String, jbCefBrowser: JBCefBrowser, messageType: String) {

    val payload = JcefDockerResultPayload(result, errorMsg)

    val message = JcefDockerResultRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        messageType,
        payload
    )
    serializeAndExecuteWindowPostMessageJavaScript(jbCefBrowser.cefBrowser, message)
}


fun sendIsDigmaEngineInstalled(result: Boolean, jbCefBrowser: JBCefBrowser) {
    val payload = JcefDockerIsDigmaEngineInstalledPayload(result)
    val message = JcefDockerIsDigmaEngineInstalledRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        JCEFGlobalConstants.GLOBAL_SET_IS_DIGMA_ENGINE_INSTALLED,
        payload
    )
    serializeAndExecuteWindowPostMessageJavaScript(jbCefBrowser.cefBrowser, message)
}


fun sendIsDigmaEngineRunning(success: Boolean, jbCefBrowser: JBCefBrowser) {

    val payload = JcefDockerIsDigmaEngineRunningPayload(success)
    val message = JcefDockerIsDigmaEngineRunningRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        JCEFGlobalConstants.GLOBAL_SET_IS_DIGMA_ENGINE_RUNNING,
        payload
    )
    serializeAndExecuteWindowPostMessageJavaScript(jbCefBrowser.cefBrowser, message)
}


fun sendIsDockerInstalled(result: Boolean, jbCefBrowser: JBCefBrowser) {
    val isDockerInstalledPayload = JcefDockerIsDockerInstalledPayload(result)
    val message = JcefDockerIsDockerInstalledRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        JCEFGlobalConstants.GLOBAL_SET_IS_DOCKER_INSTALLED,
        isDockerInstalledPayload
    )
    serializeAndExecuteWindowPostMessageJavaScript(jbCefBrowser.cefBrowser, message)

}

fun sendIsDockerComposeInstalled(result: Boolean, jbCefBrowser: JBCefBrowser) {
    val isDockerComposeInstalledPayload = JcefDockerIsDockerComposeInstalledPayload(result)
    val message = JcefDockerIsDockerComposeInstalledRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        JCEFGlobalConstants.GLOBAL_SET_IS_DOCKER_COMPOSE_INSTALLED,
        isDockerComposeInstalledPayload
    )
    serializeAndExecuteWindowPostMessageJavaScript(jbCefBrowser.cefBrowser, message)
}


class DigmaStatusUpdater {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private var myDisposable: Disposable? = null

    private var digmaInstallationStatus = AtomicReference<DigmaInstallationStatus?>(null)

    fun start(project: Project, jbCefBrowser: JBCefBrowser) {

        Log.log(logger::trace, project, "starting DigmaStatusUpdater")

        myDisposable = Disposer.newDisposable()
        digmaInstallationStatus.set(null)

        myDisposable?.let {
            it.disposingPeriodicTask("InstallationWizard.DigmaStatusUpdater", 2.seconds.inWholeMilliseconds, false) {
                try {
                    val currentStatus = service<LocalInstallationFacade>().getDigmaInstallationStatus(project)

                    //DigmaInstallationStatus is data class so we can rely on equals
                    if (digmaInstallationStatus.get() == null || currentStatus != digmaInstallationStatus.get()) {
                        Log.log(logger::trace, project, "status changed current:{}, previous:{}", currentStatus, digmaInstallationStatus)
                        digmaInstallationStatus.set(currentStatus)
                        Log.log(logger::trace, project, "updating wizard with digmaInstallationStatus {}", digmaInstallationStatus)
                        digmaInstallationStatus.get()?.let { status ->
                            updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)
                        }
                    }

                    sendIsDigmaEngineInstalled(LocalInstallationFacade.getInstance().isLocalEngineInstalled(), jbCefBrowser)
                    sendIsDigmaEngineRunning(LocalInstallationFacade.getInstance().isLocalEngineRunning(project), jbCefBrowser)


                } catch (e: Exception) {
                    Log.warnWithException(logger, project, e, "error in DigmaStatusUpdater {}", e)
                    ErrorReporter.getInstance().reportError(project, "DigmaStatusUpdater.loop", e)
                }
            }
        }
    }

    fun stop() {
        myDisposable?.let {
            Disposer.dispose(it)
        }
    }


}