package org.digma.intellij.plugin.ui.common

import com.intellij.collaboration.async.disposingScope
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator
import org.digma.intellij.plugin.docker.DigmaInstallationStatus
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.jcef.common.ConnectionCheckResult
import org.digma.intellij.plugin.jcef.common.CustomSchemeHandlerFactory
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.jcef.common.JcefConnectionCheckMessagePayload
import org.digma.intellij.plugin.jcef.common.JcefConnectionCheckMessageRequest
import org.digma.intellij.plugin.jcef.common.JcefDockerIsDigmaEngineInstalledPayload
import org.digma.intellij.plugin.jcef.common.JcefDockerIsDigmaEngineInstalledRequest
import org.digma.intellij.plugin.jcef.common.JcefDockerIsDigmaEngineRunningPayload
import org.digma.intellij.plugin.jcef.common.JcefDockerIsDigmaEngineRunningRequest
import org.digma.intellij.plugin.jcef.common.JcefDockerIsDockerComposeInstalledPayload
import org.digma.intellij.plugin.jcef.common.JcefDockerIsDockerComposeInstalledRequest
import org.digma.intellij.plugin.jcef.common.JcefDockerIsDockerInstalledPayload
import org.digma.intellij.plugin.jcef.common.JcefDockerIsDockerInstalledRequest
import org.digma.intellij.plugin.jcef.common.JcefDockerResultPayload
import org.digma.intellij.plugin.jcef.common.JcefDockerResultRequest
import org.digma.intellij.plugin.jcef.common.JcefMessageRequest
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.jcef.common.SendTrackingEventRequest
import org.digma.intellij.plugin.model.rest.jcef.installationwizard.FinishRequest
import org.digma.intellij.plugin.model.rest.jcef.installationwizard.SetObservabilityRequest
import org.digma.intellij.plugin.notifications.AppNotificationCenter
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.persistence.updateInstallationWizardFlag
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.common.ObservabilityUtil.Companion.updateObservabilityValue
import org.digma.intellij.plugin.ui.jcef.model.OpenInDefaultBrowserRequest
import org.digma.intellij.plugin.ui.jcef.updateDigmaEngineStatus
import org.digma.intellij.plugin.ui.list.insights.isJaegerButtonEnabled
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityToolWindowShower
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityUpdater
import org.digma.intellij.plugin.ui.settings.ApplicationUISettingsChangeNotifier
import org.digma.intellij.plugin.ui.settings.SettingsChangeListener
import org.digma.intellij.plugin.ui.settings.Theme
import org.digma.intellij.plugin.wizard.InstallationWizardService
import java.awt.BorderLayout
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel

