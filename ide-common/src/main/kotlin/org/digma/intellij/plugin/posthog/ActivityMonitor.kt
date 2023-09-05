package org.digma.intellij.plugin.posthog

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.posthog.java.PostHog
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.common.UserId
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.model.rest.version.PerformanceMetricsResponse
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.semanticversion.SemanticVersionUtil
import org.threeten.extra.Hours
import java.io.PrintWriter
import java.io.StringWriter
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime


@Service(Service.Level.PROJECT)
class ActivityMonitor(project: Project) : Disposable {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ActivityMonitor {
            return project.getService(ActivityMonitor::class.java)
        }
    }

    private val userId: String = UserId.userId
    private val isDevUser: Boolean = UserId.isDevUser
    private val latestUnknownRunConfigTasks = mutableMapOf<String, Instant>()

    private var postHog: PostHog? = null
    private var lastLensClick: LocalDateTime? = null
    private val lastInsightsViewed = mutableSetOf<InsightType>()
    private var lastConnectionErrorTime: Instant = Instant.MIN

    private val settingsChangeTracker = SettingsChangeTracker()

    init {

        val token = "phc_5sy6Kuv1EYJ9GAdWPeGl7gx31RAw7BR7NHnOuLCUQZK"
        postHog = PostHog.Builder(token).build()
        registerSessionDetails()

        ConnectionActivityMonitor.loadInstance(project)
        PluginActivityMonitor.loadInstance(project)

        settingsChangeTracker.start(this)

    }


    override fun dispose() {
        //nothing to do, used as parent disposable
    }


    private fun capture(eventName: String) {
        capture(eventName, mapOf())
    }

    private fun capture(eventName: String, details: Map<String, Any>) {

        val mutableDetails: MutableMap<String, Any> = mutableMapOf()
        mutableDetails.putAll(details)

        mutableDetails["firstTimeInsightReceived"] = PersistenceService.getInstance().state.firstTimeInsightReceived
        mutableDetails["firstTimeAssetsReceived"] = PersistenceService.getInstance().state.firstTimeAssetsReceived
        mutableDetails["firstTimeRecentActivityReceived"] = PersistenceService.getInstance().state.firstTimeRecentActivityReceived

        postHog?.capture(
            userId,
            eventName,
            mutableDetails
        )
    }


    fun registerFramework(framework: MonitoredFramework) {
        capture("framework detected", mapOf("framework.name" to framework.name))
        postHog?.set(
            userId, mapOf(
                "framework.last" to framework.name,
                "framework.${framework.name}.last-seen" to Instant.now().toString()
            )
        )
    }

    fun registerEmail(email: String) {
        postHog?.alias(userId, email)
        postHog?.identify(userId, mapOf("email" to email))
    }

    fun registerCustomEvent(eventName: String, tags: Map<String, Any>?) {
        capture(eventName, tags ?: mapOf())
    }

    fun registerLensClicked(lens: String) {
        lastLensClick = LocalDateTime.now()
        capture(
            "lens clicked",
            mapOf("lens" to lens)
        )
        registerUserAction("Clicked on lens")
    }

    fun registerSidePanelOpened() {
        val reason = if (lastLensClick != null && Duration.between(lastLensClick, LocalDateTime.now()).seconds < 2)
            "lens click"
        else
            "unknown"

        capture(
            "side-panel opened",
            mapOf("reason" to reason)
        )
        registerUserAction("Opened side panel")
    }

    fun registerSidePanelClosed() {
        capture("side-panel closed")
    }

    fun registerObservabilityPanelOpened() {
        capture("observability-panel opened")
        registerUserAction("Opened observability panel")
    }

    fun registerObservabilityPanelClosed() {
        capture("observability-panel closed")
    }

    fun registerFirstConnectionEstablished() {
        capture("connection first-established")
    }

    fun registerConnectionError(action: String, message: String) {
        val oneHourAgo = Instant.now().minus(Hours.of(1))
        if (lastConnectionErrorTime.isBefore(oneHourAgo)) {
            capture("connection error", mapOf("reason" to message, "action" to action))
            lastConnectionErrorTime = Instant.now()
        }
    }

    fun registerConnectionGained() {
        capture("connection gained")
    }

    fun registerConnectionLost() {
        capture("connection lost")
    }

    fun registerFirstInsightReceived() {
        capture("insight first-received")
    }

    fun registerFirstAssetsReceived() {
        capture("plugin first-assets")
    }

    fun registerFirstTimeRecentActivityReceived() {
        capture("plugin first-activity")
    }

    fun registerObservabilityOn() {
        capture("observability is turned on")
        registerUserAction("Turned on observability")
    }

    fun registerObservabilityOff() {
        capture("observability is turned off")
        registerUserAction("Turned off observability")
    }


    fun registerError(exception: Throwable, message: String) {

        //Don't call directly, use ErrorReporter.reportError

        val stringWriter = StringWriter()
        exception.printStackTrace(PrintWriter(stringWriter))

        capture(
            "error",
            mapOf(
                "error.source" to "plugin",
                "action" to "unknown",
                "message" to message,
                "exception.type" to exception.javaClass.name,
                "exception.message" to exception.message.toString(),
                "exception.stack-trace" to stringWriter.toString()
            )
        )
    }

    fun reportBackendError(message: String, action: String) {

        //Don't call directly, use ErrorReporter.reportBackendError

        capture(
            "error",
            mapOf(
                "error.source" to "backend",
                "action" to action,
                "message" to message,
            )
        )
    }

    fun reportRunConfig(runConfigTypeName: String, taskNames: Collection<String>, observabilityEnabled: Boolean, connectedToBackend: Boolean) {
        capture(
            "run config",
            mapOf(
                "run.config.type" to runConfigTypeName,
                "task.names" to taskNames,
                "observability.enabled" to observabilityEnabled,
                "backend.connected" to connectedToBackend,
            )
        )
    }

    fun reportSupportedRunConfigDetected(details: Map<String, Any>) {
        capture(
            "supported-run-configurations",
            details
        )
    }

    fun reportUnknownTaskRunning(buildSystem: String, taskNames: Collection<String>, configurationClassName: String, configurationType: String) {

        // Purge tasks older than 1 minute
        latestUnknownRunConfigTasks.entries.removeIf {
            it.value.isBefore(Instant.now().minusSeconds(60))
        }

        // Filter out tasks seen in the last 1 minute
        val taskNamesToReport = taskNames.stream()
            .filter { !latestUnknownRunConfigTasks.containsKey(it) }
            .toList()
        for (task in taskNames)
            latestUnknownRunConfigTasks[task] = Instant.now()

        if (taskNamesToReport.isEmpty())
            return

        capture(
            "unknown-config ran",
            mapOf(
                "config.build-system" to buildSystem,
                "config.tasks" to taskNamesToReport,
                "config.class-name" to configurationClassName,
                "config.type" to configurationType,
            )
        )
    }


    fun registerInsightsViewed(insightTypes: List<InsightType>) {

        val insightsTypesToRegister = mutableListOf<InsightType>()

        insightTypes.forEach {
            if (!lastInsightsViewed.contains(it)) {
                insightsTypesToRegister.add(it)
            }
        }

        lastInsightsViewed.addAll(insightTypes)

        if (insightsTypesToRegister.isEmpty()) {
            return
        }

        capture(
            "insights viewed",
            mapOf("insights" to insightsTypesToRegister)
        )
    }

    fun registerButtonClicked(panel: MonitoredPanel, button: String) {
        capture(
            "button-clicked",
            mapOf(
                "panel" to panel.name,
                "button" to button
            )
        )
        registerUserAction("Clicked on $button")
    }

    fun registerNavigationButtonClicked(navigable: Boolean) {
        capture(
            "button-clicked",
            mapOf(
                "panel" to MonitoredPanel.Scope.name,
                "button" to "NavigateToCode",
                "navigable" to navigable.toString()
            )
        )
        registerUserAction("Clicked on navigation button")
    }

    fun registerSpanLinkClicked(insight: InsightType) {
        capture(
            "span-link clicked",
            mapOf(
                "panel" to MonitoredPanel.Insights.name,
                "insight" to insight.name
            )
        )
        registerUserAction("Clicked on span link from insights")
    }

    fun registerSpanLinkClicked(panel: MonitoredPanel) {
        registerSpanLinkClicked(panel, null)
    }

    fun registerSpanLinkClicked(panel: MonitoredPanel, navigable: Boolean?) {
        capture(
            "span-link clicked",
            mapOf(
                "panel" to panel.name,
                "navigable" to (navigable?.toString() ?: "unknown")
            )
        )
        registerUserAction("Clicked on span link from ${panel.name}")
    }

    fun registerButtonClicked(button: String, insight: InsightType) {
        capture(
            "insights button-clicked",
            mapOf(
                "button" to button,
                "insight" to insight.name
            )
        )
        registerUserAction("Clicked on button from insights")
    }


    //todo: remove at some point
    fun registerFirstTimePluginLoaded() {
        postHog?.capture(userId, "plugin first-loaded")
    }

    fun registerFirstTimePluginLoadedNew() {
        postHog?.capture(userId, "plugin first-init")
    }

    fun registerPluginLoaded() {
        postHog?.capture(userId, "plugin loaded")
    }

    fun registerPluginUninstalled(): String {
        postHog?.capture(userId, "plugin uninstalled")
        return userId
    }

    fun registerPluginDisabled(): String {
        postHog?.capture(userId, "plugin disabled")
        return userId
    }

    fun registerServerInfo(serverInfo: AboutResult) {
        postHog?.set(
            userId,
            mapOf(
                "server.version" to serverInfo.applicationVersion,
                "server.deploymentType" to (serverInfo.deploymentType ?: BackendDeploymentType.Unknown)
            )
        )
    }

    fun registerContinuousPerformanceMetrics(result: PerformanceMetricsResponse) {
        val properties = mutableMapOf<String, Any>(
            "data" to result
        )
        capture("continuous-performance-metrics", properties)
    }

    fun registerPerformanceMetrics(result: PerformanceMetricsResponse) {
        val properties = mutableMapOf<String, Any>(
            "server.startTime" to result.serverStartTime,
            //"server.aliveTime" to result.serverAliveTime,
            "probeTime" to result.probeTime
        )
        for (metric in result.metrics) {
            val uncapitalizedMetric = Character.toLowerCase(metric.metric[0]) + metric.metric.substring(1);
            properties["server.metric.$uncapitalizedMetric"] = metric.value
        }

        capture("server received-data", properties)
    }


    fun registerContainerEngine(containerPlatform: String) {
        postHog?.set(
            userId,
            mapOf("user.container-engine" to containerPlatform)
        )
    }


    fun registerDigmaEngineEventStart(eventName: String, eventDetails: Map<String, Any>) {
        capture(
            "Engine.".plus(eventName).plus(".start"),
            eventDetails
        )
    }


    fun registerDigmaEngineEventEnd(eventName: String, eventDetails: Map<String, Any>) {
        capture(
            "Engine.".plus(eventName).plus(".end"),
            eventDetails
        )
    }

    fun registerDigmaEngineEventRetry(eventName: String, eventDetails: Map<String, Any>) {
        capture(
            "Engine.".plus(eventName).plus(".retry"),
            eventDetails
        )
    }

    fun registerDigmaEngineEventError(eventName: String, errorMessage: String) {
        capture(
            "Engine.".plus(eventName).plus(".error"),
            mapOf("errorMessage" to errorMessage)
        )
    }

    fun hash(message: String): String {
        val bytes = message.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }


    private fun registerSessionDetails() {
        val osType = System.getProperty("os.name")
        val ideInfo = ApplicationInfo.getInstance()
        val ideName = ideInfo.versionName
        val ideVersion = ideInfo.fullVersion
        val ideBuildNumber = ideInfo.build.asString()
        val pluginVersion = SemanticVersionUtil.getPluginVersionWithoutBuildNumberAndPreRelease("unknown")

        postHog?.set(
            userId,
            mapOf(
                "os.type" to osType,
                "ide.name" to ideName,
                "ide.version" to ideVersion,
                "ide.build" to ideBuildNumber,
                "plugin.version" to pluginVersion,
                "user.type" to if (isDevUser) "internal" else "external"
            )
        )
    }


    fun registerSettingsEvent(eventName: String, eventDetails: Map<String, Any>) {
        capture(
            "Settings.".plus(eventName),
            eventDetails
        )
    }

    fun registerNotificationCenterEvent(eventName: String, eventDetails: Map<String, Any>) {
        capture(
            "Notifications.".plus(eventName),
            eventDetails
        )
    }

    private fun registerUserAction(action: String) {
        capture(
            "user-action",
            mapOf("action" to action)
        )
    }


}