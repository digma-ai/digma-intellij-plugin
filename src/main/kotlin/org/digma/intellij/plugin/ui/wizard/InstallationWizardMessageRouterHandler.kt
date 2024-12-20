package org.digma.intellij.plugin.ui.wizard

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.analytics.refreshEnvironmentsNowOnBackground
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.digmathon.DigmathonService
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.docker.LocalInstallationFacade
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.recentactivity.RecentActivityToolWindowShower
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.common.updateObservabilityValue
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import org.digma.intellij.plugin.ui.jcef.jsonToObject
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.jcef.updateDigmaEngineStatus
import org.digma.intellij.plugin.ui.notificationcenter.AppNotificationCenter
import org.digma.intellij.plugin.ui.wizard.model.ConnectionCheckMessagePayload
import org.digma.intellij.plugin.ui.wizard.model.ConnectionCheckMessageRequest
import org.digma.intellij.plugin.ui.wizard.model.ConnectionCheckResult
import org.digma.intellij.plugin.ui.wizard.model.FinishRequest
import org.digma.intellij.plugin.ui.wizard.model.SetObservabilityRequest
import java.util.concurrent.atomic.AtomicBoolean

class InstallationWizardMessageRouterHandler(
    project: Project,
    private val digmaStatusUpdater: InstallationWizardPanel.DigmaStatusUpdater,
    private val localEngineOperationRunning: AtomicBoolean
) :
    BaseMessageRouterHandler(project) {

    override fun getOriginForTroubleshootingEvent(): String {
        return "wizard"
    }

    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {

        when (action) {

            "INSTALLATION_WIZARD/INITIALIZE" -> {
                digmaStatusUpdater.start(project, browser)
            }

            "INSTALLATION_WIZARD/CLOSE" -> {
                digmaStatusUpdater.stop()
                EDT.ensureEDT {
                    MainToolWindowCardsController.getInstance(project).wizardFinished()
                }
            }

            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_OBSERVABILITY -> {
                val (_, payload) = jsonToObject(rawRequest, SetObservabilityRequest::class.java)
                if (payload != null) {
                    updateObservabilityValue(project, payload.isObservabilityEnabled)
                }
            }

            JCEFGlobalConstants.INSTALLATION_WIZARD_FINISH -> {
                digmaStatusUpdater.stop()
                val (_, payload) = jsonToObject(rawRequest, FinishRequest::class.java)
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
                    RecentActivityToolWindowShower.getInstance(project).showToolWindow()
                }
            }

            JCEFGlobalConstants.INSTALLATION_WIZARD_CHECK_CONNECTION -> {
                val connectionCheckMessagePayload: ConnectionCheckMessagePayload =
                    if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
                        ConnectionCheckMessagePayload(ConnectionCheckResult.SUCCESS.value)
                    } else {
                        refreshEnvironmentsNowOnBackground(project)
                        ConnectionCheckMessagePayload(ConnectionCheckResult.FAILURE.value)
                    }
                val message = ConnectionCheckMessageRequest(connectionCheckMessagePayload)
                serializeAndExecuteWindowPostMessageJavaScript(browser, message)
            }


            JCEFGlobalConstants.INSTALLATION_WIZARD_INSTALL_DIGMA_ENGINE -> {

                localEngineOperationRunning.set(true)

                service<LocalInstallationFacade>().installEngine(project) { exitValue ->

                    EDT.assertNonDispatchThread()

                    if (exitValue == DockerService.NO_DOCKER_COMPOSE_COMMAND) {
                        sendIsDockerInstalled(false, browser)
                        sendIsDockerComposeInstalled(false, browser)
                    } else {
                        sendIsDockerInstalled(true, browser)
                        sendIsDockerComposeInstalled(true, browser)
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
                            browser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_INSTALL_DIGMA_ENGINE_RESULT
                        )
                        sendIsDigmaEngineInstalled(true, browser)
                        sendIsDigmaEngineRunning(true, browser)

                        service<AppNotificationCenter>().showInstallationFinishedNotification(project)
                    } else {
                        Log.log(logger::warn, "error installing engine, {}", exitValue)

                        sendDockerResult(
                            ConnectionCheckResult.FAILURE.value,
                            "Could not install engine",
                            browser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_INSTALL_DIGMA_ENGINE_RESULT
                        )
                        sendIsDigmaEngineInstalled(false, browser)
                        sendIsDigmaEngineRunning(false, browser)

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
                                updateDigmaEngineStatus(project, browser)
                                if (exitValue != "0") {
                                    Log.log(logger::warn, "error removing engine after failure {}", exitValue)
                                }
                            }
                        }

                    }

                    updateDigmaEngineStatus(project, browser)
                    localEngineOperationRunning.set(false)

                }
            }

            JCEFGlobalConstants.INSTALLATION_WIZARD_UNINSTALL_DIGMA_ENGINE -> {
                localEngineOperationRunning.set(true)
                service<LocalInstallationFacade>().removeEngine(project) { exitValue ->

                    EDT.assertNonDispatchThread()

                    if (exitValue == DockerService.NO_DOCKER_COMPOSE_COMMAND) {
                        sendIsDockerInstalled(false, browser)
                        sendIsDockerComposeInstalled(false, browser)
                    } else {
                        sendIsDockerInstalled(true, browser)
                        sendIsDockerComposeInstalled(true, browser)
                    }

                    val success = exitValue == "0"
                    if (success) {
                        sendDockerResult(
                            ConnectionCheckResult.SUCCESS.value,
                            "",
                            browser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_UNINSTALL_DIGMA_ENGINE_RESULT
                        )
                        sendIsDigmaEngineRunning(false, browser)
                        sendIsDigmaEngineInstalled(false, browser)

                    } else {
                        Log.log(logger::warn, "error uninstalling engine {}", exitValue)
                        sendDockerResult(
                            ConnectionCheckResult.FAILURE.value,
                            "Could not uninstall engine",
                            browser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_UNINSTALL_DIGMA_ENGINE_RESULT
                        )
                    }

                    updateDigmaEngineStatus(project, browser)
                    localEngineOperationRunning.set(false)
                }
            }

            JCEFGlobalConstants.INSTALLATION_WIZARD_START_DIGMA_ENGINE -> {
                localEngineOperationRunning.set(true)
                service<LocalInstallationFacade>().startEngine(project) { exitValue ->

                    EDT.assertNonDispatchThread()

                    if (exitValue == DockerService.NO_DOCKER_COMPOSE_COMMAND) {
                        sendIsDockerInstalled(false, browser)
                        sendIsDockerComposeInstalled(false, browser)
                    } else {
                        sendIsDockerInstalled(true, browser)
                        sendIsDockerComposeInstalled(true, browser)
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
                            browser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_START_DIGMA_ENGINE_RESULT
                        )
                        sendIsDigmaEngineInstalled(true, browser)
                        sendIsDigmaEngineRunning(true, browser)
                    } else {
                        Log.log(logger::warn, "error starting engine {}", exitValue)

                        sendDockerResult(
                            ConnectionCheckResult.FAILURE.value,
                            "Could not start engine",
                            browser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_START_DIGMA_ENGINE_RESULT
                        )
                        sendIsDigmaEngineInstalled(true, browser)
                        sendIsDigmaEngineRunning(false, browser)
                    }


                    updateDigmaEngineStatus(project, browser)
                    localEngineOperationRunning.set(false)

                }
            }

            JCEFGlobalConstants.INSTALLATION_WIZARD_STOP_DIGMA_ENGINE -> {
                localEngineOperationRunning.set(true)
                service<LocalInstallationFacade>().stopEngine(project) { exitValue ->

                    EDT.assertNonDispatchThread()

                    if (exitValue == DockerService.NO_DOCKER_COMPOSE_COMMAND) {
                        sendIsDockerInstalled(false, browser)
                        sendIsDockerComposeInstalled(false, browser)
                    } else {
                        sendIsDockerInstalled(true, browser)
                        sendIsDockerComposeInstalled(true, browser)
                    }

                    val success = exitValue == "0"
                    if (success) {
                        sendDockerResult(
                            ConnectionCheckResult.SUCCESS.value,
                            "",
                            browser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_STOP_DIGMA_ENGINE_RESULT
                        )
                        sendIsDigmaEngineRunning(false, browser)
                    } else {
                        Log.log(logger::warn, "error stopping engine {}", exitValue)
                        sendDockerResult(
                            ConnectionCheckResult.FAILURE.value,
                            "Could not stop engine",
                            browser,
                            JCEFGlobalConstants.INSTALLATION_WIZARD_SET_STOP_DIGMA_ENGINE_RESULT
                        )
                    }

                    updateDigmaEngineStatus(project, browser)
                    localEngineOperationRunning.set(false)
                }
            }

            else -> return false
        }

        return true
    }
}