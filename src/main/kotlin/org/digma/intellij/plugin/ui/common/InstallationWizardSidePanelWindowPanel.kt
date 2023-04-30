package org.digma.intellij.plugin.ui.common

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.jcef.JBCefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.analytics.BackendConnectionUtil
import org.digma.intellij.plugin.common.IDEUtilsService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.installationwizard.FinishRequest
import org.digma.intellij.plugin.model.rest.installationwizard.OpenInBrowserRequest
import org.digma.intellij.plugin.model.rest.installationwizard.SendTrackingEventRequest
import org.digma.intellij.plugin.model.rest.installationwizard.SetObservabilityRequest
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.toolwindow.common.CustomViewerWindow
import org.digma.intellij.plugin.toolwindow.common.ThemeChangeListener
import org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil
import org.digma.intellij.plugin.toolwindow.recentactivity.ConnectionCheckResult
import org.digma.intellij.plugin.toolwindow.recentactivity.JBCefBrowserUtil
import org.digma.intellij.plugin.toolwindow.recentactivity.JcefConnectionCheckMessagePayload
import org.digma.intellij.plugin.toolwindow.recentactivity.JcefConnectionCheckMessageRequest
import org.digma.intellij.plugin.toolwindow.recentactivity.JcefMessageRequest
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.common.ObservabilityUtil.Companion.updateObservabilityValue
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.UIManager

private const val RESOURCE_FOLDER_NAME = "installationwizard"
private const val ENV_VARIABLE_IDE: String = "ide"
private const val WIZARD_SKIP_INSTALLATION_STEP_VARIABLE: String = "wizardSkipInstallationStep"

private val logger: Logger =
    Logger.getInstance("org.digma.intellij.plugin.ui.common.InstallationWizardSidePanelWindowPanel")

fun createInstallationWizardSidePanelWindowPanel(project: Project): JPanel? {
    if (!JBCefApp.isSupported()) {
        // Fallback to an alternative browser-less solution
        return null
    }

    val isServerConnectedAlready = BackendConnectionUtil.getInstance(project).testConnectionToBackend()
    val customViewerWindow = CustomViewerWindow(
        project, RESOURCE_FOLDER_NAME,
        mapOf(
            WIZARD_SKIP_INSTALLATION_STEP_VARIABLE to isServerConnectedAlready,
            ENV_VARIABLE_IDE to ApplicationNamesInfo.getInstance().productName //Available values: "IDEA", "Rider", "PyCharm"
        )
    )
    val jbCefBrowser = customViewerWindow.getWebView()

    val jbCefClient = jbCefBrowser.jbCefClient

    val msgRouter = CefMessageRouter.create()

    val listener = ThemeChangeListener(jbCefBrowser)
    UIManager.addPropertyChangeListener(listener)

    msgRouter.addHandler(object : CefMessageRouterHandlerAdapter() {
        override fun onQuery(
            browser: CefBrowser,
            frame: CefFrame,
            queryId: Long,
            request: String,
            persistent: Boolean,
            callback: CefQueryCallback
        ): Boolean {
            Log.log(logger::debug, "request: {}", request)

            val (_, action) = ToolWindowUtil.parseJsonToObject(
                request,
                JcefMessageRequest::class.java
            )
            if (ToolWindowUtil.INSTALLATION_WIZARD_SEND_TRACKING_EVENT.equals(action, ignoreCase = true)) {
                val (_, payload) = ToolWindowUtil.parseJsonToObject(
                    request,
                    SendTrackingEventRequest::class.java
                )
                if (payload != null) {
                    ActivityMonitor.getInstance(project).registerCustomEvent(payload.eventName, payload.data)
                }
            }
            if (ToolWindowUtil.INSTALLATION_WIZARD_SET_OBSERVABILITY.equals(action, ignoreCase = true)) {
                val (_, payload) = ToolWindowUtil.parseJsonToObject(
                    request,
                    SetObservabilityRequest::class.java
                )
                if (payload != null) {
                    updateObservabilityValue(project, payload.isObservabilityEnabled)
                }
            }
            if (ToolWindowUtil.INSTALLATION_WIZARD_FINISH.equals(action, ignoreCase = true)) {
                val (_, payload) = ToolWindowUtil.parseJsonToObject(
                    request,
                    FinishRequest::class.java
                )
                if (payload?.email != null) {
                    ActivityMonitor.getInstance(project).registerEmail( payload.email!!)
                }
                ApplicationManager.getApplication().invokeLater {
                    updateInstallationWizardFlag()
                    ToolWindowShower.getInstance(project).displayMainSidePaneWindowPanel()
                    ToolWindowManager.getInstance(project).getToolWindow(PluginId.OBSERVABILITY_WINDOW_ID)!!.show()
                }
            }
            if (ToolWindowUtil.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER.equals(action, ignoreCase = true)) {
                val (_, payload) = ToolWindowUtil.parseJsonToObject(
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
            if (ToolWindowUtil.INSTALLATION_WIZARD_CHECK_CONNECTION.equals(action, ignoreCase = true)) {
                val jcefConnectionCheckMessagePayload: JcefConnectionCheckMessagePayload =
                    if (BackendConnectionUtil.getInstance(project).testConnectionToBackend()) {
                        JcefConnectionCheckMessagePayload(ConnectionCheckResult.SUCCESS.value)
                    } else {
                        JcefConnectionCheckMessagePayload(ConnectionCheckResult.FAILURE.value)
                    }
                val requestMessage = JBCefBrowserUtil.resultToString(
                    JcefConnectionCheckMessageRequest(
                        ToolWindowUtil.REQUEST_MESSAGE_TYPE,
                        ToolWindowUtil.INSTALLATION_WIZARD_SET_CHECK_CONNECTION,
                        jcefConnectionCheckMessagePayload
                    )
                )
                JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser)
            }
            callback.success("")
            return true
        }
    }, true)

    jbCefClient.cefClient.addMessageRouter(msgRouter)

    val browserPanel = JPanel()
    browserPanel.layout = BorderLayout()
    browserPanel.add(jbCefBrowser.component, BorderLayout.CENTER)


    val jcefDigmaPanel = JPanel()
    jcefDigmaPanel.layout = BorderLayout()
    jcefDigmaPanel.add(browserPanel, BorderLayout.CENTER)

    return jcefDigmaPanel
}

/**
 * Set global flag that this user has already passed the installation wizard
 */
private fun updateInstallationWizardFlag() {
    val productName = ApplicationNamesInfo.getInstance().productName
    if (IDEUtilsService.isIdeaIDE(productName)) {
        if (!PersistenceService.getInstance().state.alreadyPassedTheInstallationWizardForIdeaIDE) {
            PersistenceService.getInstance().state.alreadyPassedTheInstallationWizardForIdeaIDE = true
        }
    } else if (IDEUtilsService.isRiderIDE(productName)) {
        if (!PersistenceService.getInstance().state.alreadyPassedTheInstallationWizardForRiderIDE) {
            PersistenceService.getInstance().state.alreadyPassedTheInstallationWizardForRiderIDE = true
        }
    } else if (IDEUtilsService.isPyCharmIDE(productName)) {
        if (!PersistenceService.getInstance().state.alreadyPassedTheInstallationWizardForPyCharmIDE) {
            PersistenceService.getInstance().state.alreadyPassedTheInstallationWizardForPyCharmIDE = true
        }
    }
}