package org.digma.intellij.plugin.updates

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.apache.maven.artifact.versioning.ComparableVersion
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.ApiClientChangedEvent
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.common.buildVersionRequest
import org.digma.intellij.plugin.common.getPluginVersion
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.newerThan
import org.digma.intellij.plugin.common.runWIthRetry
import org.digma.intellij.plugin.common.runWIthRetryWithResult
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.model.rest.version.VersionResponse
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.scheduling.oneShotTask
import org.digma.intellij.plugin.settings.InternalFileSettings
import org.digma.intellij.plugin.updates.CurrentUpdateState.OK
import org.digma.intellij.plugin.updates.CurrentUpdateState.UPDATE_BACKEND
import org.digma.intellij.plugin.updates.CurrentUpdateState.UPDATE_BOTH
import org.digma.intellij.plugin.updates.CurrentUpdateState.UPDATE_PLUGIN
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class CurrentUpdateState { OK, UPDATE_BACKEND, UPDATE_PLUGIN, UPDATE_BOTH }

@Service(Service.Level.PROJECT)
class AggressiveUpdateService(val project: Project) : DisposableAdaptor {

    private val logger: Logger = Logger.getInstance(AggressiveUpdateService::class.java)

    private var delayBetweenUpdatesSeconds = getDefaultDelayBetweenUpdatesSeconds()

    private var myHighRateDisposable: Disposable? = null

    private val updateStateRef: AtomicReference<PublicUpdateState> = AtomicReference(PublicUpdateState(OK, BackendDeploymentType.Unknown))

    //take the real ErrorReporter to use here. using ErrorReporter.getInstance() may return a
    // proxy when it's paused by this service.
    private val errorReporter = service<ErrorReporter>()

    companion object {

        fun getDefaultDelayBetweenUpdatesSeconds(): Duration {
            return InternalFileSettings.getAggressiveUpdateServiceMonitorDelaySeconds(300).seconds
        }

        @JvmStatic
        fun getInstance(project: Project): AggressiveUpdateService {
            return project.service<AggressiveUpdateService>()
        }
    }


    init {

        Log.log(logger::info, "init, local settings:{}", InternalFileSettings.getAllSettingsOf("AggressiveUpdateService"))

        //if not enabled will not start monitoring and will not register listeners
        if (InternalFileSettings.getAggressiveUpdateServiceEnabled()) {

            Log.log(logger::info, "starting...")

            startMonitoring()

            project.messageBus.connect(this)
                .subscribe(
                    AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
                    object : AnalyticsServiceConnectionEvent {
                        override fun connectionLost() {
                        }

                        override fun connectionGained() {
                            Log.log(logger::debug, "got connectionGained")
                            updateStateNowInBackground()
                        }
                    })


            project.messageBus.connect(this)
                .subscribe(ApiClientChangedEvent.API_CLIENT_CHANGED_TOPIC, ApiClientChangedEvent {
                    updateStateNowInBackground()
                })

        } else {
            Log.log(logger::info, "not starting, disabled in internal settings")
        }
    }


    fun getUpdateState(): PublicUpdateState {
        return updateStateRef.get()
    }

    fun isInUpdateMode(): Boolean {
        return updateStateRef.get().updateState != OK
    }


    private fun updateStateNowInBackground() {
        oneShotTask("AggressiveUpdateService.updateStateNow") {
            update()
        }
    }


    //startMonitoring is called from init while the object is constructed.
    //it should never be called concurrently because this is a project service and the platform guards against
    // concurrent creation of services.
    private fun startMonitoring() {

        Log.log(logger::debug, "startMonitoring called")

        if (!isProjectValid(project)) {
            Log.log(logger::debug, "startMonitoring called but project is not valid, not starting monitoring")
            return
        }

        //this task is paused by the scheduler if there is no connection. It's useless to try to update without backend because there is no way to
        // know the backend version.
        //but because this service starts on project startup,and actually IDE startup, it may run once before ApiErrorHandler had the chance
        // to mark connection lost. if it will run when backend is not running there will be too many error reports in posthog,
        // so it should ignore no connection errors.
        disposingPeriodicTask("AggressiveUpdate.periodic", delayBetweenUpdatesSeconds.inWholeMilliseconds, true) {
            update()
        }
    }

    //must be called on background
    private fun update() {
        try {
            Log.log(logger::trace, "loading versions and updating state")
            updateState()
            Log.log(logger::trace, "updating state on startup completed successfully")
        } catch (e: Throwable) {
            //don't report connection errors , its useless and will report too many errors to posthog and the log
            if (!ExceptionUtils.isAnyConnectionException(e)) {
                val message = ExceptionUtils.getNonEmptyMessage(e)
                Log.debugWithException(logger, e, "error in update {}", message)
                errorReporter.reportError("AggressiveUpdateService.update", e)
            }
        }
    }


