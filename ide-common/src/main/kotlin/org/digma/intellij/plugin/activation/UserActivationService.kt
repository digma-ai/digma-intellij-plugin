package org.digma.intellij.plugin.activation

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.apache.maven.artifact.versioning.ComparableVersion
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.common.newerThan
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.activation.DiscoveredDataResponse
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.minutes

@Service(Service.Level.APP)
class UserActivationService : DisposableAdaptor {

    //todo: open specific UI tab from notifications

    private val logger = Logger.getInstance(UserActivationService::class.java)

    private val startedMonitoringLock = ReentrantLock(true)

    private var monitoringStarted = false

    private var backendActivationCompleted = false

    private var mainToolWindowOpen = mutableMapOf<String, Boolean>()
    private var recentActivityToolWindowOpen = mutableMapOf<String, Boolean>()

    private var userSawIssues = false
    private var userSawAssets = false
    private var userSawInsights = false
    private var userSawRecentActivity = false


    init {
        startMonitoring()

    }


    companion object {
        @JvmStatic
        fun getInstance(): UserActivationService {
            return service<UserActivationService>()
        }
    }

    //applyNewActivationLogic will be called only once in the lifetime of the plugin after new installation.
    //this is the indication to apply the new login only to new installations of the plugin version that contains this login.
    //the logic will not be applied to users that installed previous versions of the plugin.
    //there may be a race condition between the call to startMonitoring in the init and the call to
    // applyNewActivationLogic. So startMonitoring needs a lock to make sure we don't start the task twice.
    fun applyNewActivationLogic() {
        PersistenceService.getInstance().setApplyNewActivationLogic()
        startMonitoring()
    }


    private fun startMonitoring() {
        if (PersistenceService.getInstance().isApplyNewActivationLogic()) {
            startedMonitoringLock.withLock {
                if (!monitoringStarted) {
                    monitoringStarted = true
                    startMonitoringImpl()
                }
            }
        }
    }

    private fun startMonitoringImpl() {

        if (isBackendUserActivationComplete()) {
            Log.log(logger::trace, "user activation completed, not executing monitoring task")
            return
        }

        val disposable = Disposer.newDisposable()
        Disposer.register(this, disposable)
        disposable.disposingPeriodicTask(
            "UserActivationService.monitorUserActivation",
            1.minutes.inWholeMilliseconds,
            5.minutes.inWholeMilliseconds,
            true
        ) {

            try {
                Log.log(logger::trace, "checking user activation")
                //a project just to get a reference to some services
                val project = findActiveProject()
                if (project != null) {
                    val about = AnalyticsService.getInstance(project).about
                    Log.log(logger::trace, "server version {}", about.applicationVersion)
                    if (isBackendVersion03114OrHigher(about)) {
                        Log.log(logger::trace, "updating user activation status from server {}", about.applicationVersion)
                        val discoveredDataResponse = AnalyticsService.getInstance(project).discoveredData
                        Log.log(logger::trace, "backend discovered data {}", discoveredDataResponse)
                        updateBackendStatus(discoveredDataResponse)
                    }
                }

                if (isBackendUserActivationComplete()) {
                    Log.log(logger::trace, "user activation completed, canceling task")
                    Disposer.dispose(disposable)
                }

            } catch (e: Throwable) {
                if (!ExceptionUtils.isAnyConnectionException(e)) {
                    Log.warnWithException(logger, e, "error in startMonitoring {}", e)
                    ErrorReporter.getInstance().reportError("UserActivationService.startMonitoring", e)
                }
            }
        }
    }

    private fun isBackendUserActivationComplete(): Boolean {
        //after completed check one boolean instead of 5
        if (backendActivationCompleted) {
            return true
        }
        backendActivationCompleted =
            isBackendRecentActivityFound() && isBackendAssetFound() && isBackendIssueFound() && isBackendImportantIssueFound() && isBackendInsightFound()
        return backendActivationCompleted
    }


