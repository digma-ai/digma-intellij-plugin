package org.digma.intellij.plugin.posthog

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.Project
import com.posthog.java.PostHog
import org.digma.intellij.plugin.common.CommonUtils
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
import java.util.UUID
import java.util.concurrent.TimeUnit


class ActivityMonitor(project: Project) /*: Runnable, Disposable*/ {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ActivityMonitor {
            return project.getService(ActivityMonitor::class.java)
        }
    }

    private val userId: String
    private val isDevUser: Boolean
    private val latestUnknownRunConfigTasks = mutableMapOf<String, Instant>()
    private var errorCache: Cache<String, String>? = null

    //    private val tokenFetcherThread = Thread(this, "Token fetcher thread")
    private var postHog: PostHog? = null
    private var lastLensClick: LocalDateTime? = null
    private var lastInsightsViewed: HashSet<InsightType>? = null
    private var lastConnectionErrorTime: Instant = Instant.MIN

    init {
        val hostname = CommonUtils.getLocalHostname()
        if (System.getenv("devenv") == "digma") {
            userId = hostname
            isDevUser = true
        } else {
            if (PersistenceService.getInstance().state.userId == null) {
                // Phase #1
                PersistenceService.getInstance().state.userId = Integer.toHexString(hostname.hashCode())
                // Phase #2 (after 14/08/2023 uncomment this phase, and comment the phase #1)
                // PersistenceService.getInstance().state.userId = UUID.randomUUID().toString()
            }
            userId = PersistenceService.getInstance().state.userId!!
            isDevUser = false
        }

//        val cachedToken = getCachedToken()
//        postHog =
//                if (cachedToken != null) PostHog.Builder(cachedToken).build()
//                else null
//
//        tokenFetcherThread.start()

        errorCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build()

        val token = "phc_5sy6Kuv1EYJ9GAdWPeGl7gx31RAw7BR7NHnOuLCUQZK"
        postHog = PostHog.Builder(token).build()
        registerSessionDetails()

        ConnectionActivityMonitor.loadInstance(project)
        PluginActivityMonitor.loadInstance(project)
        ServerPerformanceActivityMonitor.loadInstance(project)
    }

