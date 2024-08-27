package org.digma.intellij.plugin.persistence

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.time.Instant


@Service(Service.Level.APP)
class PersistenceService {


    companion object {
        @JvmStatic
        fun getInstance(): PersistenceService {
            return service<PersistenceService>()
        }
    }

    //the PersistenceState should be hidden from plugin code
    private val state = service<PersistenceState>().state


    /**
     * Do not use this method to get the current environment.
     * It is used only for restoring the current env after restart or after connection lost/gained
     */
    fun getLatestSelectedEnvId(): String? {
        return state.latestSelectedEnvId
    }

    fun setLatestSelectedEnvId(envId: String?) {
        state.latestSelectedEnvId = envId
    }


    fun firstWizardLaunchDone() {
        state.firstWizardLaunch = false
    }

    fun isFirstWizardLaunch(): Boolean {
        return state.firstWizardLaunch
    }

    fun isLocalEngineInstalled(): Boolean {
        return state.isLocalEngineInstalled ?: false
    }

    fun setLocalEngineInstalled(isInstalled: Boolean) {
        state.isLocalEngineInstalled = isInstalled
    }

    fun hasEmail(): Boolean {
        return state.userEmail != null || state.userRegistrationEmail != null
    }

    fun isFirstAssetsReceived(): Boolean {
        return state.firstTimeAssetsReceivedTimestamp != null
    }

    fun setFirstAssetsReceived() {
        state.firstTimeAssetsReceivedTimestamp = Instant.now()
    }

    fun getFirstTimeAssetsReceivedTimestamp(): Instant? {
        return state.firstTimeAssetsReceivedTimestamp
    }

    fun isFirstTimePluginLoaded(): Boolean {
        return state.firstTimePluginLoadedTimestamp == null
    }

    fun setFirstTimePluginLoadedDone() {
        state.firstTimePluginLoadedTimestamp = Instant.now()
    }

    fun getFirstTimePluginLoadedTimestamp(): Instant? {
        return state.firstTimePluginLoadedTimestamp
    }

    fun isFirstTimePerformanceMetrics(): Boolean {
        return state.firstTimePerformanceMetricsTimestamp == null
    }

    fun setFirstTimePerformanceMetricsDone() {
        state.firstTimePerformanceMetricsTimestamp = Instant.now()
    }


    fun isFirstInsightReceived(): Boolean {
        return state.firstTimeInsightReceivedTimestamp != null
    }

    fun setFirstInsightReceived() {
        state.firstTimeInsightReceivedTimestamp = Instant.now()
    }


    fun isFirstRecentActivityReceived(): Boolean {
        return state.firstTimeRecentActivityReceivedTimestamp != null
    }

    fun setFirstRecentActivityReceived() {
        state.firstTimeRecentActivityReceivedTimestamp = Instant.now()
    }


    fun isObservabilityEnabled(): Boolean {
        return state.isObservabilityEnabled
    }

    fun setObservabilityEnabled(isObservabilityEnabled: Boolean) {
        state.isObservabilityEnabled = isObservabilityEnabled
    }


    fun isFirstTimeConnectionEstablished(): Boolean {
        return state.firstTimeConnectionEstablishedTimestamp != null
    }

    fun setFirstTimeConnectionEstablished() {
        state.firstTimeConnectionEstablishedTimestamp = Instant.now()
    }

    fun getFirstTimeConnectionEstablishedTimestamp(): Instant? {
        return state.firstTimeConnectionEstablishedTimestamp
    }


    fun getUserEmail(): String? {
        return state.userEmail
    }

    fun setUserEmail(userEmail: String?) {
        state.userEmail = userEmail
    }

    fun nullifyUserEmail() {
        state.userEmail = null
    }


    fun getUserRegistrationEmail(): String? {
        return state.userRegistrationEmail
    }

    fun setUserRegistrationEmail(userRegistrationEmail: String) {
        state.userRegistrationEmail = userRegistrationEmail
    }

    fun nullifyUserRegistrationEmail() {
        state.userRegistrationEmail = null
    }

    fun getUserId(): String? {
        return state.userId
    }