    private fun updateBackendStatus(discoveredDataResponse: DiscoveredDataResponse) {
        //usually findActiveProject() will find a project. but if we can't find an active project don't set the flags
        // because we will not be able to send to posthog, it will happen next time.
        if (discoveredDataResponse.importantIssueFound) {
            findActiveProject()?.let { setBackendImportantIssueFound(it) }
        }
        if (discoveredDataResponse.issueFound) {
            findActiveProject()?.let { setBackendIssueFound(it) }
        }
        if (discoveredDataResponse.insightFound) {
            findActiveProject()?.let { setBackendInsightFound(it) }
        }
        if (discoveredDataResponse.assetFound) {
            findActiveProject()?.let { setBackendAssetFound(it) }
        }
        if (discoveredDataResponse.recentActivityFound) {
            findActiveProject()?.let { setBackendRecentActivityFound(it) }
        }

        showNotification()

    }


    private fun showNotification() {

        if (shouldShowNewImportantIssueNotification()) {
            Log.log(logger::trace, "showing New Important Issue notification")
            showNewIssueNotification("New Important Issue", "first important issue notification link click")
            PersistenceService.getInstance().setAlreadyShowedNewImportantIssueNotification()
        } else if (shouldShowNewIssueNotification()) {
            Log.log(logger::trace, "showing New Issue notification")
            showNewIssueNotification("New Issue", "first issue notification link click")
            PersistenceService.getInstance().setAlreadyShowedNewIssueNotification()
        } else if (shouldShowNewInsightNotification()) {
            Log.log(logger::trace, "showing New Insight notification")
            showNewInsightNotification("New Insight", "first insight notification link click")
            PersistenceService.getInstance().setAlreadyShowedNewInsightNotification()
        } else if (shouldShowNewAssetNotification()) {
            Log.log(logger::trace, "showing New Asset notification")
            showNewAssetNotification("New Asset", "first asset notification link click")
            PersistenceService.getInstance().setAlreadyShowedNewAssetNotification()
        } else if (shouldShowNewRecentActivityNotification()) {
            Log.log(logger::trace, "showing New Recent Activity notification")
            showNewRecentActivityNotification("New Recent Activity", "first recent activity notification link click")
            PersistenceService.getInstance().setAlreadyShowedNewRecentActivityNotification()
        }
    }


    private fun shouldShowNewImportantIssueNotification(): Boolean {

        if (PersistenceService.getInstance().isAlreadyShowedNewImportantIssueNotification()) {
            Log.log(logger::trace, "not showing NewImportantIssueNotification because already shown")
            return false
        }

        if (!isBackendImportantIssueFound()) {
            Log.log(logger::trace, "not showing NewImportantIssueNotification because backend data not found")
            return false
        }

        if (userSawIssues) {
            Log.log(logger::trace, "not showing NewImportantIssueNotification because userSawIssues")
            return false
        }

        return true
    }

    private fun shouldShowNewIssueNotification(): Boolean {

        if (dontNeedToShowNewIssueNotification()) {
            Log.log(logger::trace, "not showing NewIssueNotification because already shown or higher priority shown")
            return false
        }

        if (!isBackendIssueFound()) {
            Log.log(logger::trace, "not showing NewIssueNotification because backend data not found")
            return false
        }

        if (userSawIssues) {
            Log.log(logger::trace, "not showing NewIssueNotification because userSawIssues")
            return false
        }

        return true
    }

    private fun dontNeedToShowNewIssueNotification(): Boolean {
        return PersistenceService.getInstance().isAlreadyShowedNewIssueNotification() ||
                PersistenceService.getInstance().isAlreadyShowedNewImportantIssueNotification()
    }

    private fun shouldShowNewInsightNotification(): Boolean {

        if (dontNeedToShowNewInsightNotification()) {
            Log.log(logger::trace, "not showing NewInsightNotification because already shown or higher priority shown")
            return false
        }

        if (!isBackendInsightFound()) {
            Log.log(logger::trace, "not showing NewInsightNotification because backend data not found")
            return false
        }

        if (userSawInsights) {
            Log.log(logger::trace, "not showing NewInsightNotification because userSawInsights")
            return false
        }

        return true
    }

