package org.digma.intellij.plugin.ui.common

import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.docker.LocalInstallationFacade
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.service.EditorService
import org.digma.intellij.plugin.ui.common.Links.DIGMA_DOCKER_APP_URL
import javax.swing.JComponent
import javax.swing.JLabel

const val UPDATE_GUIDE_DOCKER_COMPOSE_PATH = "/guides/upgrade_docker_compose.md"
const val UPDATE_GUIDE_DOCKER_COMPOSE_NAME = "upgrade_docker_compose.md"
const val UPDATE_GUIDE_HELM_PATH = "/guides/upgrade_helm.md"
const val UPDATE_GUIDE_HELM_NAME = "upgrade_helm.md"


class UpdateBackendAction {

    private val logger = Logger.getInstance(this::class.java)

    fun updateBackend(project: Project, backendDeploymentType: BackendDeploymentType, sourceComponent: JComponent?) {

        Log.log(logger::info, "updateBackend invoked, backendDeploymentType {}, [t:{}]", backendDeploymentType, Thread.currentThread().name)

        when (backendDeploymentType) {

            BackendDeploymentType.Helm -> {
                EditorService.getInstance(project).openClasspathResourceReadOnly(UPDATE_GUIDE_HELM_NAME, UPDATE_GUIDE_HELM_PATH)
            }

            BackendDeploymentType.DockerCompose -> {
                if (service<LocalInstallationFacade>().isLocalEngineInstalled()) {
                    sourceComponent?.let {
                        val upgradePopupLabel = JLabel(
                            asHtml(
                                "<p>" +
                                        "<b>The Digma local engine is being updated</b>" +
                                        "</p><p>This can take a few minutes in which Digma may be offline</p>"
                            )
                        )
                        upgradePopupLabel.border = empty(3)
                        HintManager.getInstance()
                            .showHint(upgradePopupLabel, RelativePoint.getNorthWestOf(it), HintManager.HIDE_BY_ESCAPE, 5000)
                    }
                    Log.log(logger::info, "calling upgrade backend for local engine. [t:{}]", Thread.currentThread().name)
                    service<LocalInstallationFacade>().upgradeEngine(project) { exitValue ->
                        if (exitValue != "0") {
                            Log.log(logger::warn, "error upgrading local engine , exitValue {}. [t:{}]", exitValue, Thread.currentThread().name)
                            ErrorReporter.getInstance().reportError(
                                "UpdateBackendAction.upgradeLocalEngine",
                                "failed to upgrade local engine",
                                mapOf("exitValue" to exitValue)
                            )
                        }
                        tryToUpdateConnectionStatusSoon(project)
                    }
                } else {
                    EditorService.getInstance(project)
                        .openClasspathResourceReadOnly(UPDATE_GUIDE_DOCKER_COMPOSE_NAME, UPDATE_GUIDE_DOCKER_COMPOSE_PATH)
                }
            }

            BackendDeploymentType.DockerExtension -> {
                BrowserUtil.browse(DIGMA_DOCKER_APP_URL, project)
            }

            else -> {
                // default fallback to Docker Extension
                BrowserUtil.browse(DIGMA_DOCKER_APP_URL, project)
            }
        }
    }


    //call some api to refresh the connection status as soon as possible
    private fun tryToUpdateConnectionStatusSoon(project: Project) {
        Log.log(logger::info, "trying to update connection status soon [t:{}]", Thread.currentThread().name)
        repeat(24) { count ->
            if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
                return@repeat
            }

            try {
                Log.log(logger::info, "waiting for connection {} [t:{}]", count, Thread.currentThread().name)
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                //ignore
            }

            try {
                AnalyticsService.getInstance(project).environments
            } catch (e: Throwable) {
                //ignore
            }
        }

        if (!BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
            Log.log(logger::warn, "connection status is ok. [t:{}]", Thread.currentThread().name)
        } else {
            Log.log(logger::warn, "connection status is not ok. [t:{}]", Thread.currentThread().name)
        }
    }


}