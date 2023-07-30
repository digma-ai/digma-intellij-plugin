package org.digma.intellij.plugin.persistence

data class PersistenceData(
        var currentEnv: String? = null,
        var isWorkspaceOnly: Boolean = false,
        var isAutoOtel: Boolean = false,
        var alreadyPassedTheInstallationWizardForIdeaIDE: Boolean = false,
        var alreadyPassedTheInstallationWizardForRiderIDE: Boolean = false,
        var alreadyPassedTheInstallationWizardForPyCharmIDE: Boolean = false,
        var firstTimeConnectionEstablished: Boolean = false,
        var firstWizardLaunch: Boolean = true,
        var firstTimeInsightReceived: Boolean = false,
        var firstTimeAssetsReceived: Boolean = false,
        var firstTimeRecentActivityReceived: Boolean = false,
        var userEmail: String? = null,
        var isLocalEngineInstalled: Boolean = false,
)