//    override fun run() {
//        val cachedToken = getCachedToken()
//        val latestToken = getLatestToken()
//        if (latestToken != null && latestToken != cachedToken) {
//            postHog = PostHog.Builder(latestToken).build()
//            setCachedToken(latestToken)
//        }
//        if (postHog != null) {
//            Log.log(LOGGER::info, "Posthog was configured successfully with " +
//                    (if (latestToken != null) "latest token" else "cached token"))
//        } else {
//            Log.log(LOGGER::info, "Posthog failed to be configured")
//        }
//        registerSessionDetails()
//    }

    fun registerFramework(framework: MonitoredFramework) {
        postHog?.capture(userId, "framework detected", mapOf("framework.name" to framework.name))
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
        postHog?.capture(userId, eventName, tags)
    }

    fun registerLensClicked(lens: String) {
        lastLensClick = LocalDateTime.now()
        postHog?.capture(
            userId,
            "lens clicked",
            mapOf("lens" to lens)
        )
    }

    fun registerSidePanelOpened() {
        val reason = if (lastLensClick != null && Duration.between(lastLensClick, LocalDateTime.now()).seconds < 2)
            "lens click"
        else
            "unknown"

        postHog?.capture(
            userId,
            "side-panel opened",
            mapOf("reason" to reason)
        )
    }

    fun registerObservabilityPanelOpened() {
        postHog?.capture(userId, "observability-panel opened")
    }

    fun registerFirstConnectionEstablished() {
        postHog?.capture(userId, "connection first-established")
    }

    fun registerConnectionError(action: String, message: String) {
        val oneHourAgo = Instant.now().minus(Hours.of(1))
        if (lastConnectionErrorTime.isBefore(oneHourAgo)) {
            postHog?.capture(userId, "connection error", mapOf("reason" to message, "action" to action))
            lastConnectionErrorTime = Instant.now()
        }
    }

    fun registerConnectionGained() {
        postHog?.capture(userId, "connection gained")
    }

    fun registerFirstInsightReceived() {
        postHog?.capture(userId, "insight first-received")
    }

    fun registerFirstAssetsReceived() {
        postHog?.capture(userId, "plugin first-assets")
    }

    fun registerFirstTimeRecentActivityReceived() {
        postHog?.capture(userId, "plugin first-activity")
    }

    fun registerObservabilityOn() {
        postHog?.capture(userId, "observability is turned on")
    }

    fun registerObservabilityOff() {
        postHog?.capture(userId, "observability is turned off")
    }

    fun registerError(exception: Exception, message: String) {
        val stringWriter = StringWriter()
        exception.printStackTrace(PrintWriter(stringWriter))

        var hash = hash(message)
        if (errorCache!!.getIfPresent(hash) != null)
            return
        errorCache!!.put(hash, hash)

        postHog?.capture(
            userId,
            "error",
            mapOf(
                "error.source" to "plugin",
                "action" to "unknown",
                "message" to message,
                "exception.type" to exception.javaClass.name,
                "exception.message" to exception.message,
                "exception.stack-trace" to stringWriter.toString()
            )
        )
    }

    fun reportBackendError(message: String, action: String) {

        val hash = hash(message)
        if (errorCache!!.getIfPresent(hash) != null)
            return
        errorCache!!.put(hash, hash)

        postHog?.capture(
            userId,
            "error",
            mapOf(
                "error.source" to "backend",
                "action" to action,
                "message" to message,
            )
        )
    }

    fun reportRunConfig(runConfigTypeName: String, taskNames: Collection<String>, observabilityEnabled: Boolean, connectedToBackend: Boolean) {
        postHog?.capture(
            userId,
            "run config",
            mapOf(
                "run.config.type" to runConfigTypeName,
                "task.names" to taskNames,
                "observability.enabled" to observabilityEnabled,
                "backend.connected" to connectedToBackend,
            )
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

        postHog?.capture(
            userId, "unknown-config ran", mapOf(
                "config.build-system" to buildSystem,
                "config.tasks" to taskNamesToReport,
                "config.class-name" to configurationClassName,
                "config.type" to configurationType,
            )
        )
    }

    fun registerInsightsViewed(insightTypes: List<out InsightType>) {
        val newInsightsViewed = HashSet(insightTypes)
        if (lastInsightsViewed != null && lastInsightsViewed == newInsightsViewed)
            return

        lastInsightsViewed = newInsightsViewed
        postHog?.capture(
            userId,
            "insights viewed",
            mapOf("insights" to insightTypes)
        )
    }

    fun registerButtonClicked(panel: MonitoredPanel, button: String) {
        postHog?.capture(
            userId,
            "button-clicked",
            mapOf(
                "panel" to panel.name,
                "button" to button
            )
        )
    }

    fun registerNavigationButtonClicked(navigable: Boolean) {
        postHog?.capture(
            userId,
            "button-clicked",
            mapOf(
                "panel" to MonitoredPanel.Scope.name,
                "button" to "NavigateToCode",
                "navigable" to navigable.toString()
            )
        )
    }

    fun registerSpanLinkClicked(insight: InsightType) {
        postHog?.capture(
            userId,
            "span-link clicked",
            mapOf(
                "panel" to MonitoredPanel.Insights.name,
                "insight" to insight.name
            )
        )
    }

    fun registerSpanLinkClicked(panel: MonitoredPanel) {
        registerSpanLinkClicked(panel, null)
    }

    fun registerSpanLinkClicked(panel: MonitoredPanel, navigable: Boolean?) {
        postHog?.capture(
            userId,
            "span-link clicked",
            mapOf(
                "panel" to panel.name,
                "navigable" to (navigable?.toString() ?: "unknown")
            )
        )
    }

    fun registerButtonClicked(button: String, insight: InsightType) {
        postHog?.capture(
            userId,
            "insights button-clicked",
            mapOf(
                "button" to button,
                "insight" to insight.name
            )
        )
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

    fun registerServerInfo(serverInfo: AboutResult) {
        postHog?.set(
            userId,
            mapOf(
                "server.version" to serverInfo.applicationVersion,
                "server.deploymentType" to (serverInfo.deploymentType ?: BackendDeploymentType.Unknown)
            )
        )
    }


    fun registerPerformanceMetrics(result: PerformanceMetricsResponse) {
        val properties = mutableMapOf<String, Any>(
            "server.startTime" to result.serverStartTime,
            //"server.aliveTime" to result.serverAliveTime,
            "probeTime" to result.probeTime
        )
        for (metric in result.metrics){
            val uncapitalizedMetric = Character.toLowerCase(metric.metric[0]) + metric.metric.substring(1);
            properties["server.metric.$uncapitalizedMetric"] = metric.value
        }

        postHog?.capture(userId,"server received-data", properties)
    }


    fun registerContainerEngine(containerPlatform: String) {
        postHog?.set(
            userId,
            mapOf("user.container-engine" to containerPlatform)
        )
    }


    fun registerDigmaEngineEventStart(eventName: String, eventDetails: Map<String, Any>) {
        postHog?.capture(
            userId,
            "Engine.".plus(eventName).plus(".start"),
            eventDetails
        )
    }


    fun registerDigmaEngineEventEnd(eventName: String, eventDetails: Map<String, Any>) {
        postHog?.capture(
            userId,
            "Engine.".plus(eventName).plus(".end"),
            eventDetails
        )
    }

    fun registerDigmaEngineEventError(eventName: String, errorMessage: String) {
        postHog?.capture(
            userId,
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

//    override fun dispose() {
//        try {
//            tokenFetcherThread.join()
//        } catch (e: InterruptedException) {
//            Log.debugWithException(LOGGER, e, "Failed waiting for tokenFetcherThread")
//        }
//    }
}