    private fun updateState() {

        //try to update with retry.
        //there may be timeouts or no connection, in any case retry few times.
        //if all retries fail this method will throw the exception

        runWIthRetry({
            Log.log(logger::debug, "loading versions")
            val versions = buildVersions()
            Log.log(logger::debug, "loaded versions {}", versions)
            Log.log(logger::debug, "updating state")
            val prevUpdateState = updateStateRef.get().copy()
            update(versions)
            Log.log(logger::debug, "state updated. prev state: {}, new state: {}", prevUpdateState, updateStateRef)
        }, backOffMillis = 1000, maxRetries = 3)
    }


    //if there is no backend running this method will throw an AnalyticsServiceException
    private fun buildVersions(): Versions {
        return try {
            //we know that getVersions may fail on timeouts so retry at least twice
            runWIthRetryWithResult({
                val versionsResponse = AnalyticsService.getInstance(project).getVersions(buildVersionRequest())
                reportVersionsErrorsIfNecessary(versionsResponse.errors)
                Versions.fromVersionsResponse(versionsResponse)
            }, backOffMillis = 1000, maxRetries = 2)
        } catch (e: Throwable) {
            //don't report connection errors , its useless and will report too many errors to posthog and the log
            if (!ExceptionUtils.isAnyConnectionException(e)) {
                val message = ExceptionUtils.getNonEmptyMessage(e)
                Log.debugWithException(logger, e, "error in buildVersions {}", message)
                errorReporter.reportError("AggressiveUpdateService.buildVersions", e)
            }

            //if getVersions failed try to get server version from getAbout.
            //if there is no connection getAbout will throw an AnalyticsServiceException
            val about = AnalyticsService.getInstance(project).about
            Versions.fromAboutResponse(about)
        }
    }


    private fun update(versions: Versions) {
        if (needsAggressiveBothUpdate(versions)) {
            Log.log(logger::debug, "needs both update {}", versions)
            doUpdateAndFireEvent(UPDATE_BOTH, versions)
        } else if (needsAggressiveBackendUpdate(versions)) {
            Log.log(logger::debug, "needs backend update {}", versions)
            doUpdateAndFireEvent(UPDATE_BACKEND, versions)
        } else if (needsAggressivePluginUpdate(versions)) {
            Log.log(logger::debug, "needs plugin update {}", versions)
            doUpdateAndFireEvent(UPDATE_PLUGIN, versions)
        } else {
            Log.log(logger::debug, "no update needed {}", versions)
            doUpdateAndFireEvent(OK, versions)
        }
    }


    private fun doUpdateAndFireEvent(updateTo: CurrentUpdateState, versions: Versions) {

        val prevState = updateStateRef.get()
        val newUpdateState = PublicUpdateState(updateTo, versions.backendDeploymentType)
        updateStateRef.set(newUpdateState)
        if (prevState.updateState != newUpdateState.updateState) {

            //the panel is going to show the update button.
            //in case when the plugin needs update, when user clicks the button we will open the intellij plugins settings.
            //sometimes the plugin list is not refreshed and user will not be able to update the plugin,
            // so we refresh plugins metadata before showing the button. waiting maximum 10 seconds for
            // the refresh to complete, and show the button anyway.
            if (listOf(UPDATE_PLUGIN, UPDATE_BOTH).any { it == updateTo }) {
                val future = refreshPluginsMetadata()
                //refreshPluginsMetadata returns a future that doesn't throw exception from get.
                future.get(10, TimeUnit.SECONDS)
            }

            registerPosthogEvent(updateTo, versions)

            Log.log(logger::debug, "state changed , firing event. prev state:{},new state:{}", prevState, newUpdateState)
            fireStateChanged()

            if (listOf(UPDATE_BACKEND, UPDATE_PLUGIN, UPDATE_BOTH).any { it == updateTo }) {
                //if in update mode start a high rate recurring update task to catch the update quickly
                Log.log(logger::trace, "entered update mode,starting high rate update")
                ErrorReporter.pause()
                //make sure its disposed
                myHighRateDisposable?.let {
                    Disposer.dispose(it)
                }

                myHighRateDisposable = Disposer.newDisposable()
                myHighRateDisposable?.let {
                    Disposer.register(this, it)
                    it.disposingPeriodicTask("AggressiveUpdate.periodicHighRate", 10.seconds.inWholeMilliseconds, true) {
                        update()
                    }
                }

            } else {
                Log.log(logger::trace, "exited update mode,stopping high rate update")
                ErrorReporter.resume()
                myHighRateDisposable?.let {
                    Disposer.dispose(it)
                }
            }

        } else {
            Log.log(logger::debug, "state has not changed , Not firing event. prev state:{},new state:{}", prevState, newUpdateState)
        }
    }


