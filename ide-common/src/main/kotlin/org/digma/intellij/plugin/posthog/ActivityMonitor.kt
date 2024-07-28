package org.digma.intellij.plugin.posthog

import com.fasterxml.jackson.core.JsonProcessingException
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.posthog.java.PostHog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaInstant
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.ExceptionUtils
import org.digma.intellij.plugin.common.UniqueGeneratedUserId
import org.digma.intellij.plugin.common.objectToJson
import org.digma.intellij.plugin.execution.DIGMA_INSTRUMENTATION_ERROR
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.user.UserUsageStatsResponse
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.model.rest.version.PerformanceCounterReport
import org.digma.intellij.plugin.model.rest.version.PerformanceMetricsResponse
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.semanticversion.SemanticVersionUtil
import org.digma.intellij.plugin.session.CURRENT_INSTALL_STATUS_KEY
import org.digma.intellij.plugin.session.LAST_INSIGHTS_VIEWED_KEY
import org.digma.intellij.plugin.session.LAST_LENS_CLICKED_KEY
import org.digma.intellij.plugin.session.LATEST_UNKNOWN_RUN_CONFIG_TASKS_KEY
import org.digma.intellij.plugin.session.SessionMetadataProperties
import org.digma.intellij.plugin.session.getCurrentInstallStatus
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes

private const val INSTALL_STATUS_PROPERTY_NAME = "install_status"
private const val ENVIRONMENT_ADDED_PROPERTY_NAME = "environment_added"
private const val LOAD_WARNING_APPEARED_PROPERTY_NAME = "load_warning_appeared"
private const val JIRA_FIELD_COPIED_PROPERTY_NAME = "jira_field_copied"

enum class InstallStatus { Active, Uninstalled, Disabled }

@Service(Service.Level.PROJECT)
class ActivityMonitor(private val project: Project, cs: CoroutineScope) : Disposable {


    private val logger: Logger = Logger.getInstance(ActivityMonitor::class.java)

    companion object {

        private const val TOKEN = "phc_5sy6Kuv1EYJ9GAdWPeGl7gx31RAw7BR7NHnOuLCUQZK"


        @JvmStatic
        fun getInstance(project: Project): ActivityMonitor {
            return project.getService(ActivityMonitor::class.java)
        }


        private fun checkAndConnect(): PostHog? {
            return if (PosthogConnectionTester.isConnectionToPosthogOk()) {
                return PostHog.Builder(TOKEN).build()
            } else {
                null
            }
        }

    }


    private var serverInfo: AboutResult? = null
    private val simpleEventInterceptor = SimpleEventInterceptor(project)

    private val userId: String = UniqueGeneratedUserId.userId
    private val isDevUser: Boolean = UniqueGeneratedUserId.isDevUser
    private var postHog: PostHog? = null
    private val settingsChangeTracker = SettingsChangeTracker()

