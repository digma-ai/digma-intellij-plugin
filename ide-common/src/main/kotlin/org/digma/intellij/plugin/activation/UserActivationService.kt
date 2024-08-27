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


    private val logger = Logger.getInstance(UserActivationService::class.java)

    private val startedMonitoringLock = ReentrantLock(true)

    private var monitoringStarted = false

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

        if (backendUserActivationIsComplete()) {
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
                    Log.log(logger::trace, "got server version {}", about.applicationVersion)
                    if (backendVersionIs0376OrHigher(about)) {
                        Log.log(logger::trace, "updating user activation status from server {}", about.applicationVersion)
                        val discoveredDataResponse = AnalyticsService.getInstance(project).discoveredData
                        updateBackendStatus(discoveredDataResponse)
                    }
                }

                if (backendUserActivationIsComplete()) {
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

    private fun backendUserActivationIsComplete(): Boolean {
        return isRecentActivityFound() && isAssetFound() && isIssueFound()
    }


    private fun updateBackendStatus(discoveredDataResponse: DiscoveredDataResponse) {

        val currentRecentActivityFound = isRecentActivityFound()
        val currentAssetFound = isAssetFound()
        val currentIssueFound = isIssueFound()


        //usually findActiveProject() will find a project. but if we can't find an active project don't set the flags
        // because we will not be able to send to posthog, it will happen next time.
        if (discoveredDataResponse.recentActivityFound) {
            findActiveProject()?.let { setRecentActivityFound(it) }
        }
        if (discoveredDataResponse.assetFound) {
            findActiveProject()?.let { setAssetFound(it) }
        }
        if (discoveredDataResponse.issueFound) {
            findActiveProject()?.let { setIssueFound(it) }
        }

        showNotification(currentRecentActivityFound, currentAssetFound, currentIssueFound)

    }


    private fun showNotification(prevRecentActivityFound: Boolean, prevAssetFound: Boolean, prevIssueFound: Boolean) {
        if (!prevIssueFound && isIssueFound()) {
            showNewIssueNotification()
        } else if (!prevAssetFound && isAssetFound()) {
            showNewAssetNotification()
        } else if (!prevRecentActivityFound && isRecentActivityFound()) {
            showNewRecentActivityNotification()
        }
    }


    private fun backendVersionIs0376OrHigher(about: AboutResult): Boolean {
        val currentServerVersion = ComparableVersion(about.applicationVersion)
        val featureServerVersion = ComparableVersion("0.3.76")
        return currentServerVersion.newerThan(featureServerVersion) ||
                currentServerVersion == featureServerVersion
    }


    private fun setRecentActivityFound(project: Project) {
        if (!isRecentActivityFound()) {
            PersistenceService.getInstance().setRecentActivityFound()
            ActivityMonitor.getInstance(project).registerRecentActivityFound()
        }
    }

    fun isRecentActivityFound(): Boolean {
        return PersistenceService.getInstance().isRecentActivityFound()
    }

    private fun setAssetFound(project: Project) {
        if (!isAssetFound()) {
            PersistenceService.getInstance().setAssetFound()
            ActivityMonitor.getInstance(project).registerAssetFound()
        }
    }

    fun isAssetFound(): Boolean {
        return PersistenceService.getInstance().isAssetFound()
    }

    private fun setIssueFound(project: Project) {
        if (!isIssueFound()) {
            PersistenceService.getInstance().setIssueFound()
            ActivityMonitor.getInstance(project).registerIssueFound()
        }
    }

    fun isIssueFound(): Boolean {
        return PersistenceService.getInstance().isIssueFound()
    }


    fun setFirstAssetsReceived(project: Project) {
        if (!isFirstAssetsReceived()) {
            PersistenceService.getInstance().setFirstAssetsReceived()
            ActivityMonitor.getInstance(project).registerFirstAssetsReceived()
        }
    }

    fun isFirstAssetsReceived(): Boolean {
        return PersistenceService.getInstance().isFirstAssetsReceived()
    }


    fun setFirstInsightReceived(project: Project) {
        if (!isFirstInsightReceived()) {
            PersistenceService.getInstance().setFirstInsightReceived()
            ActivityMonitor.getInstance(project).registerFirstInsightReceived()
        }
    }

    fun isFirstInsightReceived(): Boolean {
        return PersistenceService.getInstance().isFirstInsightReceived()
    }


    fun setFirstRecentActivityReceived(project: Project) {
        if (!isFirstRecentActivityReceived()) {
            PersistenceService.getInstance().setFirstRecentActivityReceived()
            ActivityMonitor.getInstance(project).registerFirstTimeRecentActivityReceived()
        }
    }

    fun isFirstRecentActivityReceived(): Boolean {
        return PersistenceService.getInstance().isFirstRecentActivityReceived()
    }
}