    private fun needsAggressiveBothUpdate(versions: Versions): Boolean {
        return !versions.forceBackendVersion.isNullOrBlank() && !versions.forcePluginVersion.isNullOrBlank()
    }

    private fun needsAggressivePluginUpdate(versions: Versions): Boolean {
        return !versions.forcePluginVersion.isNullOrBlank()
    }


    private fun needsAggressiveBackendUpdate(versions: Versions): Boolean {
        return needsAggressiveBackendUpdateByLocalSettings(versions) || !versions.forceBackendVersion.isNullOrBlank()
    }

    //check if we need backend update by minimal backend version from local settings file
    private fun needsAggressiveBackendUpdateByLocalSettings(versions: Versions): Boolean {

        if (versions.currentBackendVersion.isNullOrBlank() ||
            versions.minimalRequiredBackendVersionInLocalSettings.isNullOrBlank()
        ) {
            return false
        }

        val backendVersion = ComparableVersion(versions.currentBackendVersion)
        val minimalRequiredVersion = ComparableVersion(versions.minimalRequiredBackendVersionInLocalSettings)
        return minimalRequiredVersion.newerThan(backendVersion)

    }


    private data class Versions(
        //the currently running plugin version
        var currentPluginVersion: String = getPluginVersion(),
        //recommended in versions file
        var latestRecommendedPluginVersion: String? = null,
        //the version to force from VersionResponse
        var forcePluginVersion: String? = null,
        //the currently running backend version
        var currentBackendVersion: String? = null,
        //the minimal required backend version in the internal settings file
        val minimalRequiredBackendVersionInLocalSettings: String? = InternalFileSettings.getAggressiveUpdateServiceMinimalBackendVersion(),
        //recommended in versions file
        var latestRecommendedBackendVersion: String? = null,
        //the version to force from VersionResponse
        var forceBackendVersion: String? = null,
        var backendDeploymentType: BackendDeploymentType = BackendDeploymentType.Unknown
    ) {

        companion object {
            fun fromVersionsResponse(versionsResponse: VersionResponse): Versions {
                return Versions(
                    currentPluginVersion = getPluginVersion(),
                    latestRecommendedPluginVersion = versionsResponse.plugin.latestVersion,
                    forcePluginVersion = versionsResponse.forceUpdate?.minPluginVersionRequired,
                    currentBackendVersion = versionsResponse.backend.currentVersion,
                    minimalRequiredBackendVersionInLocalSettings = InternalFileSettings.getAggressiveUpdateServiceMinimalBackendVersion(),
                    latestRecommendedBackendVersion = versionsResponse.backend.latestVersion,
                    forceBackendVersion = versionsResponse.forceUpdate?.minBackendVersionRequired,
                    backendDeploymentType = versionsResponse.backend.deploymentType
                )
            }

            fun fromAboutResponse(about: AboutResult): Versions {
                return Versions(
                    currentPluginVersion = getPluginVersion(),
                    latestRecommendedPluginVersion = null,
                    forcePluginVersion = null,
                    currentBackendVersion = about.applicationVersion,
                    minimalRequiredBackendVersionInLocalSettings = InternalFileSettings.getAggressiveUpdateServiceMinimalBackendVersion(),
                    latestRecommendedBackendVersion = null,
                    forceBackendVersion = null,
                    backendDeploymentType = about.deploymentType ?: BackendDeploymentType.Unknown
                )
            }
        }
    }


    private fun fireStateChanged() {
        project.messageBus.syncPublisher(AggressiveUpdateStateChangedEvent.UPDATE_STATE_CHANGED_TOPIC)
            .stateChanged(updateStateRef.get().copy())
    }


    private fun registerPosthogEvent(currentState: CurrentUpdateState, versions: Versions) {

        //no need for posthog event if OK
        if (currentState == OK) {
            return
        }

        val details = mutableMapOf(
            "current backend version" to versions.currentBackendVersion.toString(),
            "minimal required backend version in versions file" to versions.forceBackendVersion.toString(),
            "minimal required backend version in local settings" to versions.minimalRequiredBackendVersionInLocalSettings.toString(),
            "current plugin version" to versions.currentPluginVersion,
            "minimal required plugin version in versions file" to versions.forcePluginVersion.toString(),
            "update mode" to currentState.name
        )

        Log.log(logger::info, "sending posthog event for {}", currentState)
        ActivityMonitor.getInstance(project).registerCustomEvent("ForceUpdate", details)
    }


    private fun reportVersionsErrorsIfNecessary(errors: List<String>) {
        try {
            errors.forEach {
                errorReporter.reportBackendError(null, it, "${this::class.simpleName}.getVersions")
            }
        } catch (e: Throwable) {
            errorReporter.reportError("AggressiveUpdateService.reportVersionsErrorsIfNecessary", e)
        }
    }

}


data class PublicUpdateState(val updateState: CurrentUpdateState, val backendDeploymentType: BackendDeploymentType)
