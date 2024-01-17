package org.digma.intellij.plugin.persistence

import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.OptionTag
import java.time.Instant


internal data class PersistenceData(
    var currentEnv: String? = null,
    var isWorkspaceOnly: Boolean = false,
    var isAutoOtel: Boolean = true,
    var alreadyPassedTheInstallationWizardForIdeaIDE: Boolean = false,
    var alreadyPassedTheInstallationWizardForRiderIDE: Boolean = false,
    var alreadyPassedTheInstallationWizardForPyCharmIDE: Boolean = false,
    var firstWizardLaunch: Boolean = true,


    var userEmail: String? = null,
    var userRegistrationEmail: String? = null,
    var isLocalEngineInstalled: Boolean? = null,
    var userId: String? = null,
    var lastInsightsEventTime: String? = null,
    var noInsightsYetNotificationPassed: Boolean = false,
    var notificationsStartDate: String? = null,

    var pendingEnvironment: String? = null,
    var selectedServices: MutableMap<String, Array<String>> = mutableMapOf(),

    //todo: remove firstTimeAssetsReceived in May 2024
    var firstTimeAssetsReceived: Boolean = false,
    var firstAsset: FirstTime? = null,

    //todo: remove firstTimeConnectionEstablished and firstTimeConnectionEstablishedTimestamp in May 2024
    var firstTimeConnectionEstablished: Boolean = false,
    var firstTimeConnectionEstablishedTimestamp: String? = null,
    var firstConnection: FirstTime? = null,

    //todo: remove firstTimeInsightReceived in May 2024
    var firstTimeInsightReceived: Boolean = false,
    var firstInsight: FirstTime? = null,

    //todo: remove firstTimeRecentActivityReceived in May 2024
    var firstTimeRecentActivityReceived: Boolean = false,
    var firstRecentActivity: FirstTime? = null,

    //todo: remove isFirstTimePluginLoaded in May 2024
    var isFirstTimePluginLoaded: Boolean = false,
    var firstTimeLoaded: FirstTime? = null,

    //todo: remove firstTimePerformanceMetrics in May 2024
    var firstTimePerformanceMetrics: Boolean = false,
    var firstPerformanceMetrics: FirstTime? = null,

    )


data class FirstTime(
    @OptionTag(converter = InstantConverter::class)
    var timestamp: Instant = Instant.now(),
) {
    val firstTime: Boolean = true
}


internal class InstantConverter : Converter<Instant>() {

    override fun toString(value: Instant): String {
        val epochMillis = value.toEpochMilli()
        return epochMillis.toString()
    }

    override fun fromString(value: String): Instant {
        val epochMillis = value.toLong()
        return Instant.ofEpochMilli(epochMillis)
    }
}