private const val RESOURCE_FOLDER_NAME = "installationwizard"
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

    val digmaStatusUpdater = DigmaStatusUpdater()


    val localEngineOperationRunning = AtomicBoolean(false)

    if (!JBCefApp.isSupported()) {
        // Fallback to an alternative browser-less solution
        return null
    }

    val jbCefBrowser = JBCefBrowserBuilderCreator.create()
        .setUrl("https://$RESOURCE_FOLDER_NAME/index.html")
        .build()
    val indexTemplateData = mutableMapOf<String, Any>(
        ENV_VARIABLE_IDE to ApplicationNamesInfo.getInstance().productName, //Available values: "IDEA", "Rider", "PyCharm"
        USER_EMAIL_VARIABLE to (PersistenceService.getInstance().getUserEmail() ?: ""),
        IS_OBSERVABILITY_ENABLED_VARIABLE to PersistenceService.getInstance().isAutoOtel(),
        IS_DOCKER_INSTALLED to service<DockerService>().isDockerInstalled(),
        IS_DOCKER_COMPOSE_INSTALLED to service<DockerService>().isDockerInstalled(),
        IS_DIGMA_ENGINE_INSTALLED to service<DockerService>().isEngineInstalled(),
        IS_DIGMA_ENGINE_RUNNING to service<DockerService>().isEngineRunning(project),
        IS_WIZARD_FIRST_LAUNCH to PersistenceService.getInstance().isFirstWizardLaunch(),
        IS_JAEGER_ENABLED to isJaegerButtonEnabled(),
        IS_WIZARD_SKIP_INSTALLATION_STEP to wizardSkipInstallationStep,
    )

    PersistenceService.getInstance().firstWizardLaunchDone()


    val lifeSpanHandler: CefLifeSpanHandlerAdapter = object : CefLifeSpanHandlerAdapter() {
        override fun onAfterCreated(browser: CefBrowser) {
            CefApp.getInstance()
                .registerSchemeHandlerFactory(
                    "https",
                    RESOURCE_FOLDER_NAME,
                    CustomSchemeHandlerFactory(RESOURCE_FOLDER_NAME, indexTemplateData)
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

            val (_, action) = JCefMessagesUtils.parseJsonToObject(
                request,
                JcefMessageRequest::class.java
            )
            if ("INSTALLATION_WIZARD/INITIALIZE".equals(action, ignoreCase = true)) {
                digmaStatusUpdater.start(project, jbCefBrowser.cefBrowser)
            }
            if ("INSTALLATION_WIZARD/CLOSE".equals(action, ignoreCase = true)) {
                EDT.ensureEDT {
                    digmaStatusUpdater.stop(project)
                    MainToolWindowCardsController.getInstance(project).wizardFinished()
                }
            }
            if (JCefMessagesUtils.GLOBAL_SEND_TRACKING_EVENT.equals(action, ignoreCase = true)) {
                val (_, payload) = JCefMessagesUtils.parseJsonToObject(
                    request,
                    SendTrackingEventRequest::class.java
                )
                if (payload != null) {
                    ActivityMonitor.getInstance(project).registerCustomEvent(payload.eventName, payload.data)
                }
            }
            if (JCefMessagesUtils.INSTALLATION_WIZARD_SET_OBSERVABILITY.equals(action, ignoreCase = true)) {
                val (_, payload) = JCefMessagesUtils.parseJsonToObject(
                    request,
                    SetObservabilityRequest::class.java
                )
                if (payload != null) {
                    updateObservabilityValue(project, payload.isObservabilityEnabled)
                    project.service<RecentActivityUpdater>().updateSetObservability(payload.isObservabilityEnabled)
                }
            }
            if (JCefMessagesUtils.INSTALLATION_WIZARD_FINISH.equals(action, ignoreCase = true)) {
                digmaStatusUpdater.stop(project)
                val (_, payload) = JCefMessagesUtils.parseJsonToObject(
                    request,
                    FinishRequest::class.java
                )
                val email = payload?.email
                PersistenceService.getInstance().setUserEmail(email)
                if (email != null) {
                    ActivityMonitor.getInstance(project).registerEmail(email)
                }
                EDT.ensureEDT {
                    updateInstallationWizardFlag()
                    ToolWindowShower.getInstance(project).showToolWindow()
                    MainToolWindowCardsController.getInstance(project).wizardFinished()
                    project.service<RecentActivityToolWindowShower>().showToolWindow()
                }
            }
            if (JCefMessagesUtils.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER.equals(action, ignoreCase = true)) {
                val (_, payload) = JCefMessagesUtils.parseJsonToObject(
                    request,
                    OpenInDefaultBrowserRequest::class.java
                )
                if (payload != null) {
                    ApplicationManager.getApplication().invokeLater {
                        BrowserUtil.browse(
                            payload.url
                        )
                    }
                }
            }
            if (JCefMessagesUtils.INSTALLATION_WIZARD_CHECK_CONNECTION.equals(action, ignoreCase = true)) {
                val jcefConnectionCheckMessagePayload: JcefConnectionCheckMessagePayload =
                    if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
                        JcefConnectionCheckMessagePayload(ConnectionCheckResult.SUCCESS.value)
                    } else {
                        AnalyticsService.getInstance(project).environment.refreshNowOnBackground()
                        JcefConnectionCheckMessagePayload(ConnectionCheckResult.FAILURE.value)
                    }
                val requestMessage = JCefBrowserUtil.resultToString(
                    JcefConnectionCheckMessageRequest(
                        JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
                        JCefMessagesUtils.INSTALLATION_WIZARD_SET_CHECK_CONNECTION,
                        jcefConnectionCheckMessagePayload
                    )
                )
                JCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser)
            }
            if (JCefMessagesUtils.INSTALLATION_WIZARD_INSTALL_DIGMA_ENGINE.equals(action, ignoreCase = true)) {

                localEngineOperationRunning.set(true)

                service<DockerService>().installEngine(project) { exitValue ->

                    if (exitValue == DockerService.NO_DOCKER_COMPOSE_COMMAND){
                        sendIsDockerInstalled(false, jbCefBrowser)
                        sendIsDockerComposeInstalled(false, jbCefBrowser)
                    }else{
                        sendIsDockerInstalled(true, jbCefBrowser)
                        sendIsDockerComposeInstalled(true, jbCefBrowser)
                    }

                    runBlocking {

                        val success = exitValue == "0"

                        if (success) {
                            //wait up to two minutes for connection, sometimes it takes more than a minute before connection is available
                            var i = 0
                            while (!BackendConnectionMonitor.getInstance(project).isConnectionOk() && i < 24) {
                                Log.log(logger::warn, "waiting for connection")
                                AnalyticsService.getInstance(project).environment.refreshNowOnBackground()
                                delay(5000)
                                i++
                            }
                        }


                        val connectionOk = BackendConnectionMonitor.getInstance(project).isConnectionOk()
                        if (!connectionOk) {
                            Log.log(logger::warn, "no connection after engine installation")
                            if (success) {
                                ActivityMonitor.getInstance(project)
                                    .registerDigmaEngineEventError("installEngine", "No connection 2 minutes after successful engine install")
                            }
                        }
                        val isEngineUp = connectionOk && success
                        if (isEngineUp) {
                            sendDockerResult(
                                ConnectionCheckResult.SUCCESS.value,
                                "",
                                jbCefBrowser,
                                JCefMessagesUtils.INSTALLATION_WIZARD_SET_INSTALL_DIGMA_ENGINE_RESULT
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
                                JCefMessagesUtils.INSTALLATION_WIZARD_SET_INSTALL_DIGMA_ENGINE_RESULT
                            )
                            sendIsDigmaEngineInstalled(false, jbCefBrowser)
                            sendIsDigmaEngineRunning(false, jbCefBrowser)

                            //start remove if install failed. wait a second to let the installEngine finish so it reports
                            // the installEngine.end to posthog before removeEngine.start
                            Backgroundable.executeOnPooledThread {
                                try {
                                    Thread.sleep(2000)
                                } catch (e: Exception) {
                                    //ignore
                                }
                                Log.log(logger::warn, "removing engine after installation failed")
                                service<DockerService>().removeEngine(project) { exitValue ->
                                    updateDigmaEngineStatus(project, jbCefBrowser.cefBrowser)
                                    if (exitValue != "0") {
                                        Log.log(logger::warn, "error removing engine after failure {}", exitValue)
                                    }
                                }
                            }

                        }
                    }

                    updateDigmaEngineStatus(project, jbCefBrowser.cefBrowser)
                    localEngineOperationRunning.set(false)

                }
            }
            if (JCefMessagesUtils.INSTALLATION_WIZARD_UNINSTALL_DIGMA_ENGINE.equals(action, ignoreCase = true)) {
                localEngineOperationRunning.set(true)
                service<DockerService>().removeEngine(project) { exitValue ->

                    if (exitValue == DockerService.NO_DOCKER_COMPOSE_COMMAND){
                        sendIsDockerInstalled(false, jbCefBrowser)
                        sendIsDockerComposeInstalled(false, jbCefBrowser)
                    }else{
                        sendIsDockerInstalled(true, jbCefBrowser)
                        sendIsDockerComposeInstalled(true, jbCefBrowser)
                    }

                    runBlocking {
                        val success = exitValue == "0"
                        if (success) {
                            sendDockerResult(
                                ConnectionCheckResult.SUCCESS.value,
                                "",
                                jbCefBrowser,
                                JCefMessagesUtils.INSTALLATION_WIZARD_SET_UNINSTALL_DIGMA_ENGINE_RESULT
                            )
                            sendIsDigmaEngineRunning(false, jbCefBrowser)
                            sendIsDigmaEngineInstalled(false, jbCefBrowser)

                        } else {
                            Log.log(logger::warn, "error uninstalling engine {}", exitValue)
                            sendDockerResult(
                                ConnectionCheckResult.FAILURE.value,
                                "Could not uninstall engine",
                                jbCefBrowser,
                                JCefMessagesUtils.INSTALLATION_WIZARD_SET_UNINSTALL_DIGMA_ENGINE_RESULT
                            )
                        }
                    }

                    updateDigmaEngineStatus(project, jbCefBrowser.cefBrowser)
                    localEngineOperationRunning.set(false)
                }
            }
            if (JCefMessagesUtils.INSTALLATION_WIZARD_START_DIGMA_ENGINE.equals(action, ignoreCase = true)) {
                localEngineOperationRunning.set(true)
                service<DockerService>().startEngine(project) { exitValue ->

                    if (exitValue == DockerService.NO_DOCKER_COMPOSE_COMMAND){
                        sendIsDockerInstalled(false, jbCefBrowser)
                        sendIsDockerComposeInstalled(false, jbCefBrowser)
                    }else{
                        sendIsDockerInstalled(true, jbCefBrowser)
                        sendIsDockerComposeInstalled(true, jbCefBrowser)
                    }

                    runBlocking {

                        val success = exitValue == "0"

                        if (success) {
                            //wait up to two minutes for connection, sometimes it takes more than a minute before connection is available
                            var i = 0
                            while (!BackendConnectionMonitor.getInstance(project).isConnectionOk() && i < 24) {
                                Log.log(logger::warn, "waiting for connection")
                                AnalyticsService.getInstance(project).environment.refreshNowOnBackground()
                                delay(5000)
                                i++
                            }
                        }

                        val connectionOk = BackendConnectionMonitor.getInstance(project).isConnectionOk()
                        if (!connectionOk) {
                            Log.log(logger::warn, "no connection after engine start")
                            if (success) {
                                ActivityMonitor.getInstance(project)
                                    .registerDigmaEngineEventError("startEngine", "No connection after successful engine start")
                            }
                        }

                        val isEngineUp = connectionOk && success
                        if (isEngineUp) {
                            sendDockerResult(
                                ConnectionCheckResult.SUCCESS.value,
                                "",
                                jbCefBrowser,
                                JCefMessagesUtils.INSTALLATION_WIZARD_SET_START_DIGMA_ENGINE_RESULT
                            )
                            sendIsDigmaEngineInstalled(true, jbCefBrowser)
                            sendIsDigmaEngineRunning(true, jbCefBrowser)
                        } else {
                            Log.log(logger::warn, "error starting engine {}", exitValue)

                            sendDockerResult(
                                ConnectionCheckResult.FAILURE.value,
                                "Could not start engine",
                                jbCefBrowser,
                                JCefMessagesUtils.INSTALLATION_WIZARD_SET_START_DIGMA_ENGINE_RESULT
                            )
                            sendIsDigmaEngineInstalled(true, jbCefBrowser)
                            sendIsDigmaEngineRunning(false, jbCefBrowser)
                        }

                    }

                    updateDigmaEngineStatus(project, jbCefBrowser.cefBrowser)
                    localEngineOperationRunning.set(false)

                }
            }
            if (JCefMessagesUtils.INSTALLATION_WIZARD_STOP_DIGMA_ENGINE.equals(action, ignoreCase = true)) {
                localEngineOperationRunning.set(true)
                service<DockerService>().stopEngine(project) { exitValue ->

                    if (exitValue == DockerService.NO_DOCKER_COMPOSE_COMMAND){
                        sendIsDockerInstalled(false, jbCefBrowser)
                        sendIsDockerComposeInstalled(false, jbCefBrowser)
                    }else{
                        sendIsDockerInstalled(true, jbCefBrowser)
                        sendIsDockerComposeInstalled(true, jbCefBrowser)
                    }

                    runBlocking {
                        val success = exitValue == "0"
                        if (success) {
                            sendDockerResult(
                                ConnectionCheckResult.SUCCESS.value,
                                "",
                                jbCefBrowser,
                                JCefMessagesUtils.INSTALLATION_WIZARD_SET_STOP_DIGMA_ENGINE_RESULT
                            )
                            sendIsDigmaEngineRunning(false, jbCefBrowser)
                        } else {
                            Log.log(logger::warn, "error stopping engine {}", exitValue)
                            sendDockerResult(
                                ConnectionCheckResult.FAILURE.value,
                                "Could not stop engine",
                                jbCefBrowser,
                                JCefMessagesUtils.INSTALLATION_WIZARD_SET_STOP_DIGMA_ENGINE_RESULT
                            )
                        }
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


    val jcefDigmaPanel = object: DisposablePanel(){
        override fun dispose() {
            digmaStatusUpdater.stop(project)
            jbCefBrowser.dispose()
            jbCefClient.dispose()
            msgRouter.dispose()
        }
    }
    jcefDigmaPanel.layout = BorderLayout()
    jcefDigmaPanel.add(browserPanel, BorderLayout.CENTER)



    ApplicationUISettingsChangeNotifier.getInstance(project).addSettingsChangeListener(object : SettingsChangeListener {
        override fun systemFontChange(fontName: String) {
            JCefBrowserUtil.sendRequestToChangeFont(fontName, jbCefBrowser)
        }

        override fun systemThemeChange(theme: Theme) {
            JCefBrowserUtil.sendRequestToChangeUiTheme(theme, jbCefBrowser)
        }

        override fun editorFontChange(fontName: String) {
            JCefBrowserUtil.sendRequestToChangeCodeFont(fontName, jbCefBrowser)
        }
    })


    project.messageBus.connect()
        .subscribe(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, object : AnalyticsServiceConnectionEvent {
            override fun connectionLost() {
                if (localEngineOperationRunning.get()) {
                    return
                }

                try {
                    val status = service<DockerService>().getCurrentDigmaInstallationStatusOnConnectionLost()
                    updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)
                } catch (e: Exception) {
                    ErrorReporter.getInstance().reportError("createInstallationWizardSidePanelWindowPanel.connectionLost", e)
                }
            }

            override fun connectionGained() {
                if (localEngineOperationRunning.get()) {
                    return
                }
                try {
                    val status = service<DockerService>().getCurrentDigmaInstallationStatusOnConnectionGained()
                    updateDigmaEngineStatus(jbCefBrowser.cefBrowser, status)
                } catch (e: Exception) {
                    ErrorReporter.getInstance().reportError("createInstallationWizardSidePanelWindowPanel.connectionGained", e)
                }
            }
        })



    return jcefDigmaPanel
}




private fun sendDockerResult(result: String, errorMsg: String, jbCefBrowser: JBCefBrowser, messageType: String) {

    val payload = JcefDockerResultPayload(result, errorMsg)

    val requestMessage = JCefBrowserUtil.resultToString(
        JcefDockerResultRequest(
            JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
            messageType,
            payload
        )
    )
    JCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser)
}


fun sendIsDigmaEngineInstalled(result: Boolean, jbCefBrowser: JBCefBrowser) {
    val payload = JcefDockerIsDigmaEngineInstalledPayload(result)
    val requestMessage = JCefBrowserUtil.resultToString(
        JcefDockerIsDigmaEngineInstalledRequest(
            JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
            JCefMessagesUtils.GLOBAL_SET_IS_DIGMA_ENGINE_INSTALLED,
            payload
        )
    )
    JCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser)
}


