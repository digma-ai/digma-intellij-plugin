package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.env.EnvironmentsSupplier
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredPanel
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.ui.common.openJaegerFromRecentActivity
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.recentactivity.model.CloseLiveViewMessage
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityEntrySpanForTracePayload
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityEntrySpanPayload

@Service(Service.Level.PROJECT)
class RecentActivityService(val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null

    override fun dispose() {
        //nothing to do , used as parent disposable
    }


    fun setJcefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }


    fun getRecentActivities(environments: List<String>): RecentActivityResult? {

        return try {

            Log.log(logger::trace, project, "getRecentActivities called with envs: {}", environments)

            val recentActivityData = project.service<AnalyticsService>().getRecentActivity(environments)

            if (recentActivityData.entries.isNotEmpty() && !service<PersistenceService>().isFirstTimeRecentActivityReceived()) {
                service<PersistenceService>().setFirstTimeRecentActivityReceived()
                project.service<ActivityMonitor>().registerFirstTimeRecentActivityReceived()
            }

            Log.log(logger::trace, project, "got recent activity {}", recentActivityData)

            recentActivityData

        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "AnalyticsServiceException for getRecentActivity: {}", e.meaningfulMessage)
            ErrorReporter.getInstance().reportError(project, "RecentActivityService.getRecentActivities", e)
            null
        }
    }


    fun processRecentActivityGoToSpanRequest(payload: RecentActivityEntrySpanPayload?) {

        Log.log(logger::trace, project, "processRecentActivityGoToSpanRequest called with {}", payload)

        payload?.let {
            EDT.ensureEDT {

                try {

                    //todo: we need to show the insights only after the environment changes. but environment change is done in the background
                    // and its not easy to sync the change environment and showing the insights.
                    // this actually comes to solve the case that the recent activity and the main environment combo
                    // are not the same one and they need to sync. when this is fixed we can remove
                    // the methods EnvironmentsSupplier.setCurrent(java.lang.String, boolean, java.lang.Runnable)
                    // changing environment should be atomic and should not be effected by user activities like
                    // clicking a link in recent activity

                    val spanId = payload.span.spanCodeObjectId
                    spanId?.let {
                        val environmentsSupplier: EnvironmentsSupplier = project.service<AnalyticsService>().environment
                        environmentsSupplier.setCurrent(payload.environment) {
                            ScopeManager.getInstance(project).changeScope(SpanScope(spanId))
                        }
                        project.service<ActivityMonitor>().registerSpanLinkClicked(MonitoredPanel.RecentActivity)
                    }

                } catch (e: Exception) {
                    Log.warnWithException(logger, project, e, "error in processRecentActivityGoToSpanRequest")
                    ErrorReporter.getInstance().reportError(project, "RecentActivityService.processRecentActivityGoToSpanRequest", e)
                }
            }
        }
    }


    fun processRecentActivityGoToTraceRequest(payload: RecentActivityEntrySpanForTracePayload?) {

        try {
            Log.log(logger::trace, project, "processRecentActivityGoToTraceRequest called with {}", payload)

            if (payload != null) {
                openJaegerFromRecentActivity(project, payload.traceId, payload.span.scopeId, payload.span.spanCodeObjectId)
            } else {
                Log.log({ message: String? -> logger.debug(message) }, "processRecentActivityGoToTraceRequest payload is empty")
            }
        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "error in processRecentActivityGoToTraceRequest")
            ErrorReporter.getInstance().reportError(project, "RecentActivityService.processRecentActivityGoToTraceRequest", e)
        }
    }


    fun startLiveView(codeObjectId: String) {
        EDT.ensureEDT {
            project.service<RecentActivityToolWindowShower>().showToolWindow()
        }
        project.service<LiveViewUpdater>().sendLiveData(codeObjectId)
    }

    fun liveViewClosed(closeLiveViewMessage: CloseLiveViewMessage?) {
        project.service<LiveViewUpdater>().stopLiveView(closeLiveViewMessage?.payload?.codeObjectId)
        project.service<ActivityMonitor>().registerCustomEvent("live view closed", emptyMap())
    }

    fun deleteEnvironment(environment: String) {
        try {

            Log.log(logger::trace, project, "deleteEnvironment called with {}", environment)

            val response = project.service<AnalyticsService>().deleteEnvironment(environment)
            if (response.success) {
                Log.log(logger::trace, project, "deleteEnvironment {} finished successfully", environment)
            } else {
                Log.log(logger::trace, project, "deleteEnvironment {} faled", environment)
            }
            project.service<AnalyticsService>().environment.refreshNowOnBackground()

        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "error deleting environment")
            ErrorReporter.getInstance().reportError(project, "RecentActivityService.deleteEnvironment", e)
        }
    }

    fun showRegistrationPopup() {

    }

}