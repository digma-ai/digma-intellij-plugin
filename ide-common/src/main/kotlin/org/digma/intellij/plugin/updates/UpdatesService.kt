package org.digma.intellij.plugin.updates

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.apache.maven.artifact.versioning.ComparableVersion
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.ApiClientChangedEvent
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.common.buildVersionRequest
import org.digma.intellij.plugin.common.getPluginVersion
import org.digma.intellij.plugin.common.newerThan
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.model.rest.version.BackendVersionResponse
import org.digma.intellij.plugin.model.rest.version.VersionResponse
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.scheduling.oneShotTask
import org.digma.intellij.plugin.settings.InternalFileSettings
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.digma.intellij.plugin.updates.ui.UIVersioningService
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.PROJECT)
class UpdatesService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(UpdatesService::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): UpdatesService {
            return project.service<UpdatesService>()
        }

        fun getDefaultDelayBetweenUpdatesSeconds(): Duration {
            return InternalFileSettings.getUpdateServiceMonitorDelaySeconds(300).seconds
        }
    }

    var affectedPanel: DigmaResettablePanel? = null // late init

    private var prevBackendErrorsList: List<String> = emptyList()
    private var stateBackendVersion: BackendVersionResponse
    private var statePluginVersion: PluginVersion

    init {
        stateBackendVersion = BackendVersionResponse(false, "0.0.1", "0.0.1", BackendDeploymentType.Unknown)
        statePluginVersion = PluginVersion(getPluginVersion())

        val delayMillis = getDefaultDelayBetweenUpdatesSeconds().inWholeMilliseconds
        disposingPeriodicTask("UpdatesService.checkForNewerVersions", delayMillis, true) {
            try {
                Log.log(logger::trace, "updating state")
                checkForNewerVersions()
            } catch (e: Throwable) {
                Log.debugWithException(logger, e, "Exception in checkForNewerVersions {}", ExceptionUtils.getNonEmptyMessage(e))
                ErrorReporter.getInstance().reportError(project, "UpdatesService.timer", e)
            }
        }


        project.messageBus.connect(this)
            .subscribe(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, object : AnalyticsServiceConnectionEvent {
                override fun connectionLost() {
                }

                override fun connectionGained() {
                    Log.log(logger::debug, "got connectionGained")
                    oneShotTask("UpdatesService.apiClientChanged") {
                        try {
                            //update state immediately after connectionGained, so it will not wait the delay for checking the versions.
                            checkForNewerVersions()
                        } catch (e: Throwable) {
                            Log.debugWithException(logger, e, "Exception in checkForNewerVersions {}", ExceptionUtils.getNonEmptyMessage(e))
                            ErrorReporter.getInstance().reportError(project, "UpdatesService.connectionGained", e)
                        }
                    }
                }
            })



        project.messageBus.connect(this)
            .subscribe(ApiClientChangedEvent.API_CLIENT_CHANGED_TOPIC, ApiClientChangedEvent {
                oneShotTask("UpdatesService.apiClientChanged") {
                    try {
                        checkForNewerVersions()
                    } catch (e: Throwable) {
                        Log.debugWithException(logger, e, "Exception in checkForNewerVersions {}", ExceptionUtils.getNonEmptyMessage(e))
                        ErrorReporter.getInstance().reportError(project, "UpdatesService.settingsChanged", e)
                    }
                }
            })



        project.messageBus.connect().subscribe(
            AggressiveUpdateStateChangedEvent.UPDATE_STATE_CHANGED_TOPIC, object : AggressiveUpdateStateChangedEvent {
                override fun stateChanged(updateState: PublicUpdateState) {
                    oneShotTask("UpdatesService.aggressiveUpdateStateChanged") {
                        try {
                            //UpdatesService and AggressiveUpdateService work independently.
                            //it may happen that UpdatesService will change the panel to visible just before AggressiveUpdateService
                            // enters update mode. when AggressiveUpdateService exits update mode it may take a while before UpdatesService
                            // will hide the update panel, so user will see it.
                            //acting on stateChanged when AggressiveUpdateService change to update mode OK will hide the
                            // update panel immediately.
                            if (updateState.updateState == CurrentUpdateState.OK) {
                                checkForNewerVersions()
                            }
                        } catch (e: Throwable) {
                            Log.debugWithException(logger, e, "Exception in checkForNewerVersions {}", ExceptionUtils.getNonEmptyMessage(e))
                            ErrorReporter.getInstance().reportError(project, "UpdatesService.stateChanged", e)
                        }
                    }
                }
            })

    }

    override fun dispose() {
        //nothing to do , used as parent disposable
    }

    //this method may throw exception, always catch and report
    private fun checkForNewerVersions() {

        Log.log(logger::trace, "checking for new versions")
        val versionsResp: VersionResponse = AnalyticsService.getInstance(project).getVersions(buildVersionRequest())
        Log.log(logger::debug, "got version response {}", versionsResp)

        if (versionsResp.errors.isNotEmpty()) {
            val currErrors = versionsResp.errors.toList()

            if (currErrors != prevBackendErrorsList) {
                currErrors.forEach {
                    ErrorReporter.getInstance().reportBackendError(project, "UpdatesService.checkForNewerVersions", it)
                }
            }

            prevBackendErrorsList = currErrors
            return
        }

        stateBackendVersion = versionsResp.backend
        statePluginVersion.latestVersion = versionsResp.plugin.latestVersion

        //the panel is going to show the update button if shouldUpdatePlugin is true.
        //when user clicks the button we will open the intellij plugins settings.
        //sometimes the plugin list is not refreshed and user will not be able to update the plugin,
        // so we refresh plugins metadata before showing the button. waiting maximum 10 seconds for
        // the refresh to complete, and show the button anyway.
        if (shouldUpdatePlugin()) {
            //refreshPluginsMetadata returns a future that doesn't throw exception from get.
            val future = refreshPluginsMetadata()
            future.get(10, TimeUnit.SECONDS)
        }

        EDT.ensureEDT {
            affectedPanel?.reset()
        }
    }


    fun evalAndGetState(): UpdateState {
        Log.log(logger::debug, "evalAndGetState called")
        val state = UpdateState(
            stateBackendVersion.deploymentType,
            shouldUpdateBackend(),
            shouldUpdatePlugin(),
            UIVersioningService.getInstance().isNewUIBundleAvailable()
        )
        Log.log(logger::debug, "current state is {}", state)
        return state
    }

    private fun shouldUpdateBackend(): Boolean {
        return evalHasNewerVersion(stateBackendVersion)
    }

    private fun shouldUpdatePlugin(): Boolean {
        return evalHasNewerVersion(statePluginVersion)
    }


    private fun evalHasNewerVersion(backend: BackendVersionResponse): Boolean {
        val currCompVersion = ComparableVersion(backend.currentVersion)
        val latestCompVersion = ComparableVersion(backend.latestVersion)
        return latestCompVersion.newerThan(currCompVersion)
    }

    private fun evalHasNewerVersion(plugin: PluginVersion): Boolean {
        val currCompVersion = ComparableVersion(plugin.currentVersion)
        val latestCompVersion = ComparableVersion(plugin.latestVersion)
        return latestCompVersion.newerThan(currCompVersion)
    }

}

data class PluginVersion(val currentVersion: String) {
    var latestVersion: String? = ""

    init {
        latestVersion = currentVersion
    }
}
