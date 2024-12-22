package org.digma.intellij.plugin.ui.wizard

import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.jcef.BaseResourceHandler
import org.digma.intellij.plugin.ui.jcef.JCEF_WIZARD_SKIP_INSTALLATION_STEP_PROPERTY_NAME
import org.digma.intellij.plugin.ui.jcef.getProject
import org.digma.intellij.plugin.ui.jcef.getProperty
import java.io.InputStream

class InstallationWizardResourceHandler(browser: CefBrowser, path: String) : BaseResourceHandler(path, browser) {

    override fun getResourceFolderName(): String {
        return INSTALLATION_WIZARD_RESOURCE_FOLDER_NAME
    }

    override fun buildEnvJsFromTemplate(path: String): InputStream? {
        val project = getProject(browser)
        if (project == null) {
            Log.log(logger::warn, "project is null , should never happen")
            ErrorReporter.getInstance().reportError(null, "InstallationWizardResourceHandler.buildEnvJsFromTemplate", "project is null", mapOf())
            return null
        }

        val wizardSkipInstallationStep = getProperty(browser, JCEF_WIZARD_SKIP_INSTALLATION_STEP_PROPERTY_NAME) as Boolean?
        if (wizardSkipInstallationStep == null) {
            Log.log(logger::warn, "wizardSkipInstallationStep is null , should never happen")
            ErrorReporter.getInstance()
                .reportError(null, "InstallationWizardResourceHandler.buildEnvJsFromTemplate", "wizardSkipInstallationStep is null", mapOf())
            return null
        }

        return InstallationWizardEnvJsTemplateBuilder(path, wizardSkipInstallationStep).build(project)
    }
}