    private fun dontNeedToShowNewInsightNotification(): Boolean {
        return PersistenceService.getInstance().isAlreadyShowedNewIssueNotification() ||
                PersistenceService.getInstance().isAlreadyShowedNewImportantIssueNotification() ||
                PersistenceService.getInstance().isAlreadyShowedNewInsightNotification()
    }

    private fun shouldShowNewAssetNotification(): Boolean {
        if (dontNeedToShowNewAssetNotification()) {
            Log.log(logger::trace, "not showing NewAssetNotification because already shown or higher priority shown")
            return false
        }

        if (!isBackendAssetFound()) {
            Log.log(logger::trace, "not showing NewAssetNotification because backend data not found")
            return false
        }

        if (userSawAssets) {
            Log.log(logger::trace, "not showing NewAssetNotification because userSawAssets")
            return false
        }

        return true
    }

    private fun dontNeedToShowNewAssetNotification(): Boolean {
        return PersistenceService.getInstance().isAlreadyShowedNewIssueNotification() ||
                PersistenceService.getInstance().isAlreadyShowedNewImportantIssueNotification() ||
                PersistenceService.getInstance().isAlreadyShowedNewInsightNotification() ||
                PersistenceService.getInstance().isAlreadyShowedNewAssetNotification()
    }

    private fun shouldShowNewRecentActivityNotification(): Boolean {
        if (dontNeedToShowNewRecentActivityNotification()) {
            Log.log(logger::trace, "not showing NewRecentActivityNotification because already shown or higher priority shown")
            return false
        }

        if (!isBackendRecentActivityFound()) {
            Log.log(logger::trace, "not showing NewRecentActivityNotification because backend data not found")
            return false
        }

        if (userSawRecentActivity) {
            Log.log(logger::trace, "not showing NewRecentActivityNotification because userSawRecentActivity")
            return false
        }

        return true
    }

    private fun dontNeedToShowNewRecentActivityNotification(): Boolean {
        return PersistenceService.getInstance().isAlreadyShowedNewIssueNotification() ||
                PersistenceService.getInstance().isAlreadyShowedNewImportantIssueNotification() ||
                PersistenceService.getInstance().isAlreadyShowedNewInsightNotification() ||
                PersistenceService.getInstance().isAlreadyShowedNewAssetNotification() ||
                PersistenceService.getInstance().isAlreadyShowedNewRecentActivityNotification()
    }


    private fun isBackendVersion03114OrHigher(about: AboutResult): Boolean {
        if (about.applicationVersion == "unknown") {
            return true
        }
        val currentServerVersion = ComparableVersion(about.applicationVersion)
        val featureServerVersion = ComparableVersion("0.3.114")
        return currentServerVersion.newerThan(featureServerVersion) ||
                currentServerVersion == featureServerVersion
    }


    private fun setBackendRecentActivityFound(project: Project) {
        Log.log(logger::trace, "backend recent activity found")
        setBackendDataFound(project)
        if (!isBackendRecentActivityFound()) {
            PersistenceService.getInstance().setBackendRecentActivityFound()
            ActivityMonitor.getInstance(project).registerBackendRecentActivityFound()
        }
    }

    fun isBackendRecentActivityFound(): Boolean {
        return PersistenceService.getInstance().isBackendRecentActivityFound()
    }

    private fun setBackendAssetFound(project: Project) {
        Log.log(logger::trace, "backend asset found")
        setBackendDataFound(project)
        if (!isBackendAssetFound()) {
            PersistenceService.getInstance().setBackendAssetFound()
            ActivityMonitor.getInstance(project).registerBackendAssetFound()
        }
    }

    fun isBackendAssetFound(): Boolean {
        return PersistenceService.getInstance().isBackendAssetFound()
    }

    private fun setBackendInsightFound(project: Project) {
        Log.log(logger::trace, "backend insight found")
        setBackendDataFound(project)
        if (!isBackendInsightFound()) {
            PersistenceService.getInstance().setBackendInsightFound()
            ActivityMonitor.getInstance(project).registerBackendInsightFound()
        }
    }

    fun isBackendInsightFound(): Boolean {
        return PersistenceService.getInstance().isBackendInsightFound()
    }

