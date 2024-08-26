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
import kotlin.time.Duration.Companion.minutes

@Service(Service.Level.APP)
class UserActivationService : DisposableAdaptor {

    //todo: ar some point we need to stop supporting backwards compatibility and remove
    // all code that calls setFirstAssetsReceivedOld,setFirstInsightReceivedOld and setFirstRecentActivityReceivedOld
    // it can be done when most users will update to backend version 0.3.76
    // this class was developed in August 2024, so probably in few months we can remove the backwards compatibility.
    // check in posthog how many users still use backend lower then 0.3.76


    private val logger = Logger.getInstance(UserActivationService::class.java)

    private var backendVersionIs0376OrHigher: Boolean? = null

    //userActivationStatus will be updated from server data,
    //or from calls to setFirstAssetsReceivedOld,setFirstInsightReceivedOld and setFirstRecentActivityReceivedOld if server is older than 0.3.76
    private val userActivationStatus: UserActivationStatus = UserActivationStatus(
        PersistenceService.getInstance().isFirstAssetsReceived(),
        PersistenceService.getInstance().isFirstInsightReceived(),
        PersistenceService.getInstance().isFirstRecentActivityReceived()
    )


    init {
        startMonitoring()
    }


    companion object {
        @JvmStatic
        fun getInstance(): UserActivationService {
            return service<UserActivationService>()
        }
    }


    private fun startMonitoring() {

        if (userActivationStatus.isDone()) {
            Log.log(logger::trace, "user activation completed, no executing monitoring task")
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
                    updateServerVersionFlag(about)
                    if (backendVersionIs0376OrHigher == true) {
                        Log.log(logger::trace, "updating user activation status from server {}", about.applicationVersion)
                        val discoveredDataResponse = AnalyticsService.getInstance(project).discoveredData
                        updateStatus(discoveredDataResponse)
                    }
                }

                if (userActivationStatus.isDone()) {
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

    private fun updateStatus(discoveredDataResponse: DiscoveredDataResponse) {
        //usually findActiveProject() will find a project. but if we can't find an active project don't set the flags
        // because we will not be able to send to posthog, it will happen next time.
        if (discoveredDataResponse.recentActivityFound) {
            findActiveProject()?.let { setFirstRecentActivityReceived(it) }
        }
        if (discoveredDataResponse.assetFound) {
            findActiveProject()?.let { setFirstAssetsReceived(it) }
        }
        if (discoveredDataResponse.issueFound) {
            findActiveProject()?.let { setFirstInsightReceived(it) }
        }
    }

    private fun updateServerVersionFlag(about: AboutResult) {
        val currentServerVersion = ComparableVersion(about.applicationVersion)
        val featureServerVersion = ComparableVersion("0.3.76")
        backendVersionIs0376OrHigher = currentServerVersion.newerThan(featureServerVersion) ||
                currentServerVersion == featureServerVersion
    }


    fun setFirstAssetsReceivedOld(project: Project) {
        if (backendVersionIs0376OrHigher == true) {
            return
        }
        setFirstAssetsReceived(project)
    }

    private fun setFirstAssetsReceived(project: Project) {
        userActivationStatus.assetFound = true
        PersistenceService.getInstance().setFirstAssetsReceived()
        ActivityMonitor.getInstance(project).registerFirstAssetsReceived()
    }

    fun isFirstAssetsReceived(): Boolean {
        return userActivationStatus.assetFound
    }


    fun setFirstInsightReceivedOld(project: Project) {
        if (backendVersionIs0376OrHigher == true) {
            return
        }
        setFirstInsightReceived(project)
    }

    private fun setFirstInsightReceived(project: Project) {
        userActivationStatus.issueFound = true
        PersistenceService.getInstance().setFirstInsightReceived()
        ActivityMonitor.getInstance(project).registerFirstInsightReceived()
    }

    fun isFirstInsightReceived(): Boolean {
        return userActivationStatus.issueFound
    }


    fun setFirstRecentActivityReceivedOld(project: Project) {
        if (backendVersionIs0376OrHigher == true) {
            return
        }
        setFirstRecentActivityReceived(project)
    }

    private fun setFirstRecentActivityReceived(project: Project) {
        userActivationStatus.recentActivityFound = true
        PersistenceService.getInstance().setFirstRecentActivityReceived()
        ActivityMonitor.getInstance(project).registerFirstTimeRecentActivityReceived()
    }

    fun isFirstRecentActivityReceived(): Boolean {
        return userActivationStatus.recentActivityFound
    }

}


private class UserActivationStatus(
    var assetFound: Boolean = false,
    var issueFound: Boolean = false,
    var recentActivityFound: Boolean = false
) {
    fun isDone(): Boolean {
        return assetFound && issueFound && recentActivityFound
    }
}
