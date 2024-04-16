package org.digma.intellij.plugin.updates

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.maven.artifact.versioning.ComparableVersion
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.BackendConnectionEvent
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
import org.digma.intellij.plugin.settings.InternalFileSettings
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
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

        @Suppress("UnstableApiUsage")
        disposingScope().launch {

            val delaySeconds = getDefaultDelayBetweenUpdatesSeconds()

            while (isActive) {

                try {
                    Log.log(logger::trace, "updating state")
                    checkForNewerVersions()
                    Log.log(logger::trace, "sleeping {}", delaySeconds)
                    delay(delaySeconds.inWholeMilliseconds)
                } catch (e: CancellationException) {
                    Log.debugWithException(logger, e, "Exception in checkForNewerVersions")
                } catch (e: Throwable) {
                    Log.debugWithException(logger, e, "Exception in checkForNewerVersions {}", ExceptionUtils.getNonEmptyMessage(e))
                    ErrorReporter.getInstance().reportError("UpdatesService.timer", e)
                    try {
                        Log.log(logger::trace, "sleeping {}", delaySeconds)
                        delay(delaySeconds.inWholeMilliseconds)
                    } catch (_: Throwable) {
                        //ignore
                    }
                }

            }
        }


        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(BackendConnectionEvent.BACKEND_CONNECTION_STATE_TOPIC, object : BackendConnectionEvent {
                override fun connectionLost() {
                }

                override fun connectionGained() {
                    Log.log(logger::debug, "got connectionGained")
                    //update state immediately after connectionGained, so it will not wait the delay for checking the versions.
                    checkForNewerVersions()
                }
            })


        SettingsState.getInstance().addChangeListener({
            @Suppress("UnstableApiUsage")
            disposingScope().launch {
                //update state immediately after settings change. we are interested in api url change, but it will
                // do no harm to call it on any settings change
                checkForNewerVersions()
            }

        }, this)
    }

    override fun dispose() {
        //nothing to do , used as parent disposable
    }

    private fun checkForNewerVersions() {

        Log.log(logger::trace, "checking for new versions")

        var versionsResp: VersionResponse? = null
        versionsResp = AnalyticsService.getInstance(project).getVersions(buildVersionRequest())
        Log.log(logger::debug, "got version response {}", versionsResp)

        if (versionsResp.errors.isNotEmpty()) {
            val currErrors = versionsResp.errors.toList()

            if (currErrors != prevBackendErrorsList) {
                currErrors.forEach {
                    ErrorReporter.getInstance().reportBackendError(project, it, "query-for-versions-and-propose-to-update")
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
