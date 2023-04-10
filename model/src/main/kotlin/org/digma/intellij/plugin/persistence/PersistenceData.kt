package org.digma.intellij.plugin.persistence

data class PersistenceData(
        var currentEnv: String? = null,
        var isWorkspaceOnly: Boolean = false,
        var isAutoOtel: Boolean = false,
        var alreadyPassedTheInstallationWizard: Boolean = false,
        var firstTimeConnectionEstablished: Boolean = false,
        var firstTimePluginLoaded: Boolean = false,
        var firstTimeInsightReceived: Boolean = false
)