    fun setUserId(userId: String) {
        state.userId = userId
    }

    fun nullifyUserId() {
        state.userId = null
    }

    fun getLastInsightsEventTime(): String? {
        return state.lastInsightsEventTime
    }

    fun setLastInsightsEventTime(lastInsightsEventTime: String) {
        state.lastInsightsEventTime = lastInsightsEventTime
    }

    fun isNoInsightsYetNotificationPassed(): Boolean {
        return state.noInsightsYetNotificationPassed
    }

    fun setNoInsightsYetNotificationPassed() {
        state.noInsightsYetNotificationPassed = true
    }

    fun updateLastConnectionTimestamp() {
        state.lastConnectionTimestamp = Instant.now()
    }

    fun getLastConnectionTimestamp(): Instant? {
        return state.lastConnectionTimestamp
    }

    fun setLastUserActionTimestamp(): Instant {
        val now = Instant.now()
        state.lastUserActionTimestamp = now
        return now
    }

    fun getLastUserActionTimestamp(): Instant? {
        return state.lastUserActionTimestamp
    }


    fun setEnvironmentAddedTimestamp() {
        state.environmentAddedTimestamp = Instant.now()
    }

    fun getEnvironmentAddedTimestamp(): Instant? {
        return state.environmentAddedTimestamp
    }

    fun isEnvironmentAdded(): Boolean {
        return state.environmentAddedTimestamp != null
    }

    fun setJiraFieldCopiedTimestamp() {
        state.jiraFieldCopiedTimestamp = Instant.now()
    }

    fun getJiraFieldCopiedTimestamp(): Instant? {
        return state.jiraFieldCopiedTimestamp
    }

    fun isJiraFieldCopied(): Boolean {
        return state.jiraFieldCopiedTimestamp != null
    }

    fun setLoadWarningAppearedTimestamp() {
        state.loadWarningAppearedTimestamp = Instant.now()
    }

    fun getLoadWarningAppearedTimestamp(): Instant? {
        return state.loadWarningAppearedTimestamp
    }

    fun isLoadWarningAppeared(): Boolean {
        return state.loadWarningAppearedTimestamp != null
    }

    fun setFinishDigmathonGameForUser(isFinished: Boolean) {
        state.isFinishDigmathonGameForUser = isFinished
    }

    fun isFinishDigmathonGameForUser(): Boolean {
        return state.isFinishDigmathonGameForUser
    }

    //nullable so it can be reset every time a new digmathon starts
    fun setDigmathonInsightsViewed(insights: String?) {
        state.digmathonViewedInsights = insights
    }

    fun getDigmathonInsightsViewed(): String? {
        return state.digmathonViewedInsights
    }

    fun setDigmathonInsightsViewedLastUpdated(instant: Instant?) {
        state.digmathonViewedInsightsLastUpdated = instant
    }

    fun getDigmathonInsightsViewedLastUpdated(): Instant? {
        return state.digmathonViewedInsightsLastUpdated
    }

    fun isDigmathonStartedForUser(): Boolean {
        return state.digmathonStartedForUser
    }

    fun setDigmathonStartedForUser(started: Boolean) {
        state.digmathonStartedForUser = started
    }

    fun setRecentActivityFound() {
        state.firstTimeRecentActivityFoundTimestamp = Instant.now()
    }

    fun isRecentActivityFound(): Boolean {
        return state.firstTimeRecentActivityFoundTimestamp != null
    }

    fun setAssetFound() {
        state.firstTimeAssetFoundTimestamp = Instant.now()
    }

    fun isAssetFound(): Boolean {
        return state.firstTimeAssetFoundTimestamp != null
    }

    fun setIssueFound() {
        state.firstTimeIssueFoundTimestamp = Instant.now()
    }

    fun isIssueFound(): Boolean {
        return state.firstTimeIssueFoundTimestamp != null
    }

    fun setApplyNewActivationLogic() {
        state.applyNewActivationLogic = true
    }

    fun isApplyNewActivationLogic(): Boolean {
        return state.applyNewActivationLogic
    }


}
