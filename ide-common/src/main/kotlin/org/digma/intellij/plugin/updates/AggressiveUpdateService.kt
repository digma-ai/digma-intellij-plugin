package org.digma.intellij.plugin.updates

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.apache.maven.artifact.versioning.ComparableVersion
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.ApiClientChangedEvent
import org.digma.intellij.plugin.analytics.BackendInfoHolder
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.common.buildVersionRequest
import org.digma.intellij.plugin.common.getPluginVersion
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.newerThan
import org.digma.intellij.plugin.common.runWIthRetry
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.model.rest.version.VersionResponse
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.settings.InternalFileSettings
import org.digma.intellij.plugin.updates.CurrentUpdateState.OK
import org.digma.intellij.plugin.updates.CurrentUpdateState.UPDATE_BACKEND
import org.digma.intellij.plugin.updates.CurrentUpdateState.UPDATE_BOTH
import org.digma.intellij.plugin.updates.CurrentUpdateState.UPDATE_PLUGIN
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class CurrentUpdateState { OK, UPDATE_BACKEND, UPDATE_PLUGIN, UPDATE_BOTH }

@Service(Service.Level.PROJECT)
class AggressiveUpdateService(val project: Project) : Disposable {

    private val logger: Logger = Logger.getInstance(AggressiveUpdateService::class.java)

    private val isConnectionLost = AtomicBoolean(false)

    private var delayBetweenUpdatesSeconds = getDefaultDelayBetweenUpdatesSeconds()

    private val updateStateLock = ReentrantLock()

    private val updateStateRef: AtomicReference<PublicUpdateState> = AtomicReference(PublicUpdateState(OK, BackendDeploymentType.Unknown))

    private var myJob: Job? = null

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

            //update state now so that the state exists as part of the object instantiation
            updateStateNow()

            startMonitoring()

            project.messageBus.connect(this)
                .subscribe(
                    AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
                    object : AnalyticsServiceConnectionEvent {
                        override fun connectionLost() {
                            Log.log(logger::debug, "got connectionLost")
                            isConnectionLost.set(true)
                            myJob?.cancel(CancellationException("connection lost"))
                        }

                        override fun connectionGained() {
                            Log.log(logger::debug, "got connectionGained")
                            isConnectionLost.set(false)

                            //startMonitoring is canceled on connection lost, resume it on connection gained.
                            //connectionGained will be invoked for every open project because AnalyticsServiceConnectionEvent is currently not
                            // a real application event. but startMonitoring is synchronized and protected against multiple threads.
                            startMonitoring()
                        }
                    })


