package org.digma.intellij.plugin.posthog

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.Project
import com.posthog.java.PostHog
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.semanticversion.SemanticVersionUtil
import org.threeten.extra.Hours
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

class ActivityMonitor(private val project: Project) /*: Runnable, Disposable*/ {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ActivityMonitor {
            return project.getService(ActivityMonitor::class.java)
        }
    }

    private val userId: String
    private val isDevUser: Boolean

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
            userId = Integer.toHexString(hostname.hashCode())
            isDevUser = false
        }

//        val cachedToken = getCachedToken()
//        postHog =
//                if (cachedToken != null) PostHog.Builder(cachedToken).build()
//                else null
//
//        tokenFetcherThread.start()

        val token = "phc_5sy6Kuv1EYJ9GAdWPeGl7gx31RAw7BR7NHnOuLCUQZK"
        postHog = PostHog.Builder(token).build()
        registerSessionDetails()

        ConnectionActivityMonitor.loadInstance(project)
        PluginActivityMonitor.loadInstance(project)
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

    fun registerEmail(email: String) {
        postHog?.alias(userId, email)
    }

    fun registerCustomEvent(eventName: String, tags: Map<String, Any>?) {
        postHog?.capture(userId, eventName, tags)
    }

    fun registerLensClicked() {
        lastLensClick = LocalDateTime.now()
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

    fun registerFirstInsightReceived() {
        postHog?.capture(userId, "insight first-received")
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

    fun reportRunConfig(runConfigTypeName: String, observabilityEnabled: Boolean) {
        postHog?.capture(
            userId,
            "run config",
            mapOf(
                "run.config.type" to runConfigTypeName,
                "observability.enabled" to observabilityEnabled,
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

    fun registerButtonClicked(button: String, source: Any ? = null) {
        val properties:MutableMap<String, Any> = mutableMapOf()
        properties["button"] = button
        if(source != null){
            properties["source"] = source
        }

        postHog?.capture(
            userId,
            "button-clicked",
            properties
        )
    }

    fun registerInsightButtonClicked(button: String, insight: Any) {
        val properties:MutableMap<String, Any> = mutableMapOf()
        properties["button"] = button
        if(insight != null){
            properties["source"] = insight
        }

        postHog?.capture(
            userId,
            "insights button-clicked",
            properties
        )
    }

    fun registerFirstTimePluginLoaded() {
        postHog?.capture(userId, "plugin first-loaded")
    }

    fun registerPluginUninstalled() {
        postHog?.capture(userId, "plugin uninstalled")
    }


    fun registerServerInfo(serverInfo: AboutResult) {
        postHog?.set(
            userId,
            mapOf("server.version" to serverInfo.applicationVersion,
                "server.deploymentType" to if(serverInfo.deploymentType != null) serverInfo.deploymentType else BackendDeploymentType.Unknown )
        )
    }

    fun registerContainerEngine(containerPlatform: String) {
        postHog?.set(
            userId,
            mapOf("user.container-engine" to containerPlatform)
        )
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