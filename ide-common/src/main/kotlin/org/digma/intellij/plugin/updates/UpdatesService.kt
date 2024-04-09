package org.digma.intellij.plugin.updates

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.maven.artifact.versioning.ComparableVersion
import org.digma.intellij.plugin.analytics.AnalyticsProviderException
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.buildVersionRequest
import org.digma.intellij.plugin.common.getPluginVersion
import org.digma.intellij.plugin.common.newerThan
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.model.rest.version.BackendVersionResponse
import org.digma.intellij.plugin.model.rest.version.PluginVersionResponse
import org.digma.intellij.plugin.model.rest.version.VersionResponse
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

@Service(Service.Level.PROJECT)
class UpdatesService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(UpdatesService::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): UpdatesService {
            return project.service<UpdatesService>()
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
            try {

                while (isActive) {
                    checkForNewerVersions()
                    delay(5.minutes.inWholeMilliseconds)
                }

            } catch (e: CancellationException) {
                Log.debugWithException(logger, e, "Exception in checkForNewerVersions")
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "Exception in checkForNewerVersions")
                ErrorReporter.getInstance().reportError("UpdatesService.timer", e)
            }
        }
    }

    override fun dispose() {
        //nothing to do , used as parent disposable
    }

    private fun checkForNewerVersions() {

        Log.log(logger::debug, "checking for new versions")

        val backendConnectionMonitor = BackendConnectionMonitor.getInstance(project)
        if (backendConnectionMonitor.isConnectionError()) {
            return
        }

        val analyticsService = AnalyticsService.getInstance(project)

        var versionsResp: VersionResponse? = null
        try {
            versionsResp = analyticsService.getVersions(buildVersionRequest())
            Log.log(logger::debug, "got version response {}", versionsResp)
        } catch (ase: AnalyticsServiceException) {
            var logException = true
            if (ase.cause is AnalyticsProviderException) {
                // addressing issue https://github.com/digma-ai/digma-intellij-plugin/issues/606
                // look if got HTTP Error Code 404 (https://en.wikipedia.org/wiki/HTTP_404), it means that backend is too old
                val ape = ase.cause as AnalyticsProviderException
                if (ape.responseCode == 404) {
                    logException = false
                    versionsResp = createResponseToInduceBackendUpdate()
                }
            }

            if (logException) {
                Log.log(logger::debug, "AnalyticsServiceException for getVersions: {}", ase.message)
                versionsResp = null
            }
        }

        if (versionsResp == null) {
            return
        }

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

    private fun createResponseToInduceBackendUpdate(): VersionResponse {
        val pluginVersionResp = PluginVersionResponse(false, "0.0.1")
        val backendVersionResp = BackendVersionResponse(
            true, "0.0.2", "0.0.1",
            BackendDeploymentType.Unknown // cannot determine the deployment type - it will fallback to DockerExtension - as required
        )
        val resp = VersionResponse(pluginVersionResp, backendVersionResp, emptyList())
        return resp
    }


    fun evalAndGetState(): UpdateState {
        Log.log(logger::debug, "evalAndGetState called")
        return UpdateState(
            stateBackendVersion.deploymentType,
            shouldUpdateBackend(),
            shouldUpdatePlugin(),
        )
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
