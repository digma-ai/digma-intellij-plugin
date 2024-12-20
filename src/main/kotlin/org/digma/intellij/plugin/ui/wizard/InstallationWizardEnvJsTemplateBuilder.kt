package org.digma.intellij.plugin.ui.wizard

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.jcef.BaseEnvJsTemplateBuilder

private const val IS_WIZARD_SKIP_INSTALLATION_STEP = "wizardSkipInstallationStep"
private const val IS_WIZARD_FIRST_LAUNCH = "wizardFirstLaunch"


class InstallationWizardEnvJsTemplateBuilder(templatePath: String, private val wizardSkipInstallationStep: Boolean) :
    BaseEnvJsTemplateBuilder(templatePath) {

    override fun addAppSpecificEnvVariable(project: Project, data: MutableMap<String, Any>) {
        data[IS_WIZARD_SKIP_INSTALLATION_STEP] = wizardSkipInstallationStep
        data[IS_WIZARD_FIRST_LAUNCH] = PersistenceService.getInstance().isFirstWizardLaunch()
    }
}