            project.messageBus.connect(this)
                .subscribe(ApiClientChangedEvent.API_CLIENT_CHANGED_TOPIC, ApiClientChangedEvent {
                    @Suppress("UnstableApiUsage")
                    disposingScope().launch {
                        try {
                            //update state immediately after client is replaced.
                            updateState()
                        } catch (c: CancellationException) {
                            Log.debugWithException(logger, c, "apiClientChanged canceled {}", c)
                        } catch (e: Throwable) {
                            val message = ExceptionUtils.getNonEmptyMessage(e)
                            Log.debugWithException(logger, e, "error in apiClientChanged {}", message)
                            errorReporter.reportError("AggressiveUpdateService.apiClientChanged", e)
                        }

                        //and call startMonitoring just in case it is stopped by a previous connectionLost but there was no connection gained
                        startMonitoring()
                    }
                })

        } else {
            Log.log(logger::info, "not starting, disabled in internal settings")
        }
    }


    override fun dispose() {
        //nothing to do, used as parent disposable
    }

    fun getUpdateState(): PublicUpdateState {
        return updateStateRef.get()
    }

    fun isInUpdateMode(): Boolean {
        return updateStateRef.get().updateState != OK
    }


    private fun updateStateNow() {

        @Suppress("UnstableApiUsage")
        val deferred = disposingScope().async {
            try {
                Log.log(logger::trace, "loading versions and updating state on startup")
                updateState()
                Log.log(logger::trace, "updating state on startup completed successfully")
            } catch (c: CancellationException) {
                Log.debugWithException(logger, c, "updateStateNow canceled {}", c)
            } catch (e: Throwable) {
                val message = ExceptionUtils.getNonEmptyMessage(e)
                Log.debugWithException(logger, e, "error in updateStateNow {}", message)
                errorReporter.reportError("AggressiveUpdateService.updateStateNow", e)
            }
        }

        runBlocking {
            try {
                withTimeout(2000) {
                    deferred.await()
                }
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("AggressiveUpdateService.updateStateNow", e)
            }
        }
    }



    @Synchronized
    private fun startMonitoring() {

        Log.log(logger::debug, "startMonitoring called")


        if (myJob?.isActive == true) {
            Log.log(logger::debug, "startMonitoring called but already monitoring")
            return
        }

        if (!isProjectValid(project)) {
            Log.log(logger::debug, "startMonitoring called but project is not valid, not starting monitoring")
            return
        }

        @Suppress("UnstableApiUsage")
        myJob = disposingScope().launch {

            Log.log(logger::debug, "starting monitoring..")
            var failures = 0
            while (isActive) {

                try {
                    Log.log(logger::trace, "loading versions and updating state")
                    updateState()
                    failures = 0
                    Log.log(logger::trace, "sleeping {}", delayBetweenUpdatesSeconds)
                    //delay with Duration doesn't work
                    delay(delayBetweenUpdatesSeconds.inWholeMilliseconds)

                } catch (c: CancellationException) {
                    Log.debugWithException(logger, c, "startMonitoring canceled {}", c)
                } catch (e: Throwable) {
                    failures++
                    val message = ExceptionUtils.getNonEmptyMessage(e)
                    Log.debugWithException(logger, e, "error in startMonitoring {}", message)
                    errorReporter.reportError("AggressiveUpdateService.startMonitoring", e)
                    try {
                        //maybe backend is down or had timeout, wait a bit and retry
                        delay(60.seconds.inWholeMilliseconds)
                    } catch (c: CancellationException) {
                        Log.debugWithException(logger, c, "startMonitoring canceled {}", c)
                    }
                }

                //if we got 50 exceptions, cancel, something is wrong,maybe backend is down, and we didn't get connectionLost.
                // hopefully connectionGained will resume monitoring
                if (failures > 100) {
                    cancel(CancellationException("too many failures"))
                }
            }

            Log.log(logger::trace, "quiting monitoring")
        }
    }


    private fun updateState() {

        try {
            //todo: don't need the lock, only one thread is calling this method
            updateStateLock.lock()
            runWIthRetry({
                Log.log(logger::debug, "loading versions")
                val versions = buildVersions()
                Log.log(logger::debug, "loaded versions {}", versions)
                Log.log(logger::debug, "updating state")
                val prevUpdateState = updateStateRef.get().copy()
                update(versions)
                Log.log(logger::debug, "state updated. prev state: {}, new state: {}", prevUpdateState, updateStateRef)
            }, backOffMillis = 2000, maxRetries = 5)

        } finally {
            if (updateStateLock.isHeldByCurrentThread) {
                updateStateLock.unlock()
            }
        }
    }


    private fun buildVersions(): Versions {
        try {
            val versionsResponse = AnalyticsService.getInstance(project).getVersions(buildVersionRequest())
            reportVersionsErrorsIfNecessary(versionsResponse.errors)
            return Versions.fromVersionsResponse(versionsResponse)
        } catch (e: Throwable) {
            val message = ExceptionUtils.getNonEmptyMessage(e)
            Log.debugWithException(logger, e, "error in buildVersions {}", message)
            errorReporter.reportError("AggressiveUpdateService.buildVersions", e)

            //if getVersions failed try to get server version from getAbout
            val about = BackendInfoHolder.getInstance(project).getAbout()
            about?.let {
                return Versions.fromAboutResponse(it)
            } ?: throw RuntimeException("could not get backend info from getVersions or getAbout")
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
                //shorten the delay if in update mode
                delayBetweenUpdatesSeconds = 10.seconds
                Log.log(logger::trace, "changed monitoring delay to $delayBetweenUpdatesSeconds")
                ErrorReporter.pause()
            } else {
                //longer the delay when not in update mode
                delayBetweenUpdatesSeconds = getDefaultDelayBetweenUpdatesSeconds()
                Log.log(logger::trace, "setting monitoring delay back to default $delayBetweenUpdatesSeconds")
                ErrorReporter.resume()
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