    private fun setBackendIssueFound(project: Project) {
        Log.log(logger::trace, "backend issue found")
        setBackendDataFound(project)
        if (!isBackendIssueFound()) {
            PersistenceService.getInstance().setBackendIssueFound()
            ActivityMonitor.getInstance(project).registerBackendIssueFound()
        }
    }

    fun isBackendIssueFound(): Boolean {
        return PersistenceService.getInstance().isBackendIssueFound()
    }

    private fun setBackendImportantIssueFound(project: Project) {
        Log.log(logger::trace, "backend important issue found")
        setBackendDataFound(project)
        if (!isBackendImportantIssueFound()) {
            PersistenceService.getInstance().setBackendImportantIssueFound()
            ActivityMonitor.getInstance(project).registerBackendImportantIssueFound()
        }
    }

    fun isBackendImportantIssueFound(): Boolean {
        return PersistenceService.getInstance().isBackendImportantIssueFound()
    }

    private fun setBackendDataFound(project: Project) {
        if (!isBackendDataFound()) {
            PersistenceService.getInstance().setBackendDataFound()
            ActivityMonitor.getInstance(project).registerBackendDataFound()
        }
    }

    private fun isBackendDataFound(): Boolean {
        return PersistenceService.getInstance().isBackendDataFound()
    }


    fun setFirstIssueReceived(project: Project) {
        Log.log(logger::trace, "plugin first issue received")
        issuesReceivedInProject(project)
        setFirstDataReceived(project)
        if (!isFirstIssueReceived()) {
            PersistenceService.getInstance().setFirstIssueReceived()
            ActivityMonitor.getInstance(project).registerFirstIssueReceived()
        }
    }

    fun isFirstIssueReceived(): Boolean {
        return PersistenceService.getInstance().isFirstIssueReceived()
    }

    /*
      this method comes to help figure out if user saw issues.
      this method is called every time we got issues in a project. after the first issues received
      this method will be called every time without checking the number of issues,
      it's not necessary because first issues already received.
      if we got issues and the tool window in that project is opened, and the active project is the same project,
      we assume user saw the issues.
      this method will be called every time issues are refreshed, so if user switches windows the method will
      be called maximum after 10 seconds if the issues tab is open.
      So we will catch the time that user saw the issues.
      it will not work if user is now viewing another application and will never switch back to Idea
      and shut down the computer.
      the same thing for assets and insights.
     */
    fun issuesReceivedInProject(project: Project) {

        //not interesting. user activation completed, not going to show notifications anymore
        if (isBackendUserActivationComplete()) {
            return
        }

        //not interesting anymore
        if (userSawIssues || dontNeedToShowNewIssueNotification()) {
            return
        }

        if (mainToolWindowOpen[project.name] == true && findActiveProject() == project) {
            Log.log(logger::trace, "marking userSawIssues in project {}", project.name)
            userSawIssues = true
            //if we decide that user saw the issues then also mark the new issue notification as already shown
            // so that we don't show it after restart
            PersistenceService.getInstance().setAlreadyShowedNewIssueNotification()
        }
    }

    fun setFirstAssetsReceived(project: Project) {
        Log.log(logger::trace, "plugin first asset received")
        assetsReceivedInProject(project)
        setFirstDataReceived(project)
        if (!isFirstAssetsReceived()) {
            PersistenceService.getInstance().setFirstAssetsReceived()
            ActivityMonitor.getInstance(project).registerFirstAssetsReceived()
        }
    }

    fun isFirstAssetsReceived(): Boolean {
        return PersistenceService.getInstance().isFirstAssetsReceived()
    }

    fun assetsReceivedInProject(project: Project) {

        //not interesting. user activation completed, not going to show notifications anymore
        if (isBackendUserActivationComplete()) {
            return
        }

        //not interesting anymore
        if (userSawAssets || dontNeedToShowNewAssetNotification()) {
            return
        }

        if (mainToolWindowOpen[project.name] == true && findActiveProject() == project) {
            Log.log(logger::trace, "marking userSawAssets in project {}", project.name)
            userSawAssets = true
            //if we decide that user saw the assets then also mark the new asset notification as already shown
            // so that we don't show it after restart
            PersistenceService.getInstance().setAlreadyShowedNewAssetNotification()

        }
    }

