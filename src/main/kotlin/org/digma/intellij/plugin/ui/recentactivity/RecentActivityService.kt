package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.activation.UserActivationService
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.analytics.refreshEnvironmentsNowOnBackground
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.UserActionOrigin
import org.digma.intellij.plugin.recentactivity.RecentActivityToolWindowShower
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.ui.common.openJaegerFromRecentActivity
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.model.DigmathonProgressDataPayload
import org.digma.intellij.plugin.ui.jcef.model.SetDigmathonProgressData
import org.digma.intellij.plugin.ui.jcef.model.ViewedInsight
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.recentactivity.model.AddToConfigData
import org.digma.intellij.plugin.ui.recentactivity.model.AdditionToConfigResult
import org.digma.intellij.plugin.ui.recentactivity.model.CloseLiveViewMessage
import org.digma.intellij.plugin.ui.recentactivity.model.OpenRegistrationDialogMessage
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityEntrySpanForTracePayload
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityEntrySpanPayload
import org.digma.intellij.plugin.ui.recentactivity.model.SetAddToConfigResult
import org.digma.intellij.plugin.ui.recentactivity.model.SetEnvironmentCreatedMessage
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class RecentActivityService(val project: Project, private val cs: CoroutineScope) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null

    private val appInitialized: AtomicBoolean = AtomicBoolean(false)

    companion object {
        fun getInstance(project: Project): RecentActivityService {
            return project.service<RecentActivityService>()
        }
    }

    override fun dispose() {
        jCefComponent = null
    }


    fun setJcefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }

    fun appInitialized() {
        appInitialized.set(true)
    }


    fun getRecentActivities(environmentsIds: List<String>): RecentActivityResult? {

        return try {

            Log.log(logger::trace, project, "getRecentActivities called with envs: {}", environmentsIds)

            val recentActivityData = AnalyticsService.getInstance(project).getRecentActivity(environmentsIds)

            if (recentActivityData.entries.isNotEmpty()) {
                if (UserActivationService.getInstance().isFirstRecentActivityReceived()) {
                    UserActivationService.getInstance().recentActivityReceivedInProject(project)
                } else {
                    UserActivationService.getInstance().setFirstRecentActivityReceived(project)
                }
            }

            Log.log(logger::trace, project, "got recent activity {}", recentActivityData)

            recentActivityData

        } catch (e: AnalyticsServiceException) {
            Log.debugWithException(logger, project, e, "AnalyticsServiceException for getRecentActivity: {}", e.nonNullMessage)
            ErrorReporter.getInstance().reportError(project, "RecentActivityService.getRecentActivities", e)
            null
        }
    }


    fun processRecentActivityGoToSpanRequest(payload: RecentActivityEntrySpanPayload?) {

        Log.log(logger::trace, project, "processRecentActivityGoToSpanRequest called with {}", payload)

        payload?.let {
            val spanId = payload.span.spanCodeObjectId
            spanId?.let {
                ScopeManager.getInstance(project).changeScope(SpanScope(spanId))
                ActivityMonitor.getInstance(project).registerSpanLinkClicked(it, UserActionOrigin.RecentActivity)
            }
        }
    }


    fun processRecentActivityGoToTraceRequest(payload: RecentActivityEntrySpanForTracePayload?) {

        Log.log(logger::trace, project, "processRecentActivityGoToTraceRequest called with {}", payload)

        if (payload != null) {
            openJaegerFromRecentActivity(project, payload.traceId, payload.span.scopeId, payload.span.spanCodeObjectId)
        } else {
            Log.log(logger::debug, "processRecentActivityGoToTraceRequest payload is empty")
        }
    }


    fun startLiveView(codeObjectId: String) {
        EDT.ensureEDT {
            project.service<RecentActivityToolWindowShower>().showToolWindow()
        }
        project.service<LiveViewUpdater>().sendLiveData(codeObjectId)
    }

    fun liveViewClosed(closeLiveViewMessage: CloseLiveViewMessage?) {
        project.service<LiveViewUpdater>().stopLiveView()
        if (closeLiveViewMessage == null) {
            ActivityMonitor.getInstance(project).registerCustomEvent("live view closed without message", emptyMap())
        } else {
            ActivityMonitor.getInstance(project)
                .registerUserAction("live view closed", mapOf("code object id" to closeLiveViewMessage.payload.codeObjectId))
        }
    }

    fun createEnvironment(request: MutableMap<String, Any>) {
        val response = try {
            val result = AnalyticsService.getInstance(project).createEnvironment(request)
            result
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error creation {}", e.message)
            ErrorReporter.getInstance().reportError(project, "RecentActivityService.createEnvironment", e)
            ""
        }
        refreshEnvironmentsNowOnBackground(project)
        val msg = SetEnvironmentCreatedMessage(response)
        jCefComponent?.let {
            serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, msg)
        }
    }

    fun deleteEnvironmentV2(id: String) {
        try {
            AnalyticsService.getInstance(project).deleteEnvironmentV2(id)
            refreshEnvironmentsNowOnBackground(project)
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error delete {}", e.message)
            ErrorReporter.getInstance().reportError(project, "RecentActivityService.deleteEnvironmentV2", e)
        }
    }

    fun addVarRunToConfig(environment: String) {
        val result = service<AddEnvironmentsService>().addToCurrentRunConfig(project, environment)

        val msg = SetAddToConfigResult(
            AddToConfigData(environment, if (result) AdditionToConfigResult.success else AdditionToConfigResult.failure)
        )
        jCefComponent?.let {
            serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, msg)
        }

        project.service<RecentActivityUpdater>().updateLatestActivities()
    }


    fun clearSelectedRunConfig() {
        service<AddEnvironmentsService>().clearSelectedRunConfig(project)
    }


    fun openRegistrationDialog() {

        project.service<RecentActivityToolWindowShower>().showToolWindow()

        //maybe the recent activity is not open yet. it will be opened as a result of calling
        // showToolWindow, but takes some time before the app is ready to accept messages.
        // so wait here until the app is initialized
        cs.launch {

            val startTime = Instant.now()

            //wait for the app to initialize. maximum 60 seconds and stop the coroutine.
            // 60 seconds should be enough, if the app was not initialized then something is wrong
            while (!appInitialized.get() && isActive &&
                startTime.until(Instant.now(), ChronoUnit.SECONDS) < 60
            ) {
                delay(5)
            }

            jCefComponent?.let {
                val message = OpenRegistrationDialogMessage()
                serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, message)
            }
        }
    }


    fun setDigmathonProgressData(insightsTypes: Map<String, Instant>, insightsViewedLastUpdated: Instant?) {

        val viewedInsights = insightsTypes.map {
            ViewedInsight(it.key, it.value.toString())
        }

        jCefComponent?.jbCefBrowser?.cefBrowser?.let {
            serializeAndExecuteWindowPostMessageJavaScript(
                it,
                SetDigmathonProgressData(DigmathonProgressDataPayload(viewedInsights, insightsViewedLastUpdated?.toString()))
            )
        }
    }


}