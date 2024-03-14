package org.digma.intellij.plugin.persistence

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import org.digma.intellij.plugin.common.DatesUtils
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
     * @see org.digma.intellij.plugin.env.Env.Companion.getCurrentEnvName
     */
    fun getCurrentEnv(): String? {
        return state.currentEnv
    }

    fun setCurrentEnv(env: String?) {
        state.currentEnv = env
    }


    fun firstWizardLaunchDone() {
        state.firstWizardLaunch = false
    }

    fun isFirstWizardLaunch(): Boolean {
        return state.firstWizardLaunch
    }

    fun isLocalEngineInstalled(): Boolean? {
        return state.isLocalEngineInstalled
    }

    fun setLocalEngineInstalled(isInstalled: Boolean) {
        state.isLocalEngineInstalled = isInstalled
    }

    fun setSelectedServices(projectName: String, services: Array<String>?) {
        if (services == null) {
            if (state.selectedServices.containsKey(projectName)) {
                state.selectedServices.remove(projectName)
            }
        } else {
            state.selectedServices[projectName] = services
        }
    }


    fun hasEmail(): Boolean {
        return state.userEmail != null || state.userRegistrationEmail != null
    }

    fun isFirstTimeAssetsReceived(): Boolean {
        //todo: backwards compatibility, remove state.firstTimeAssetsReceived on May 2024
        if (state.firstTimeAssetsReceived && state.firstTimeAssetsReceivedTimestamp == null) {
            state.firstTimeAssetsReceivedTimestamp = Instant.now()
        }

        return state.firstTimeAssetsReceivedTimestamp != null
    }

    fun setFirstTimeAssetsReceived() {
        state.firstTimeAssetsReceivedTimestamp = Instant.now()
    }

    fun getFirstTimeAssetsReceivedTimestamp(): Instant? {
        return state.firstTimeAssetsReceivedTimestamp
    }

    fun isFirstTimePluginLoaded(): Boolean {
        //todo: backwards compatibility, remove state.isFirstTimePluginLoaded on May 2024
        if (state.isFirstTimePluginLoaded && state.firstTimePluginLoadedTimestamp == null) {
            state.firstTimePluginLoadedTimestamp = Instant.now()
        }

        return state.firstTimePluginLoadedTimestamp != null
    }

    fun setFirstTimePluginLoaded() {
        state.firstTimePluginLoadedTimestamp = Instant.now()
    }

    fun getFirstTimePluginLoadedTimestamp(): Instant? {
        return state.firstTimePluginLoadedTimestamp
    }

    fun isFirstTimePerformanceMetrics(): Boolean {
        //todo: backwards compatibility, remove state.firstTimePerformanceMetrics on May 2024
        if (state.firstTimePerformanceMetrics && state.firstTimePerformanceMetricsTimestamp == null) {
            state.firstTimePerformanceMetricsTimestamp = Instant.now()
        }

        return state.firstTimePerformanceMetricsTimestamp != null
    }

    fun setFirstTimePerformanceMetrics() {
        state.firstTimePerformanceMetricsTimestamp = Instant.now()
    }


    fun isFirstTimeInsightReceived(): Boolean {
        //todo: backwards compatibility, remove state.firstTimeInsightReceived on May 2024
        if (state.firstTimeInsightReceived && state.firstTimeInsightReceivedTimestamp == null) {
            state.firstTimeInsightReceivedTimestamp = Instant.now()
        }

        return state.firstTimeInsightReceivedTimestamp != null
    }

    fun setFirstTimeInsightReceived() {
        state.firstTimeInsightReceivedTimestamp = Instant.now()
    }


    fun isFirstTimeRecentActivityReceived(): Boolean {
        //todo: backwards compatibility, remove state.firstTimeRecentActivityReceived on May 2024
        if (state.firstTimeRecentActivityReceived && state.firstTimeRecentActivityReceivedTimestamp == null) {
            state.firstTimeRecentActivityReceivedTimestamp = Instant.now()
        }

        return state.firstTimeRecentActivityReceivedTimestamp != null
    }

    fun setFirstTimeRecentActivityReceived() {
        state.firstTimeRecentActivityReceivedTimestamp = Instant.now()
    }


    fun isWorkspaceOnly(): Boolean {
        return state.isWorkspaceOnly
    }

    fun setWorkspaceOnly(isWorkspaceOnly: Boolean) {
        state.isWorkspaceOnly = isWorkspaceOnly
    }

    fun isObservabilityEnabled(): Boolean {
        return state.isObservabilityEnabled
    }

    fun setObservabilityEnabled(isObservabilityEnabled: Boolean) {
        state.isObservabilityEnabled = isObservabilityEnabled
        //todo: remove isAutoOtel on June 2024
        state.isAutoOtel = isObservabilityEnabled
    }

    fun isAlreadyPassedTheInstallationWizardForIdeaIDE(): Boolean {
        return state.alreadyPassedTheInstallationWizardForIdeaIDE
    }

    fun setAlreadyPassedTheInstallationWizardForIdeaIDE() {
        state.alreadyPassedTheInstallationWizardForIdeaIDE = true
    }

    fun isAlreadyPassedTheInstallationWizardForRiderIDE(): Boolean {
        return state.alreadyPassedTheInstallationWizardForRiderIDE
    }

    fun setAlreadyPassedTheInstallationWizardForRiderIDE() {
        state.alreadyPassedTheInstallationWizardForRiderIDE = true
    }

    fun isAlreadyPassedTheInstallationWizardForPyCharmIDE(): Boolean {
        return state.alreadyPassedTheInstallationWizardForPyCharmIDE
    }

    fun setAlreadyPassedTheInstallationWizardForPyCharmIDE() {
        state.alreadyPassedTheInstallationWizardForPyCharmIDE = true
    }


    fun isFirstTimeConnectionEstablished(): Boolean {
        //todo: backwards compatibility, remove state.firstTimeConnectionEstablished and
        // state.firstTimeConnectionEstablishedTimestamp on May 2024
        if (state.firstTimeConnectionEstablished && state.firstTimeConnectionEstablishedTimestampNew == null) {
            val instant = state.firstTimeConnectionEstablishedTimestamp?.let {
                DatesUtils.Instants.stringToInstant(it)
            } ?: Instant.now()
            state.firstTimeConnectionEstablishedTimestampNew = instant
        }

        return state.firstTimeConnectionEstablishedTimestampNew != null
    }

    fun setFirstTimeConnectionEstablished() {
        state.firstTimeConnectionEstablishedTimestampNew = Instant.now()
    }

    fun getFirstTimeConnectionEstablishedTimestamp(): Instant? {
        return state.firstTimeConnectionEstablishedTimestampNew
    }


    fun getUserEmail(): String? {
        return state.userEmail
    }

    fun setUserEmail(userEmail: String?) {
        state.userEmail = userEmail
    }


    fun getUserRegistrationEmail(): String? {
        return state.userRegistrationEmail
    }

    fun setUserRegistrationEmail(userRegistrationEmail: String) {
        state.userRegistrationEmail = userRegistrationEmail
    }


    fun getUserId(): String? {
        return state.userId
    }

    fun setUserId(userId: String) {
        state.userId = userId
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


    fun getNotificationsStartDate(): String? {
        return state.notificationsStartDate
    }

    fun setNotificationsStartDate(notificationsStartDate: String) {
        state.notificationsStartDate = notificationsStartDate
    }


    fun getPendingEnvironment(): String? {
        return state.pendingEnvironment
    }

    fun setPendingEnvironment(pendingEnvironment: String) {
        state.pendingEnvironment = pendingEnvironment
    }


    fun getSelectedServices(): MutableMap<String, Array<String>> {
        return state.selectedServices
    }

    fun setSelectedServices(selectedServices: MutableMap<String, Array<String>>) {
        state.selectedServices = selectedServices
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



}
