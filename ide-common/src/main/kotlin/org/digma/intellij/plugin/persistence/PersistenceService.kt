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


    fun getCurrentEnv(): String? {
        return state.currentEnv
    }

    fun setCurrentEnv(env: String) {
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
        //todo: Jan 17 2024: state.firstTimeAssetsReceived is backwards compatibility, for users that installed before
        // it was changed to FirstTime object. after some period when all users updated and have the
        // version with FirstTime object we can remove state.firstTimeAssetsReceived
        // this condition will be true after user updated the plugin to the version with FirstTime object and has
        // state.firstTimeAssetsReceived=true
        if (state.firstTimeAssetsReceived && state.firstAsset == null) {
            state.firstAsset = FirstTime(Instant.now())
        }

        return state.firstAsset?.firstTime ?: false
    }

    fun setFirstTimeAssetsReceived() {
        state.firstAsset = FirstTime(Instant.now())
    }


    fun isFirstTimePluginLoaded(): Boolean {
        //todo: Jan 17 2024: state.isFirstTimePluginLoaded is backwards compatibility, for users that installed before
        // it was changed to FirstTime object. after some period when all users updated and have the
        // version with FirstTime object we can remove state.isFirstTimePluginLoaded
        // this condition will be true after user updated the plugin to the version with FirstTime object and has
        // state.isFirstTimePluginLoaded=true
        if (state.isFirstTimePluginLoaded && state.firstTimeLoaded == null) {
            state.firstTimeLoaded = FirstTime(Instant.now())
        }

        return state.firstTimeLoaded?.firstTime ?: false
    }

    fun setFirstTimePluginLoaded() {
        state.firstTimeLoaded = FirstTime(Instant.now())
    }

    fun isFirstTimePerformanceMetrics(): Boolean {
        //todo: Jan 17 2024: state.firstTimePerformanceMetrics is backwards compatibility, for users that installed before
        // it was changed to FirstTime object. after some period when all users updated and have the
        // version with FirstTime object we can remove state.firstTimePerformanceMetrics
        // this condition will be true after user updated the plugin to the version with FirstTime object and has
        // state.firstTimePerformanceMetrics=true
        if (state.firstTimePerformanceMetrics && state.firstPerformanceMetrics == null) {
            state.firstPerformanceMetrics = FirstTime(Instant.now())
        }

        return state.firstPerformanceMetrics?.firstTime ?: false
    }

    fun setFirstTimePerformanceMetrics() {
        state.firstPerformanceMetrics = FirstTime(Instant.now())
    }


    fun isFirstTimeInsightReceived(): Boolean {
        //todo: Jan 17 2024: state.firstTimeInsightReceived is backwards compatibility, for users that installed before
        // it was changed to FirstTime object. after some period when all users updated and have the
        // version with FirstTime object we can remove state.firstTimeInsightReceived
        // this condition will be true after user updated the plugin to the version with FirstTime object and has
        // state.firstTimeInsightReceived=true
        if (state.firstTimeInsightReceived && state.firstInsight == null) {
            state.firstInsight = FirstTime(Instant.now())
        }

        return state.firstInsight?.firstTime ?: false
    }

    fun setFirstTimeInsightReceived() {
        state.firstInsight = FirstTime(Instant.now())
    }


    fun isFirstTimeRecentActivityReceived(): Boolean {
        //todo: Jan 17 2024: state.firstTimeRecentActivityReceived is backwards compatibility, for users that installed before
        // it was changed to FirstTime object. after some period when all users updated and have the
        // version with FirstTime object we can remove state.firstTimeRecentActivityReceived
        // this condition will be true after user updated the plugin to the version with FirstTime object and has
        // state.firstTimeRecentActivityReceived=true
        if (state.firstTimeRecentActivityReceived && state.firstRecentActivity == null) {
            state.firstRecentActivity = FirstTime(Instant.now())
        }

        return state.firstRecentActivity?.firstTime ?: false
    }

    fun setFirstTimeRecentActivityReceived() {
        state.firstRecentActivity = FirstTime(Instant.now())
    }


    fun isWorkspaceOnly(): Boolean {
        return state.isWorkspaceOnly
    }

    fun setWorkspaceOnly(isWorkspaceOnly: Boolean) {
        state.isWorkspaceOnly = isWorkspaceOnly
    }

    fun isAutoOtel(): Boolean {
        return state.isAutoOtel
    }

    fun setAutoOtel(isAutoOtel: Boolean) {
        state.isAutoOtel = isAutoOtel
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
        //todo: Jan 17 2024: state.firstTimeConnectionEstablished is backwards compatibility, for users that installed before
        // it was changed to FirstTime object. after some period when all users updated and have the
        // version with FirstTime object we can remove state.firstTimeConnectionEstablished and state.firstTimeConnectionEstablishedTimestamp
        // this condition will be true after user updated the plugin to the version with FirstTime object and has
        // state.firstTimeConnectionEstablished=true
        if (state.firstTimeConnectionEstablished && state.firstConnection == null) {
            val instant = state.firstTimeConnectionEstablishedTimestamp?.let {
                DatesUtils.Instants.stringToInstant(it)
            } ?: Instant.now()
            state.firstConnection = FirstTime(instant)
        }

        return state.firstConnection?.firstTime ?: false
    }

    fun setFirstTimeConnectionEstablished() {
        state.firstConnection = FirstTime(Instant.now())
    }

    fun getFirstTimeConnectionEstablishedTimestamp(): Instant? {
        return state.firstConnection?.timestamp
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


}