    fun setFirstInsightReceived(project: Project) {
        Log.log(logger::trace, "plugin first insight received")
        insightsReceivedInProject(project)
        setFirstDataReceived(project)
        if (!isFirstInsightReceived()) {
            PersistenceService.getInstance().setFirstInsightReceived()
            ActivityMonitor.getInstance(project).registerFirstInsightReceived()
        }
    }

    fun isFirstInsightReceived(): Boolean {
        return PersistenceService.getInstance().isFirstInsightReceived()
    }

    fun insightsReceivedInProject(project: Project) {

        //not interesting. user activation completed, not going to show notifications anymore
        if (isBackendUserActivationComplete()) {
            return
        }

        //not interesting anymore
        if (userSawInsights || dontNeedToShowNewInsightNotification()) {
            return
        }

        if (mainToolWindowOpen[project.name] == true && findActiveProject() == project) {
            Log.log(logger::trace, "marking userSawInsights in project {}", project.name)
            userSawInsights = true
            //if we decide that user saw the insights then also mark the new insight notification as already shown
            // so that we don't show it after restart
            PersistenceService.getInstance().setAlreadyShowedNewInsightNotification()
        }
    }


    fun setFirstRecentActivityReceived(project: Project) {
        Log.log(logger::trace, "plugin first recent activity received")
        recentActivityReceivedInProject(project)
        setFirstDataReceived(project)
        if (!isFirstRecentActivityReceived()) {
            PersistenceService.getInstance().setFirstRecentActivityReceived()
            ActivityMonitor.getInstance(project).registerFirstTimeRecentActivityReceived()
        }
    }

    fun isFirstRecentActivityReceived(): Boolean {
        return PersistenceService.getInstance().isFirstRecentActivityReceived()
    }

    fun recentActivityReceivedInProject(project: Project) {

        //not interesting. user activation completed, not going to show notifications anymore
        if (isBackendUserActivationComplete()) {
            return
        }

        //not interesting anymore
        if (userSawRecentActivity || dontNeedToShowNewRecentActivityNotification()) {
            return
        }

        if (recentActivityToolWindowOpen[project.name] == true && findActiveProject() == project) {
            Log.log(logger::trace, "marking userSawRecentActivity in project {}", project.name)
            userSawRecentActivity = true
            //if we decide that user saw recent activities then also mark the new recent activity notification as already shown
            // so that we don't show it after restart
            PersistenceService.getInstance().setAlreadyShowedNewRecentActivityNotification()
        }
    }


    private fun setFirstDataReceived(project: Project) {
        if (!isFirstDataReceived()) {
            PersistenceService.getInstance().setFirstDataReceived()
            ActivityMonitor.getInstance(project).registerFirstTimeDataReceived()
        }
    }

    private fun isFirstDataReceived(): Boolean {
        return PersistenceService.getInstance().isFirstDataReceived()
    }


    fun isAnyUsageReported(): Boolean {
        return isBackendDataFound() || isFirstDataReceived()
    }

    fun mainToolWindowShown(project: Project) {
        //not interesting. user activation completed, not going to show notifications anymore
        if (isBackendUserActivationComplete()) {
            return
        }

        mainToolWindowOpen[project.name] = true
    }

    fun mainToolWindowHidden(project: Project) {
        //not interesting. user activation completed, not going to show notifications anymore
        if (isBackendUserActivationComplete()) {
            return
        }

        mainToolWindowOpen[project.name] = false
    }

    fun recentActivityToolWindowShown(project: Project) {
        //not interesting. user activation completed, not going to show notifications anymore
        if (isBackendUserActivationComplete()) {
            return
        }

        recentActivityToolWindowOpen[project.name] = true
    }

    fun recentActivityToolWindowHidden(project: Project) {
        //not interesting. user activation completed, not going to show notifications anymore
        if (isBackendUserActivationComplete()) {
            return
        }

        recentActivityToolWindowOpen[project.name] = false
    }

}