    init {

        SessionMetadataProperties.getInstance().put(CURRENT_INSTALL_STATUS_KEY, InstallStatus.Active)

        postHog = checkAndConnect()

        registerSessionDetails()

        PluginActivityMonitor.getInstance(project)

        settingsChangeTracker.start(this)

        //why we need all that?
        //there are users that can't connect to posthog due to network restrictions like firewalls or company proxies.
        //these users complain that there are many errors in idea.log. that is because PostHog prints connection errors with
        //System.out/err and e.printStackTrace(), this is fault behaviour of posthog java client, and we can't change it.
        //posthog does not throw or notifies about connection errors,it just prints them and doesn't send the events.
        //So, on initialization, we try to connect with checkAndConnect(), if it fails then maybe it's one of these users,
        // and we'll probably never succeed. checkAndConnect() will return null and when posthog is null no events will be
        // sent and no errors will be logged.
        //but, it could be just a momentary network issue, that's why we keep trying every 5 minutes.
        //once we succeed connection and posthog is not null we stop trying because we have a posthog client.
        //don't care if there are network issues later, the only reason for all this is to identify users that
        // can never connect to posthog and prevent the unnecessary errors to idea.log.
        //another reason is if user started the IDE when there was no network and network started after some time, in that case
        // also we don't want posthog to explode the logs while it can not connect.

        //if posthog is not null don't even start the coroutine.
        if (postHog == null) {
            cs.launch {
                //once we have a non-null posthog stop this coroutine.
                //increase the delay on every iteration, but not more than 30 minutes. probably there is no connection.
                var delay = 1
                while (isActive && postHog == null) {
                    delay(delay.minutes.inWholeMilliseconds)
                    try {
                        delay = min(30, delay + 1)
                        if (isActive) {
                            Log.log(logger::trace, "checking posthog connection")
                            postHog = checkAndConnect()
                            if (postHog == null) {
                                Log.log(logger::trace, "can't connect to posthog")
                            } else {
                                Log.log(logger::trace, "posthog connection OK")
                            }
                        }
                    } catch (e: Throwable) {
                        Log.warnWithException(logger, e, "error while checking posthog connection")
                    }
                }
            }
        }

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

        mutableDetails["firstTimeInsightReceived"] = PersistenceService.getInstance().isFirstTimeInsightReceived()
        mutableDetails["firstTimeAssetsReceived"] = PersistenceService.getInstance().isFirstTimeAssetsReceived()
        mutableDetails["firstTimeRecentActivityReceived"] = PersistenceService.getInstance().isFirstTimeRecentActivityReceived()
        mutableDetails["plugin.version"] = SemanticVersionUtil.getPluginVersionWithoutBuildNumberAndPreRelease("unknown")

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

    fun registerEmail(email: String, courseRequested: Boolean) {
        postHog?.identify(
            userId, mapOf(
                "email" to getEmailForEvent(),
                INSTALL_STATUS_PROPERTY_NAME to getCurrentInstallStatus(),
                "user_requested_course" to courseRequested.toString()
            )
        )
        postHog?.alias(userId, email)
    }

    fun registerCustomEvent(eventName: String, tags: Map<String, Any> = mapOf()) {

        if (simpleEventInterceptor.intercept(eventName, tags)) {
            return
        }

        if (eventName == "user-action")//handling user-action event from jcef component
        {
            tags["action"]?.let {
                registerUserAction(it.toString(), tags)
            }
        } else {
            capture(eventName, tags)
        }
    }


    fun registerUserActionWithOrigin(action: String, origin: UserActionOrigin, details: Map<String, Any> = mapOf()) {

        val detailsToSend = details.toMutableMap()
        detailsToSend["origin"] = origin

        registerUserAction(action, detailsToSend)
    }

    fun registerUserAction(action: String) {

        registerUserAction(action, mapOf())
    }

    fun registerUserAction(action: String, details: Map<String, Any>) {

        val lastUserActionTimestamp = PersistenceService.getInstance().setLastUserActionTimestamp()

        val detailsToSend = details.toMutableMap()
        detailsToSend["action"] = action

        capture(
            "user-action",
            detailsToSend
        )
        postHog?.set(
            userId, mapOf(
                "last-user-action" to action,
                "last-user-action-timestamp" to lastUserActionTimestamp.toString()
            )
        )

        registerOnlineOfflineUserAction(detailsToSend)
    }

    private fun registerOnlineOfflineUserAction(details: Map<String, Any>) {

        val eventName =
            if (PersistenceService.getInstance().isFirstTimeAssetsReceived() && BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
                "online-user-action"
            } else {
                "offline-user-action"
            }

        capture(
            eventName,
            details
        )

    }

    fun registerSpanLinkClicked(spanId: String, origin: UserActionOrigin) {
        registerUserActionWithOrigin(
            "span link clicked", origin, mapOf("span id" to spanId)
        )
    }


    fun registerLensClicked(lens: String) {
        SessionMetadataProperties.getInstance().put(LAST_LENS_CLICKED_KEY, lens)
        registerUserAction("lens clicked", mapOf("lens id" to lens))
    }

    fun registerSidePanelOpened() {
        val lastLensClickTime = SessionMetadataProperties.getInstance().getCreated(LAST_LENS_CLICKED_KEY)
        val origin = if (lastLensClickTime != null && Duration.between(lastLensClickTime.toJavaInstant(), Instant.now()).seconds < 2)
            "lens click"
        else
            "unknown"
        registerUserAction("opened side panel", mapOf("origin" to origin))
    }

    fun registerSidePanelClosed() {
        registerUserAction("closed side panel")
    }

    fun registerObservabilityPanelOpened() {
        registerUserAction("opened observability panel")
    }

    fun registerObservabilityPanelClosed() {
        registerUserAction("closed observability panel")
    }

    fun registerFirstConnectionEstablished() {
        capture("connection first-established")
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
        postHog?.set(
            userId, mapOf(
                "first-assets-timestamp" to Instant.now().toString()
            )
        )
    }

    fun registerFirstTimeRecentActivityReceived() {
        capture("plugin first-activity")
    }

    fun registerObservabilityOn() {
        registerUserAction("turned on observability")
    }

    fun registerObservabilityOff() {
        registerUserAction("turned off observability")
    }


    fun registerError(exception: Throwable?, message: String, extraDetails: Map<String, Any> = mapOf()) {

        //there may be many ProcessCanceledException , our code must deal correctly with that,
        // but we do have code that sends it to error reporting, it's useless.
        //this is the easiest and most central point to ignore PCE.
        //it's enabled in development, in task runIde this property is set to true, we still want to see that in development.
        //need to check isAssignableFrom because there are inheritors like CeProcessCanceledException
        if (exception != null &&
            ProcessCanceledException::class.java.isAssignableFrom(exception::class.java) &&
            System.getProperty("org.digma.plugin.report.pce") == null
        ) {
            return
        }


        try {
            val osType = System.getProperty("os.name")
            val ideInfo = ApplicationInfo.getInstance()
            val ideName = ideInfo.versionName
            val ideVersion = ideInfo.fullVersion
            val ideBuildNumber = ideInfo.build.asString()
            val pluginVersion = SemanticVersionUtil.getPluginVersionWithoutBuildNumberAndPreRelease("unknown")

            //Don't call directly, use ErrorReporter.reportError

            val details = mutableMapOf<String, Any>(
                "error.source" to "plugin",
                "message" to message,
                "os.type" to osType,
                "ide.name" to ideName,
                "ide.version" to ideVersion,
                "ide.build" to ideBuildNumber,
                "plugin.version" to pluginVersion,
                "server.version" to serverInfo?.applicationVersion.toString(),
                "user.type" to if (isDevUser) "internal" else "external"
            )


            exception?.let {
                val exceptionStackTrace = it.let {
                    val stringWriter = StringWriter()
                    exception.printStackTrace(PrintWriter(stringWriter))
                    stringWriter.toString()
                }

                val exceptionMessage: String = ExceptionUtils.getNonEmptyMessage(it)

                val causeExceptionType = ExceptionUtils.findFirstRealExceptionCauseTypeName(it)

                details["exception.message"] = exceptionMessage
                details["exception.stack-trace"] = exceptionStackTrace
                details["cause.exception.type"] = causeExceptionType
                details["exception.type"] = it.javaClass.name
            }

            details.putAll(extraDetails)


            capture(
                "error",
                details
            )
        } catch (e: Exception) {
            val exceptionStackTrace = e.let {
                val stringWriter = StringWriter()
                it.printStackTrace(PrintWriter(stringWriter))
                stringWriter.toString()
            }
            registerCustomEvent(
                "error in registerError", mapOf(
                    "original message" to message,
                    "exception type" to e.javaClass,
                    "stack trace" to exceptionStackTrace
                )
            )
        }
    }


    fun registerFatalError(details: Map<String, String>) {
        capture(
            "fatal error",
            details
        )
    }


    fun registerAnalyticsServiceError(exception: Throwable, message: String, methodName: String, isConnectionException: Boolean) {

        try {

            val osType = System.getProperty("os.name")
            val ideInfo = ApplicationInfo.getInstance()
            val ideName = ideInfo.versionName
            val ideVersion = ideInfo.fullVersion
            val ideBuildNumber = ideInfo.build.asString()
            val pluginVersion = SemanticVersionUtil.getPluginVersionWithoutBuildNumberAndPreRelease("unknown")

            val stringWriter = StringWriter()
            exception.printStackTrace(PrintWriter(stringWriter))

            val exceptionMessage: String = ExceptionUtils.getNonEmptyMessage(exception)

            val eventName = if (isConnectionException) "connection error" else "analytics api error"

            capture(
                eventName,
                mapOf(
                    "error.source" to "plugin",
                    "apiMethodName" to methodName,
                    "message" to message,
                    "exception.type" to exception.javaClass.name,
                    "cause.exception.type" to ExceptionUtils.findFirstRealExceptionCauseTypeName(exception),
                    "exception.message" to exceptionMessage,
                    "exception.stack-trace" to stringWriter.toString(),
                    "os.type" to osType,
                    "ide.name" to ideName,
                    "ide.version" to ideVersion,
                    "ide.build" to ideBuildNumber,
                    "plugin.version" to pluginVersion,
                    "server.version" to serverInfo?.applicationVersion.toString(),
                    "user.type" to if (isDevUser) "internal" else "external"
                )
            )
        } catch (e: Exception) {
            registerCustomEvent(
                "error in registerAnalyticsServiceError", mapOf(
                    "message" to e.message.toString()
                )
            )
        }
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

    fun reportUserUsageStats(uss: UserUsageStatsResponse) {
        capture(
            "user-usage-stats",
            mapOf(
                "has.db.spans" to uss.hasDbSpans(),
                "services.count" to uss.servicesCount,
                "has.distributed.calls" to uss.hasDistributedCalls,
                "trace.depth.max" to uss.traceDepthMax,
                "trace.depth.avg" to uss.traceDepthAvg,
                "unique.spans.count" to uss.uniqueSpansCount,
                "environments.count" to uss.environmentsCount,
                "groupedByClassification" to uss.classificationAggregationString
            )
        )
    }

    fun reportRunConfig(
        runConfigTypeName: String,
        description: String,
        javaToolOptions: String,
        resourceAttributes: String,
        observabilityEnabled: Boolean,
        connectedToBackend: Boolean
    ) {

        val details = mutableMapOf(
            "run.config.type" to runConfigTypeName,
            "configuration description" to description,
            "observability.enabled" to observabilityEnabled,
            "backend.connected" to connectedToBackend,
        )

        //javaToolOptions may be an error message in case instrumentation failed
        if (javaToolOptions.startsWith(DIGMA_INSTRUMENTATION_ERROR)) {
            details["java tool options"] = ""
            details["error message"] = javaToolOptions
            details["error"] = "true"
        } else {
            details["java tool options"] = javaToolOptions
        }

        details["otel resource attributes"] = resourceAttributes

        capture(
            "instrumented run configuration",
            details
        )
    }

    fun reportSupportedRunConfigDetected(details: Map<String, Any>) {
        capture(
            "supported-run-configurations",
            details
        )
    }

    fun reportUnknownConfigurationType(configurationClassName: String, configurationTypeId: String, configurationTypeDisplayName: String) {
        capture(
            "unknown configuration type run",
            mapOf(
                "config.class-name" to configurationClassName,
                "config.type.id" to configurationTypeId,
                "config.type.displayName" to configurationTypeDisplayName
            )
        )
    }

    fun reportUnhandledConfiguration(
        description: String,
        buildSystem: String,
        taskNames: Collection<String>,
        configurationClassName: String,
        configurationType: String
    ) {

        val latestUnknownRunConfigTasks =
            SessionMetadataProperties.getInstance().getAndPutDefault<MutableMap<String, Instant>>(LATEST_UNKNOWN_RUN_CONFIG_TASKS_KEY, mutableMapOf())

        // Purge tasks older than 1 hour
        latestUnknownRunConfigTasks.entries.removeIf {
            it.value.isBefore(Instant.now().minus(1, ChronoUnit.HOURS))
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
            "unhandled configuration ran",
            mapOf(
                "config.build-system" to buildSystem,
                "config.tasks" to taskNamesToReport,
                "config.class-name" to configurationClassName,
                "config.type" to configurationType,
                "config.description" to description
            )
        )
    }

    fun clearLastInsightsViewed() {
        SessionMetadataProperties.getInstance().getAndPutDefault<MutableSet<String>>(LAST_INSIGHTS_VIEWED_KEY, mutableSetOf()).clear()
    }

    fun registerInsightsViewed(insightsAndCounts: List<Pair<String, Int>>) {

        //put a set on first call and no need to put again because it will be the same instance
        val lastInsightsViewed =
            SessionMetadataProperties.getInstance().getAndPutDefault<MutableSet<String>>(LAST_INSIGHTS_VIEWED_KEY, mutableSetOf())

        val insightsTypesToRegister = mutableListOf<Pair<String, Int>>()

        insightsAndCounts.forEach { (insightType, reopenCount) ->
            if (!lastInsightsViewed.contains(insightType)) {
                insightsTypesToRegister.add(insightType to reopenCount)
            }
        }

        lastInsightsViewed.addAll(insightsAndCounts.map { it.first })

        if (insightsTypesToRegister.isEmpty()) {
            return
        }

        val insightsToReopenCount = insightsTypesToRegister.map { InsightToReopenCount(it.first, it.second) }
        capture(
            "insights viewed",
            mapOf(
                "insights" to insightsTypesToRegister.map { it.first },
                "insights_v2" to insightsToReopenCount,
                "maxReopenCount" to insightsToReopenCount.maxByOrNull { it.reopenCount }?.reopenCount.toString()
            )
        )
    }

    fun registerSubDashboardViewed(dashboardType: String) {
        capture(
            "Dashboard sub-chart is viewed",
            mapOf("dashboardType" to dashboardType)
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

        SessionMetadataProperties.getInstance().put(CURRENT_INSTALL_STATUS_KEY, InstallStatus.Uninstalled)

        capture("plugin uninstalled")
        if (PersistenceService.getInstance().hasEmail()) {
            capture(
                "registered user uninstalled", mapOf(
                    "email" to getEmailForEvent(),
                    INSTALL_STATUS_PROPERTY_NAME to getCurrentInstallStatus(),
                    ENVIRONMENT_ADDED_PROPERTY_NAME to PersistenceService.getInstance().isEnvironmentAdded(),
                    LOAD_WARNING_APPEARED_PROPERTY_NAME to PersistenceService.getInstance().isLoadWarningAppeared(),
                    JIRA_FIELD_COPIED_PROPERTY_NAME to PersistenceService.getInstance().isJiraFieldCopied(),
                    "user_requested_course" to "false"
                )
            )
        }
        postHog?.set(
            userId, mapOf(
                INSTALL_STATUS_PROPERTY_NAME to getCurrentInstallStatus(),
                INSTALL_STATUS_PROPERTY_NAME + "_timestamp" to SessionMetadataProperties.getInstance().getCreatedAsString(CURRENT_INSTALL_STATUS_KEY)
            )
        )
        return userId
    }

    fun registerPluginDisabled(): String {

        SessionMetadataProperties.getInstance().put(CURRENT_INSTALL_STATUS_KEY, InstallStatus.Disabled)

        capture("plugin disabled")
        if (PersistenceService.getInstance().hasEmail()) {
            capture(
                "registered user disabled", mapOf(
                    "email" to getEmailForEvent(),
                    INSTALL_STATUS_PROPERTY_NAME to getCurrentInstallStatus(),
                    ENVIRONMENT_ADDED_PROPERTY_NAME to PersistenceService.getInstance().isEnvironmentAdded(),
                    LOAD_WARNING_APPEARED_PROPERTY_NAME to PersistenceService.getInstance().isLoadWarningAppeared(),
                    JIRA_FIELD_COPIED_PROPERTY_NAME to PersistenceService.getInstance().isJiraFieldCopied(),
                    "user_requested_course" to "false"
                )
            )
        }
        postHog?.set(
            userId, mapOf(
                INSTALL_STATUS_PROPERTY_NAME to getCurrentInstallStatus(),
                INSTALL_STATUS_PROPERTY_NAME + "_timestamp" to SessionMetadataProperties.getInstance().getCreatedAsString(CURRENT_INSTALL_STATUS_KEY)
            )
        )
        return userId
    }


    fun registerServerInfo(serverInfo: AboutResult) {
        //AboutResult is a data class , equals should work correctly
        if (this.serverInfo != serverInfo) {
            this.serverInfo = serverInfo
            postHog?.set(
                userId,
                mapOf(
                    "server.version" to serverInfo.applicationVersion,
                    "server.deploymentType" to (serverInfo.deploymentType ?: BackendDeploymentType.Unknown),
                    "site" to (serverInfo.site ?: "")
                )
            )
        }
    }


    fun registerPerformanceMetrics(performanceMetrics: PerformanceMetricsResponse, isFirstTime: Boolean) {

        val jsonData = try {
            objectToJson(performanceMetrics)
        } catch (e: JsonProcessingException) {
            "could not write PerformanceMetricsResponse to json $e"
        }

        val properties = mutableMapOf<String, Any>(
            "data" to jsonData,
            "server.startTime" to performanceMetrics.serverStartTime,
            "probeTime" to performanceMetrics.probeTime
        )

        if (isFirstTime) {
            properties["first time"] = true
        }

        performanceMetrics.metrics.forEach { metric: PerformanceCounterReport ->

            when (metric.metric) {
                "TotalUniqueSpans" -> {
                    if (metric.value is List<*>) {
                        val sum: Int = (metric.value as List<*>).sumOf { any: Any? -> any as Int }
                        properties["TotalUniqueSpans"] = sum
                    }
                }

                "MaxSpans" -> properties["MaxSpans"] = metric.value
                "MaxTraces" -> properties["MaxTraces"] = metric.value
            }
        }

        capture("performance-metrics", properties)
    }


    fun registerContainerEngine(containerPlatform: String) {
        postHog?.set(
            userId,
            mapOf("user.container-engine" to containerPlatform)
        )
    }


    fun registerDigmaEngineEventStart(eventName: String, eventDetails: Map<String, Any> = mapOf()) {
        capture(
            "Engine.".plus(eventName).plus(".start"),
            eventDetails
        )
    }


    fun registerDigmaEngineEventEnd(eventName: String, eventDetails: Map<String, Any> = mapOf()) {
        capture(
            "Engine.".plus(eventName).plus(".end"),
            eventDetails
        )
    }

    fun registerDigmaEngineEventRetry(eventName: String, eventDetails: Map<String, Any> = mapOf()) {
        capture(
            "Engine.".plus(eventName).plus(".retry"),
            eventDetails
        )
    }

    fun registerDigmaEngineEventError(eventName: String, errorMessage: String, eventDetails: Map<String, Any> = mapOf()) {
        val detailsToUse = eventDetails.toMutableMap()
        detailsToUse["errorMessage"] = errorMessage
        capture(
            "Engine.".plus(eventName).plus(".error"),
            detailsToUse
        )
    }

    fun registerDigmaEngineEventInfo(eventName: String, eventDetails: Map<String, Any> = mapOf()) {
        capture(
            "Engine.".plus(eventName),
            eventDetails
        )
    }


    fun registerJvmNavigationDiscoveryEvent(eventName: String, details: Map<String, Any> = mapOf()) {
        val enrichedDetails = details.toMutableMap()
        enrichedDetails["type"] = "jvm"
        registerNavigationDiscoveryEvent(eventName, enrichedDetails)
    }


    private fun registerNavigationDiscoveryEvent(eventName: String, details: Map<String, Any> = mapOf()) {
        capture(
            "NavigationDiscovery.".plus(eventName),
            details
        )
    }


    private fun registerSessionDetails() {

        SessionMetadataProperties.getInstance().put(CURRENT_INSTALL_STATUS_KEY, InstallStatus.Active)

        val osType = System.getProperty("os.name")
        val ideInfo = ApplicationInfo.getInstance()
        val ideName = ideInfo.versionName
        val ideVersion = ideInfo.fullVersion
        val ideBuildNumber = ideInfo.build.asString()
        val pluginVersion = SemanticVersionUtil.getPluginVersionWithoutBuildNumberAndPreRelease("unknown")
        val isJcefSupported = JBCefApp.isSupported()

        postHog?.set(
            userId,
            mapOf(
                "os.type" to osType,
                "ide.name" to ideName,
                "ide.version" to ideVersion,
                "ide.build" to ideBuildNumber,
                "plugin.version" to pluginVersion,
                "user.type" to if (isDevUser) "internal" else "external",
                "jcef.supported" to isJcefSupported,
                INSTALL_STATUS_PROPERTY_NAME to getCurrentInstallStatus()
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

    fun registerLoadWarning(description: String, lastUpdated: Date) {

        PersistenceService.getInstance().setLoadWarningAppearedTimestamp()

        capture(
            "load-warning-appeared",
            mapOf(
                "description" to description,
                "last-updated" to lastUpdated,
                INSTALL_STATUS_PROPERTY_NAME to getCurrentInstallStatus(),
                ENVIRONMENT_ADDED_PROPERTY_NAME to PersistenceService.getInstance().isEnvironmentAdded(),
                LOAD_WARNING_APPEARED_PROPERTY_NAME to PersistenceService.getInstance().isLoadWarningAppeared(),
                JIRA_FIELD_COPIED_PROPERTY_NAME to PersistenceService.getInstance().isJiraFieldCopied()
            )
        )

        postHog?.identify(
            userId,
            mapOf(
                LOAD_WARNING_APPEARED_PROPERTY_NAME + "_timestamp" to PersistenceService.getInstance().getLoadWarningAppearedTimestamp(),
                "user_requested_course" to "false"
            ),
            mapOf(
                LOAD_WARNING_APPEARED_PROPERTY_NAME to PersistenceService.getInstance().isLoadWarningAppeared(),
            ),
        )

    }

    @Suppress("unused")
    fun registerAddEnvironment(environment: String) {

        PersistenceService.getInstance().setEnvironmentAddedTimestamp()

        val eventName = "add environment"
        val eventDetails = mapOf(
            "environment" to environment,
            INSTALL_STATUS_PROPERTY_NAME to getCurrentInstallStatus(),
            ENVIRONMENT_ADDED_PROPERTY_NAME to PersistenceService.getInstance().isEnvironmentAdded(),
            LOAD_WARNING_APPEARED_PROPERTY_NAME to PersistenceService.getInstance().isLoadWarningAppeared(),
            JIRA_FIELD_COPIED_PROPERTY_NAME to PersistenceService.getInstance().isJiraFieldCopied()
        )
        capture(
            eventName,
            eventDetails
        )
        registerUserAction(eventName, eventDetails)

        postHog?.identify(
            userId,
            mapOf(
                ENVIRONMENT_ADDED_PROPERTY_NAME + "_timestamp" to PersistenceService.getInstance().getEnvironmentAddedTimestamp(),
                "user_requested_course" to "false"
            ),
            mapOf(
                ENVIRONMENT_ADDED_PROPERTY_NAME to PersistenceService.getInstance().isEnvironmentAdded()
            )
        )
    }


    fun registerJiraFieldCopied(eventName: String, details: Map<String, Any>) {

        PersistenceService.getInstance().setJiraFieldCopiedTimestamp()

        val extraDetails = mapOf(
            INSTALL_STATUS_PROPERTY_NAME to getCurrentInstallStatus(),
            ENVIRONMENT_ADDED_PROPERTY_NAME to PersistenceService.getInstance().isEnvironmentAdded(),
            LOAD_WARNING_APPEARED_PROPERTY_NAME to PersistenceService.getInstance().isLoadWarningAppeared(),
            JIRA_FIELD_COPIED_PROPERTY_NAME to PersistenceService.getInstance().isJiraFieldCopied()
        )

        val mutableDetails = details.toMutableMap()
        mutableDetails.putAll(extraDetails)

        capture(
            eventName,
            mutableDetails
        )

        postHog?.identify(
            userId,
            mapOf(
                JIRA_FIELD_COPIED_PROPERTY_NAME + "_timestamp" to PersistenceService.getInstance().getJiraFieldCopiedTimestamp(),
                "user_requested_course" to "false"
            ),
            mapOf(
                JIRA_FIELD_COPIED_PROPERTY_NAME to PersistenceService.getInstance().isJiraFieldCopied()
            )
        )

    }

    fun registerScopeChanged(scope: String) {
        capture(
            "scope changed",
            mapOf("scope" to scope)
        )
    }


    fun reportDigmathonEvent(eventType: String, details: Map<String, String>) {

        val mutableDetails = details.toMutableMap()
        mutableDetails["event type"] = eventType

        capture(
            "Digmathon",
            mutableDetails
        )

    }

    fun registerCloseThrottlingMessage(throttlingType: String) {
        capture(
            "throttlingClosed",
            mapOf("throttlingType" to throttlingType)
        )
    }

    private fun getEmailForEvent(): String {
        return PersistenceService.getInstance().getUserRegistrationEmail() ?: PersistenceService.getInstance().getUserEmail() ?: ""
    }

    fun reportApiPerformanceIssue(details: MutableMap<String, Any>) {
        capture(
            "api-performance-issue", details
        )
    }

    fun reportScheduledTaskPerformanceIssue(details: MutableMap<String, Any>) {
        capture(
            "scheduled-task-performance-issue", details
        )
    }

    fun registerSchedulerStatistics(details: Map<String, Any>) {
        capture("SchedulerStatistics", details)
    }

    fun registerSchedulerSizeIncreased(details: Map<String, Any>) {
        capture("SchedulerSizeIncreased", details)
    }

}