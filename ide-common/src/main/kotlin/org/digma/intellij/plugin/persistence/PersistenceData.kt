package org.digma.intellij.plugin.persistence

import com.intellij.util.xmlb.annotations.OptionTag
import java.time.Instant


internal data class PersistenceData(

    var latestSelectedEnvId: String? = null,
    var isObservabilityEnabled: Boolean = true,
    var firstWizardLaunch: Boolean = true,
    var userEmail: String? = null,
    var userRegistrationEmail: String? = null,
    var isLocalEngineInstalled: Boolean? = null,
    var userId: String? = null,

    //lastInsightsEventTime is different from other timestamps in that it needs to be same format and
    // timezone as in the backend
    var lastInsightsEventTime: String? = null,

    var noInsightsYetNotificationPassed: Boolean = false,

    @OptionTag(converter = InstantConverter::class)
    var firstTimeAssetsReceivedTimestamp: Instant? = null,

    //todo: backwards compatibility, remove firstTimeConnectionEstablishedTimestampNew in January 2025
    // renamed without the new suffix
    @OptionTag(converter = InstantConverter::class)
    var firstTimeConnectionEstablishedTimestampNew: Instant? = null,
    @OptionTag(converter = InstantConverter::class)
    var firstTimeConnectionEstablishedTimestamp: Instant? = null,

    @OptionTag(converter = InstantConverter::class)
    var firstTimeInsightReceivedTimestamp: Instant? = null,

    @OptionTag(converter = InstantConverter::class)
    var firstTimeRecentActivityReceivedTimestamp: Instant? = null,

    @OptionTag(converter = InstantConverter::class)
    var firstTimePluginLoadedTimestamp: Instant? = null,

    @OptionTag(converter = InstantConverter::class)
    var firstTimePerformanceMetricsTimestamp: Instant? = null,

    @OptionTag(converter = InstantConverter::class)
    var lastConnectionTimestamp: Instant? = null,

    @OptionTag(converter = InstantConverter::class)
    var lastUserActionTimestamp: Instant? = null,

    @OptionTag(converter = InstantConverter::class)
    var environmentAddedTimestamp: Instant? = null,

    @OptionTag(converter = InstantConverter::class)
    var jiraFieldCopiedTimestamp: Instant? = null,

    @OptionTag(converter = InstantConverter::class)
    var loadWarningAppearedTimestamp: Instant? = null,

    var isFinishDigmathonGameForUser: Boolean = false,
    var digmathonViewedInsights: String? = null,

    @OptionTag(converter = InstantConverter::class)
    var digmathonViewedInsightsLastUpdated: Instant? = null,
    var digmathonStartedForUser: Boolean = false

)
