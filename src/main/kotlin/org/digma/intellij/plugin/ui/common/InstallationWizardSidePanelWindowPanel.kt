package org.digma.intellij.plugin.ui.common

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.digma.intellij.plugin.analytics.BackendConnectionUtil
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.IDEUtilsService
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.jcef.common.CustomSchemeHandlerFactory
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.jcef.common.OpenInBrowserRequest
import org.digma.intellij.plugin.model.rest.jcef.common.SendTrackingEventRequest
import org.digma.intellij.plugin.model.rest.jcef.installationwizard.FinishRequest
import org.digma.intellij.plugin.model.rest.jcef.installationwizard.SetObservabilityRequest
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.recentactivity.ConnectionCheckResult
import org.digma.intellij.plugin.recentactivity.JcefConnectionCheckMessagePayload
import org.digma.intellij.plugin.recentactivity.JcefConnectionCheckMessageRequest
import org.digma.intellij.plugin.recentactivity.JcefDockerResultPayload
import org.digma.intellij.plugin.recentactivity.JcefDockerResultRequest
import org.digma.intellij.plugin.recentactivity.JcefMessageRequest
import org.digma.intellij.plugin.recentactivity.RecentActivityToolWindowShower
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.common.ObservabilityUtil.Companion.updateObservabilityValue
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import org.digma.intellij.plugin.ui.settings.ApplicationUISettingsChangeNotifier
import org.digma.intellij.plugin.ui.settings.SettingsChangeListener
import org.digma.intellij.plugin.ui.settings.Theme
import java.awt.BorderLayout
import javax.swing.JPanel

private const val RESOURCE_FOLDER_NAME = "installationwizard"
private const val ENV_VARIABLE_IDE: String = "ide"
private const val WIZARD_SKIP_INSTALLATION_STEP_VARIABLE: String = "wizardSkipInstallationStep"
private const val USER_EMAIL_VARIABLE: String = "userEmail"
private const val IS_OBSERVABILITY_ENABLED_VARIABLE: String = "isObservabilityEnabled"
private const val IS_DOCKER_INSTALLED: String = "isDockerInstalled"
private const val IS_DOCKER_COMPOSE_INSTALLED: String = "isDockerComposeInstalled"
private const val IS_DIGMA_ENGINE_INSTALLED: String = "isDigmaEngineInstalled"
private const val IS_DIGMA_ENGINE_RUNNING: String = "isDigmaEngineRunning"
private const val IS_WIZARD_FIRST_LAUNCH: String = "wizardFirstLaunch"

private val logger: Logger =
    Logger.getInstance("org.digma.intellij.plugin.ui.common.InstallationWizardSidePanelWindowPanel")