fun sendIsDigmaEngineRunning(success: Boolean, jbCefBrowser: JBCefBrowser) {

    val payload = JcefDockerIsDigmaEngineRunningPayload(success)
    val requestMessage = JCefBrowserUtil.resultToString(
        JcefDockerIsDigmaEngineRunningRequest(
            JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
            JCefMessagesUtils.GLOBAL_SET_IS_DIGMA_ENGINE_RUNNING,
            payload
        )
    )
    JCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser)
}




fun sendIsDockerInstalled(result: Boolean, jbCefBrowser: JBCefBrowser) {
    val isDockerInstalledPayload = JcefDockerIsDockerInstalledPayload(result)
    val isDockerInstalledRequestMessage = JCefBrowserUtil.resultToString(
        JcefDockerIsDockerInstalledRequest(
            JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
            JCefMessagesUtils.GLOBAL_SET_IS_DOCKER_INSTALLED,
            isDockerInstalledPayload
        )
    )
    JCefBrowserUtil.postJSMessage(isDockerInstalledRequestMessage, jbCefBrowser)
}

fun sendIsDockerComposeInstalled(result: Boolean, jbCefBrowser: JBCefBrowser) {
    val isDockerComposeInstalledPayload = JcefDockerIsDockerComposeInstalledPayload(result)
    val isDockerComposeInstalledRequestMessage = JCefBrowserUtil.resultToString(
        JcefDockerIsDockerComposeInstalledRequest(
            JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
            JCefMessagesUtils.GLOBAL_SET_IS_DOCKER_COMPOSE_INSTALLED,
            isDockerComposeInstalledPayload
        )
    )
    JCefBrowserUtil.postJSMessage(isDockerComposeInstalledRequestMessage, jbCefBrowser)
}


