package org.digma.intellij.plugin.toolwindow.sidepane

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.toolwindow.common.CustomViewerWindow

private const val INSTALLATION_WIZARD_RESOURCE_FOLDER_NAME = "installationwizard"
class InstallationWizardCustomViewerWindowService(val project: Project) {
    val customViewerWindow = CustomViewerWindow(project, INSTALLATION_WIZARD_RESOURCE_FOLDER_NAME)
}