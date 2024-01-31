package org.digma.intellij.plugin.updates

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.maven.artifact.versioning.ComparableVersion
import org.digma.intellij.plugin.analytics.AnalyticsProviderException
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.newerThan
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.model.rest.version.BackendVersionResponse
import org.digma.intellij.plugin.model.rest.version.PluginVersionResponse
import org.digma.intellij.plugin.model.rest.version.VersionRequest
import org.digma.intellij.plugin.model.rest.version.VersionResponse
import org.digma.intellij.plugin.semanticversion.SemanticVersionUtil
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.VisibleForTesting
import java.time.LocalDateTime
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class UpdatesService(private val project: Project) : Disposable {

    companion object {
        private val logger = Logger.getInstance(UpdatesService::class.java)

        @JvmStatic
        fun getInstance(project: Project): UpdatesService {
            return project.getService(UpdatesService::class.java)
        }
    }

    private val BlackoutDurationSeconds =
        TimeUnit.MINUTES.toSeconds(5) // production value
//        TimeUnit.SECONDS.toSeconds(12) // use short period (few seconds) when debugging

    // delay for first check for update since startup
    private val DelayMilliseconds = TimeUnit.SECONDS.toMillis(0)

    private val PeriodMilliseconds =
        TimeUnit.MINUTES.toMillis(5) // production value is 5 minutes
//        TimeUnit.SECONDS.toMillis(12) // use short period (few seconds) when debugging

    private val timer = Timer()

    var affectedPanel: DigmaResettablePanel? = null // late init

    private var blackoutStopTime: LocalDateTime = LocalDateTime.now().minusMonths(3)

    private var prevBackendErrorsList: List<String> = emptyList()
    private var stateBackendVersion: BackendVersionResponse
    private var statePluginVersion: PluginVersion

    init {
        stateBackendVersion = BackendVersionResponse(false, "0.0.1", "0.0.1", BackendDeploymentType.Unknown)
        statePluginVersion = PluginVersion(getPluginVersion())

        val fetchTask = object : TimerTask() {
            override fun run() {
                try {
                    checkForNewerVersions()
                } catch (e: Exception) {
                    Log.warnWithException(logger, e, "Exception in checkForNewerVersions")
                    ErrorReporter.getInstance().reportError(project, "UpdatesService.checkForNewerVersions", e)
                }
            }
        }

        timer.schedule(
            fetchTask, DelayMilliseconds, PeriodMilliseconds
        )
    }

    override fun dispose() {
        timer.cancel()
    }

    fun checkForNewerVersions() {

        Log.log(logger::debug,"checking for new versions")

        val backendConnectionMonitor = BackendConnectionMonitor.getInstance(project)
        if (backendConnectionMonitor.isConnectionError()) {
            return
        }

        val analyticsService = AnalyticsService.getInstance(project)

        var versionsResp: VersionResponse? = null
        try {
            versionsResp = analyticsService.getVersions(buildVersionRequest())
            Log.log(logger::debug,"got version response {}",versionsResp)
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
/*
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
*/
        stateBackendVersion = versionsResp.backend
        statePluginVersion.latestVersion = versionsResp.plugin.latestVersion

        EDT.ensureEDT {
            affectedPanel?.reset()
        }
    }

    @VisibleForTesting
    protected fun createResponseToInduceBackendUpdate(): VersionResponse {
        val pluginVersionResp = PluginVersionResponse(false, "0.0.1")
        val backendVersionResp = BackendVersionResponse(
            true, "0.0.2", "0.0.1",
            BackendDeploymentType.Unknown // cannot determine the deployment type - it will fallback to DockerExtension - as required
        )
        val resp = VersionResponse(pluginVersionResp, backendVersionResp, emptyList())
        return resp
    }

    fun updateButtonClicked() {
        // start blackout time that update-state won't be displayed
        blackoutStopTime = LocalDateTime.now().plusSeconds(BlackoutDurationSeconds)

        // give some time for the user/system to make the desired update, and only then recheck for newer version
        @Suppress("UnstableApiUsage")
        disposingScope().launch {
            delay(TimeUnit.SECONDS.toMillis(BlackoutDurationSeconds) + 500)

            checkForNewerVersions()
        }
    }

    fun evalAndGetState(): UpdateState {
        Log.log(logger::debug, "evalAndGetState called")
        return UpdateState(
            stateBackendVersion.deploymentType,
            shouldUpdateBackend(),
            shouldUpdatePlugin(),
        )
    }

    @VisibleForTesting
    protected fun isDuringBlackout(): Boolean {
        val now = LocalDateTime.now()
        return now < blackoutStopTime
    }

    @VisibleForTesting
    protected fun shouldUpdateBackend(): Boolean {
        if (isDuringBlackout()) return false

        var hasNewVersion = evalHasNewerVersion(stateBackendVersion)
//        hasNewVersion = true // use const only when debugging
        return hasNewVersion
    }

    @VisibleForTesting
    protected fun shouldUpdatePlugin(): Boolean {
        if (isDuringBlackout()) return false

        var hasNewVersion = evalHasNewerVersion(statePluginVersion)
//        hasNewVersion = true // use const only when debugging
        return hasNewVersion
    }

    @VisibleForTesting
    protected fun evalHasNewerVersion(backend: BackendVersionResponse): Boolean {
        val currCompVersion = ComparableVersion(backend.currentVersion)
        val latestCompVersion = ComparableVersion(backend.latestVersion)
        return latestCompVersion.newerThan(currCompVersion)
    }

    @VisibleForTesting
    protected fun evalHasNewerVersion(plugin: PluginVersion): Boolean {
        val currCompVersion = ComparableVersion(plugin.currentVersion)
        val latestCompVersion = ComparableVersion(plugin.latestVersion)
        return latestCompVersion.newerThan(currCompVersion)
    }

    @NotNull
    private fun buildVersionRequest(): VersionRequest {
        return VersionRequest(
            getPluginVersion(), getPlatformType(), getPlatformVersion()
        )
    }

    // returns one of:
    // IC - Intellij Community
    // RD - Rider
    @NotNull
    fun getPlatformType(): String {
        val appInfo = ApplicationInfo.getInstance()
        return appInfo.build.productCode
    }

    @NotNull
    fun getPlatformVersion(): String {
        val appInfo = ApplicationInfo.getInstance()
        return appInfo.fullVersion
    }

    // when plugin is not installed it will return 0.0.0
    private fun getPluginVersion(): String {
        return SemanticVersionUtil.getPluginVersionWithoutBuildNumberAndPreRelease("0.0.0")
    }
}

data class PluginVersion(val currentVersion: String) {
    var latestVersion: String? = ""

    init {
        latestVersion = currentVersion
    }
}