class DigmaStatusUpdater {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private var myDisposable: Disposable? = null

    private var digmaInstallationStatus = AtomicReference<DigmaInstallationStatus?>(null)

    fun start(project: Project, cefBrowser: CefBrowser) {

        Log.log(logger::trace, project, "starting DigmaStatusUpdater")

        myDisposable = Disposer.newDisposable()
        digmaInstallationStatus.set(null)

        myDisposable?.let {
            @Suppress("UnstableApiUsage")
            it.disposingScope().launch {
                try {
                    while (isActive) {

                        val currentStatus = service<DockerService>().getActualRunningEngine(project)

                        if (!isActive) break

                        //DigmaInstallationStatus is data class so we can rely on equals
                        if (digmaInstallationStatus.get() == null || currentStatus != digmaInstallationStatus.get()) {
                            Log.log(logger::trace, project, "status changed current:{}, previous:{}", currentStatus, digmaInstallationStatus)
                            digmaInstallationStatus.set(currentStatus)
                            Log.log(logger::trace, project, "updating wizard with digmaInstallationStatus {}", digmaInstallationStatus)
                            digmaInstallationStatus.get()?.let { status ->
                                updateDigmaEngineStatus(cefBrowser, status)
                            }

                        }

                        delay(2000)
                    }

                } catch (e: Exception) {
                    //no need to report CancellationException , it's not an error, we cancel the job
                    if (e !is CancellationException) {
                        ErrorReporter.getInstance().reportError(project, "DigmaStatusUpdater.loop", e)
                    }

                }
            }
        }
    }

    fun stop(project: Project) {
        myDisposable?.let {
            Disposer.dispose(it)
        }
    }


}