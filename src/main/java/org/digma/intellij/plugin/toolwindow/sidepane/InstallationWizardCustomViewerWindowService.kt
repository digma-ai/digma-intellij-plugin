package org.digma.intellij.plugin.toolwindow.sidepane

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.BackendConnectionUtil
import org.digma.intellij.plugin.toolwindow.common.CustomViewerWindow

private const val INSTALLATION_WIZARD_RESOURCE_FOLDER_NAME = "installationwizard"
private const val ENV_VARIABLE_IDE: String = "ide"
private const val WIZARD_SKIP_INSTALLATION_STEP_VARIABLE: String = "wizardSkipInstallationStep"

class InstallationWizardCustomViewerWindowService(val project: Project) {

    fun getCustomViewerWindow(): CustomViewerWindow {
        val isServerConnectedAlready = BackendConnectionUtil.getInstance(project).testConnectionToBackend()
        return CustomViewerWindow(
            project, INSTALLATION_WIZARD_RESOURCE_FOLDER_NAME,
            mapOf(
                WIZARD_SKIP_INSTALLATION_STEP_VARIABLE to isServerConnectedAlready,
                ENV_VARIABLE_IDE to ApplicationNamesInfo.getInstance().productName //Available values: "IDEA", "Rider", "PyCharm"
            )
        )
    }
}