fun createInstallationWizardSidePanelWindowPanel(project: Project): DisposablePanel? {

    if (!JBCefApp.isSupported()) {
        // Fallback to an alternative browser-less solution
        return null
    }

    //at this stage the AnalyticsService was initialized already and if there is no connection then
    // BackendConnectionMonitor should already know that
    val isServerConnectedAlready = BackendConnectionUtil.getInstance(project).testConnectionToBackend()

    val jbCefBrowser = JBCefBrowserBuilderCreator.create()
        .setUrl("https://$RESOURCE_FOLDER_NAME/index.html")
        .build()
    val indexTemplateData = mutableMapOf<String, Any>(
        WIZARD_SKIP_INSTALLATION_STEP_VARIABLE to isServerConnectedAlready,
        ENV_VARIABLE_IDE to ApplicationNamesInfo.getInstance().productName, //Available values: "IDEA", "Rider", "PyCharm"
        USER_EMAIL_VARIABLE to (PersistenceService.getInstance().state.userEmail ?: ""),
        IS_OBSERVABILITY_ENABLED_VARIABLE to PersistenceService.getInstance().state.isAutoOtel,
        IS_DOCKER_INSTALLED to service<DockerService>().isDockerInstalled(),
        IS_DOCKER_COMPOSE_INSTALLED to service<DockerService>().isDockerComposeInstalled(),
        IS_DIGMA_ENGINE_INSTALLED to service<DockerService>().isEngineInstalled(),
        IS_DIGMA_ENGINE_RUNNING to service<DockerService>().isEngineRunning(project),
        IS_WIZARD_FIRST_LAUNCH to service<DockerService>().isEngineInstalled(),
    )
    CefApp.getInstance()
        .registerSchemeHandlerFactory(
            "https",
            RESOURCE_FOLDER_NAME,
            CustomSchemeHandlerFactory(RESOURCE_FOLDER_NAME, indexTemplateData)
        )

    val jbCefClient = jbCefBrowser.jbCefClient

    val msgRouter = CefMessageRouter.create()


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
                }
            }
            if (JCefMessagesUtils.INSTALLATION_WIZARD_FINISH.equals(action, ignoreCase = true)) {
                val (_, payload) = JCefMessagesUtils.parseJsonToObject(
                    request,
                    FinishRequest::class.java
                )
                val email = payload?.email
                PersistenceService.getInstance().state.userEmail = email
                if (email != null) {
                    ActivityMonitor.getInstance(project).registerEmail(email)
                }
                EDT.ensureEDT {
                    updateInstallationWizardFlag()
                    ToolWindowShower.getInstance(project).showToolWindow()
                    MainToolWindowCardsController.getInstance(project).wizardFinished()
                    RecentActivityToolWindowShower.getInstance(project).showToolWindow()
                }
            }
            if (JCefMessagesUtils.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER.equals(action, ignoreCase = true)) {
                val (_, payload) = JCefMessagesUtils.parseJsonToObject(
                    request,
                    OpenInBrowserRequest::class.java
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
                    if (BackendConnectionUtil.getInstance(project).testConnectionToBackend()) {
                        JcefConnectionCheckMessagePayload(ConnectionCheckResult.SUCCESS.value)
                    } else {
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
                service<DockerService>().installEngine(project) {
                    sendDockerResult(it, jbCefBrowser, JCefMessagesUtils.INSTALLATION_WIZARD_SET_INSTALL_DIGMA_ENGINE_RESULT)
                }
            }
            if (JCefMessagesUtils.INSTALLATION_WIZARD_UNINSTALL_DIGMA_ENGINE.equals(action, ignoreCase = true)) {
                service<DockerService>().removeEngine(project) {
                    sendDockerResult(it, jbCefBrowser, JCefMessagesUtils.INSTALLATION_WIZARD_SET_UNINSTALL_DIGMA_ENGINE_RESULT)
                }
            }
            if (JCefMessagesUtils.INSTALLATION_WIZARD_START_DIGMA_ENGINE.equals(action, ignoreCase = true)) {
                service<DockerService>().startEngine(project) {
                    sendDockerResult(it, jbCefBrowser, JCefMessagesUtils.INSTALLATION_WIZARD_SET_START_DIGMA_ENGINE_RESULT)
                }
            }
            if (JCefMessagesUtils.INSTALLATION_WIZARD_STOP_DIGMA_ENGINE.equals(action, ignoreCase = true)) {
                service<DockerService>().stopEngine(project) {
                    sendDockerResult(it, jbCefBrowser, JCefMessagesUtils.INSTALLATION_WIZARD_SET_STOP_DIGMA_ENGINE_RESULT)
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



    return jcefDigmaPanel
}

/**
 * Set global flag that this user has already passed the installation wizard
 */
private fun updateInstallationWizardFlag() {
    if (IDEUtilsService.isIdeaIDE()) {
        if (!PersistenceService.getInstance().state.alreadyPassedTheInstallationWizardForIdeaIDE) {
            PersistenceService.getInstance().state.alreadyPassedTheInstallationWizardForIdeaIDE = true
        }
    } else if (IDEUtilsService.isRiderIDE()) {
        if (!PersistenceService.getInstance().state.alreadyPassedTheInstallationWizardForRiderIDE) {
            PersistenceService.getInstance().state.alreadyPassedTheInstallationWizardForRiderIDE = true
        }
    } else if (IDEUtilsService.isPyCharmIDE()) {
        if (!PersistenceService.getInstance().state.alreadyPassedTheInstallationWizardForPyCharmIDE) {
            PersistenceService.getInstance().state.alreadyPassedTheInstallationWizardForPyCharmIDE = true
        }
    }
}


private fun sendDockerResult(msg: String, jbCefBrowser: JBCefBrowser, messageType: String) {
    val payload = if (msg == "0") {
        JcefDockerResultPayload("success", "")
    } else {
        JcefDockerResultPayload("failure", msg)
    }

    val requestMessage = JCefBrowserUtil.resultToString(
        JcefDockerResultRequest(
            JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
            messageType,
            payload
        )
    )
    JCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser)
}
