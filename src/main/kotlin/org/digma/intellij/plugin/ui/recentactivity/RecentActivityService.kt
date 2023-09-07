package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.LOCAL_ENV
import org.digma.intellij.plugin.common.LOCAL_TESTS_ENV
import org.digma.intellij.plugin.common.SUFFIX_OF_LOCAL
import org.digma.intellij.plugin.common.SUFFIX_OF_LOCAL_TESTS
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredPanel
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.list.insights.openJaegerFromRecentActivity
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import org.digma.intellij.plugin.ui.recentactivity.model.CloseLiveViewMessage
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityEntrySpanForTracePayload
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityEntrySpanPayload
import java.util.Locale

@Service(Service.Level.PROJECT)
class RecentActivityService(val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private val emptyRecentActivitiesResult = RecentActivityResult(null, listOf())


    override fun dispose() {
        //nothing to do , used as parent disposable
    }


    fun getRecentActivities(environments: List<String>): RecentActivityResult {

//        val allEnvironments = project.service<AnalyticsService>().environments ?: return emptyRecentActivitiesResult

        return try {
            val recentActivityData = project.service<AnalyticsService>().getRecentActivity(environments)

            if (recentActivityData.entries.isNotEmpty() && !service<PersistenceService>().state.firstTimeRecentActivityReceived) {
                service<PersistenceService>().state.firstTimeRecentActivityReceived = true
                project.service<ActivityMonitor>().registerFirstTimeRecentActivityReceived()
            }

            recentActivityData

        } catch (e: AnalyticsServiceException) {
            Log.log(logger::warn, "AnalyticsServiceException for getRecentActivity: {}", e.meaningfulMessage)
            emptyRecentActivitiesResult
        }
    }


    fun processRecentActivityGoToSpanRequest(payload: RecentActivityEntrySpanPayload?) {
        payload?.let {
            EDT.ensureEDT {

                //todo: we need to show the insights only after the environment changes. but environment change is done in the background
                // and its not easy to sync the change environment and showing the insights.
                // this actually comes to solve the case that the recent activity and the main environment combo
                // are not the same one and they need to sync. when this is fixed we can remove
                // the methods EnvironmentsSupplier.setCurrent(java.lang.String, boolean, java.lang.Runnable)
                // changing environment should be atomic and should not be effected by user activities like
                // clicking a link in recent activity

                val spanId = payload.span.spanCodeObjectId
                val methodId = payload.span.methodCodeObjectId
                val canNavigate = project.service<CodeNavigator>().canNavigateToSpanOrMethod(spanId, methodId)
                if (canNavigate) {
                    project.service<MainToolWindowCardsController>().closeAllNotificationsIfShowing()
                    val environmentsSupplier: EnvironmentsSupplier = project.service<AnalyticsService>().environment
                    val actualEnvName: String = adjustBackEnvNameIfNeeded(payload.environment)
                    environmentsSupplier.setCurrent(actualEnvName, false) {
                        EDT.ensureEDT {
                            project.service<InsightsViewOrchestrator>().showInsightsForSpanOrMethodAndNavigateToCode(spanId, methodId)
                        }
                    }
                } else {
                    project.service<MainToolWindowCardsController>().closeAllNotificationsIfShowing()
                    NotificationUtil.showNotification(project, "code object could not be found in the workspace")
                    val environmentsSupplier: EnvironmentsSupplier = project.service<AnalyticsService>().environment
                    val actualEnvName: String = adjustBackEnvNameIfNeeded(payload.environment)
                    environmentsSupplier.setCurrent(actualEnvName, false) {
                        EDT.ensureEDT {
                            project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(payload.span.spanCodeObjectId)
                        }
                    }
                }
                project.service<ActivityMonitor>().registerSpanLinkClicked(MonitoredPanel.RecentActivity, canNavigate)
            }
        }
    }


    private fun adjustBackEnvNameIfNeeded(environment: String): String {

        val localHostname = CommonUtils.getLocalHostname()

        if (environment.equals(LOCAL_ENV, ignoreCase = true)) {
            return (localHostname + SUFFIX_OF_LOCAL).uppercase(Locale.getDefault())
        }
        return if (environment.equals(LOCAL_TESTS_ENV, ignoreCase = true)) {
            (localHostname + SUFFIX_OF_LOCAL_TESTS).uppercase(Locale.getDefault())
        } else environment
    }


    fun processRecentActivityGoToTraceRequest(payload: RecentActivityEntrySpanForTracePayload?) {
        if (payload != null) {
            openJaegerFromRecentActivity(project, payload.traceId, payload.span.scopeId)
        } else {
            Log.log({ message: String? -> logger.debug(message) }, "processRecentActivityGoToTraceRequest payload is empty")
        }
    }


    fun startLiveView(codeObjectId: String) {
        EDT.invokdeAndWait {
            project.service<RecentActivityToolWindowShower>().showToolWindow()
        }

        project.service<LiveViewUpdater>().sendLiveData(codeObjectId)
    }

    fun liveViewClosed(closeLiveViewMessage: CloseLiveViewMessage?) {
        project.service<LiveViewUpdater>().stopLiveView(closeLiveViewMessage?.payload?.codeObjectId)
    }

}