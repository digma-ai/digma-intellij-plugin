package org.digma.intellij.plugin.ui.common

import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.docker.DockerService
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


    fun updateBackend(project: Project, backendDeploymentType: BackendDeploymentType, sourceComponent: JComponent?) {

        when (backendDeploymentType) {

            BackendDeploymentType.Helm -> {
                EditorService.getInstance(project).openClasspathResourceReadOnly(UPDATE_GUIDE_HELM_NAME, UPDATE_GUIDE_HELM_PATH)
            }

            BackendDeploymentType.DockerCompose -> {
                if (service<DockerService>().isEngineInstalled()) {
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
                    service<DockerService>().upgradeEngine(project)
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


}