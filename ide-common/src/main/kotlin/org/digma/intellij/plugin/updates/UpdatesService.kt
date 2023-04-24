package org.digma.intellij.plugin.updates

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.ui.EdtInvocationManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.maven.artifact.versioning.ComparableVersion
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.newerThan
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.model.rest.version.BackendVersionResponse
import org.digma.intellij.plugin.model.rest.version.VersionRequest
import org.digma.intellij.plugin.model.rest.version.VersionResponse
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.VisibleForTesting
import java.time.LocalDateTime
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

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
    private val DelayMilliseconds = TimeUnit.SECONDS.toMillis(5)

    private val PeriodMilliseconds =
        TimeUnit.MINUTES.toMillis(5) // production value is 5 minutes
//        TimeUnit.SECONDS.toMillis(12) // use short period (few seconds) when debugging

    private val timer = Timer()

    var affectedPanel: DigmaResettablePanel? = null // late init

    private var blackoutStopTime: LocalDateTime = LocalDateTime.now().minusMonths(3)

    private var stateBackendVersion: BackendVersionResponse
    private var statePluginVersion: PluginVersion

    init {
        stateBackendVersion = BackendVersionResponse(false, "0.0.1", "0.0.1", BackendDeploymentType.Unknown)
        statePluginVersion = PluginVersion(getPluginVersion())

        val fetchTask = object : TimerTask() {
            override fun run() {
                checkForNewerVersions()
            }
        }

        timer.scheduleAtFixedRate(
            fetchTask, DelayMilliseconds, PeriodMilliseconds
        )
    }

    override fun dispose() {
        timer.cancel()
    }

    fun checkForNewerVersions() {
        val backend = BackendConnectionMonitor.getInstance(project)
        if (!backend.isConnectionOk()) {
            return
        }

        val analyticsService = AnalyticsService.getInstance(project)

        var versionsResp: VersionResponse?
        try {
            versionsResp = analyticsService.getVersions(buildVersionRequest())
        } catch (ase: AnalyticsServiceException) {
            Log.log(logger::debug, "AnalyticsServiceException for getVersions: {}", ase.message);
            versionsResp = null
        }

        if (versionsResp == null) {
            return
        }

        stateBackendVersion = versionsResp.backend
        statePluginVersion.latestVersion = versionsResp.plugin.latestVersion

        EdtInvocationManager.getInstance().invokeLater {
            affectedPanel?.reset()
        }
    }

    fun updateButtonClicked() {
        // start blackout time that update-state won't be displayed
        blackoutStopTime = LocalDateTime.now().plusSeconds(BlackoutDurationSeconds)

        // give some time for the user/system to make the desired update, and only then recheck for newer version
        GlobalScope.launch {
            delay(TimeUnit.SECONDS.toMillis(BlackoutDurationSeconds) + 500)

            checkForNewerVersions()
        }
    }

    fun evalAndGetState(): UpdateState {
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
    @NotNull
    fun getPluginVersion(): String {
        val plugin = PluginManagerCore.getPlugin(com.intellij.openapi.extensions.PluginId.getId(PluginId.PLUGIN_ID))
        if (plugin != null)
            return plugin.version

        return "0.0.0"
    }
}

data class PluginVersion(val currentVersion: String) {
    var latestVersion: String? = ""

    init {
        latestVersion = currentVersion
    }
}
