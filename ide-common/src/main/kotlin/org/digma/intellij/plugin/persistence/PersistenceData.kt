package org.digma.intellij.plugin.persistence

import com.intellij.util.xmlb.annotations.OptionTag
import java.time.Instant


internal data class PersistenceData(


    //todo: some flags where initially Booleans, after a while we needed the timestamp too.
    // so a timestamp is added and should replace the Boolean, no need to a boolean instead check the timestamp for
    // null. these booleans can be removed after some time when all users already updated. probably waiting two months
    // is enough. for new users only the timestamp will be used.


    var currentEnv: String? = null,
    var isWorkspaceOnly: Boolean = false,
    //todo: we want to change the name to isObservabilityEnabled.
    // remove isAutoOtel after some versions, can remove in June 2024 when probably all users updated the plugin
    var isAutoOtel: Boolean = true,
    var isObservabilityEnabled: Boolean = true,
    var alreadyPassedTheInstallationWizardForIdeaIDE: Boolean = false,
    var alreadyPassedTheInstallationWizardForRiderIDE: Boolean = false,
    var alreadyPassedTheInstallationWizardForPyCharmIDE: Boolean = false,
    var firstWizardLaunch: Boolean = true,


    var userEmail: String? = null,
    var userRegistrationEmail: String? = null,
    var isLocalEngineInstalled: Boolean? = null,
    var userId: String? = null,

    //lastInsightsEventTime is different from other timestamps in that it needs to be same format and
    // timezone as in the backend
    var lastInsightsEventTime: String? = null,

    var noInsightsYetNotificationPassed: Boolean = false,

    //notificationsStartDate is different from other timestamps in that it needs to be same format and
    // timezone as in the backend
    var notificationsStartDate: String? = null,

    var pendingEnvironment: String? = null,
    var selectedServices: MutableMap<String, Array<String>> = mutableMapOf(),

    //todo: remove firstTimeAssetsReceived in May 2024
    var firstTimeAssetsReceived: Boolean = false,
    @OptionTag(converter = InstantConverter::class)
    var firstTimeAssetsReceivedTimestamp: Instant? = null,

    //todo: remove firstTimeConnectionEstablished and firstTimeConnectionEstablishedTimestamp in May 2024
    // and rename firstTimeConnectionEstablishedTimestampNew to firstTimeConnectionEstablishedTimestamp
    var firstTimeConnectionEstablished: Boolean = false,
    var firstTimeConnectionEstablishedTimestamp: String? = null,
    @OptionTag(converter = InstantConverter::class)
    var firstTimeConnectionEstablishedTimestampNew: Instant? = null,

    //todo: remove firstTimeInsightReceived in May 2024
    var firstTimeInsightReceived: Boolean = false,
    @OptionTag(converter = InstantConverter::class)
    var firstTimeInsightReceivedTimestamp: Instant? = null,

    //todo: remove firstTimeRecentActivityReceived in May 2024
    var firstTimeRecentActivityReceived: Boolean = false,
    @OptionTag(converter = InstantConverter::class)
    var firstTimeRecentActivityReceivedTimestamp: Instant? = null,

    //todo: remove isFirstTimePluginLoaded in May 2024
    var isFirstTimePluginLoaded: Boolean = false,
    @OptionTag(converter = InstantConverter::class)
    var firstTimePluginLoadedTimestamp: Instant? = null,

    //todo: remove firstTimePerformanceMetrics in May 2024
    var firstTimePerformanceMetrics: Boolean = false,
    @OptionTag(converter = InstantConverter::class)
    var firstTimePerformanceMetricsTimestamp: Instant? = null,

    @OptionTag(converter = InstantConverter::class)
    var lastConnectionTimestamp: Instant? = null,

    @OptionTag(converter = InstantConverter::class)
    var lastUserActionTimestamp: Instant? = null,

    )
