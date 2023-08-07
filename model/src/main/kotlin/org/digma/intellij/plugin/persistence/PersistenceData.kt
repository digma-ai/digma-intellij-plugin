package org.digma.intellij.plugin.persistence

data class PersistenceData(
        var currentEnv: String? = null,
        var isWorkspaceOnly: Boolean = false,
        var isAutoOtel: Boolean = false,
        var alreadyPassedTheInstallationWizardForIdeaIDE: Boolean = false,
        var alreadyPassedTheInstallationWizardForRiderIDE: Boolean = false,
        var alreadyPassedTheInstallationWizardForPyCharmIDE: Boolean = false,
        var firstTimeConnectionEstablished: Boolean = true,
        var firstTimePluginLoaded: Boolean = false,
        var firstTimeInsightReceived: Boolean = false,
        var firstTimeAssetsReceived: Boolean = false,
        var userEmail: String? = null
)
