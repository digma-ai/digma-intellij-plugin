package org.digma.intellij.plugin.posthog

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.project.Project
import com.posthog.java.PostHog
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.InsightType
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import java.time.LocalDateTime
import java.util.Optional

class ActivityMonitor(private val project: Project) : Runnable, Disposable {

    companion object {
        private val LOGGER = Logger.getInstance(ActivityMonitor::class.java)

        @JvmStatic
        fun getInstance(project: Project): ActivityMonitor {
            return project.getService(ActivityMonitor::class.java)
        }
    }

    private val clientId: String
    private val tokenFetcherThread = Thread(this, "Token fetcher thread")
    private var postHog: PostHog? = null
    private var lastLensClick: LocalDateTime? = null
    private var lastInsightsViewed: HashSet<InsightType>? = null

    init {
        val hostname = CommonUtils.getLocalHostname()
        clientId =
                if (System.getenv("devenv") == "digma") hostname
                else Integer.toHexString(hostname.hashCode())

        val cachedToken = getCachedToken()
        postHog =
                if (cachedToken != null) PostHog.Builder(cachedToken).build()
                else null

        tokenFetcherThread.start()
    }

    override fun run() {
        val cachedToken = getCachedToken()
        val latestToken = getLatestToken()
        if (latestToken != null && latestToken != cachedToken) {
            postHog = PostHog.Builder(latestToken).build()
            setCachedToken(latestToken)
        }
        if (postHog != null) {
            Log.log(LOGGER::info, "Posthog was configured successfully with " +
                    (if (latestToken != null) "latest token" else "cached token"))
        } else {
            Log.log(LOGGER::info, "Posthog failed to be configured")
        }
        registerSessionDetails()
    }

    fun registerCustomEvent(eventName: String){
        postHog?.capture(clientId, eventName)
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
                clientId,
                "side-panel opened",
                mapOf("reason" to reason)
        )
    }

    fun registerFirstConnectionEstablished() {
        postHog?.capture(clientId, "connection first-established")
    }

    fun registerConnectionError(action: String, message: String) {
        postHog?.capture(clientId, "connection error", mapOf("reason" to message, "action" to action))
    }

    fun registerFirstInsightReceived() {
        postHog?.capture(clientId, "insight first-received")
    }

    fun registerObservabilityOn() {
        postHog?.capture(clientId, "observability is turned on")
    }

    fun registerObservabilityOff() {
        postHog?.capture(clientId, "observability is turned off")
    }

    fun registerError(exception: Exception, message: String) {
        val stringWriter = StringWriter()
        exception.printStackTrace(PrintWriter(stringWriter))

        postHog?.capture(
                clientId,
                "error",
                mapOf(
                        "message" to message,
                        "exception.type" to exception.javaClass.name,
                        "exception.message" to exception.message,
                        "exception.stack-trace" to stringWriter.toString()
                )
        )
    }

    fun registerInsightsViewed(insightTypes: List<out InsightType>) {
        val newInsightsViewed = HashSet(insightTypes)
        if (lastInsightsViewed != null && lastInsightsViewed == newInsightsViewed)
            return

        lastInsightsViewed = newInsightsViewed
        postHog?.capture(
                clientId,
                "insights viewed",
                mapOf("insights" to insightTypes)
        )
    }

    fun registerInsightButtonClicked(button: String) {
        postHog?.capture(
                clientId,
                "insights button-clicked",
                mapOf("button" to button)
        )
    }

    private fun registerSessionDetails() {
        val osType = System.getProperty("os.name")
        val ideVersion = ApplicationInfo.getInstance().build.asString()
        val pluginVersion = Optional.ofNullable(PluginManagerCore.getPlugin(com.intellij.openapi.extensions.PluginId.getId(PluginId.PLUGIN_ID)))
                .map(PluginDescriptor::getVersion)
                .orElse("unknown")

        postHog?.set(
                clientId,
                mapOf(
                        "os.type" to osType,
                        "ide.version" to ideVersion,
                        "plugin.version" to pluginVersion
                ))
    }

    override fun dispose() {
        try {
            tokenFetcherThread.join()
        } catch (e: InterruptedException) {
            Log.debugWithException(LOGGER, e, "Failed waiting for tokenFetcherThread")
        }
    }
}