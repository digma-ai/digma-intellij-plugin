package org.digma.intellij.plugin.scope

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.CodeObjectsUtil
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_HIGH_TRY_FIX
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
import org.digma.intellij.plugin.model.rest.insights.InsightsStatsResult
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.navigation.View
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.service.ErrorsViewOrchestrator
import org.digma.intellij.plugin.ui.service.ErrorsViewService

@Service(Service.Level.PROJECT)
class ScopeManager(private val project: Project) {

    private val logger = Logger.getInstance(this::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ScopeManager {
            return project.service<ScopeManager>()
        }
    }

    fun changeToHome(isCalledFromReact: Boolean = false) {

        EDT.assertNonDispatchThread()

        ActivityMonitor.getInstance(project).registerScopeChanged("home")

        ErrorsViewService.getInstance(project).empty()

        EDT.ensureEDT {
            MainToolWindowCardsController.getInstance(project).closeAllNotificationsIfShowing()
            MainToolWindowCardsController.getInstance(project).closeCoveringViewsIfNecessary()
            project.service<ErrorsViewOrchestrator>().closeErrorDetails()

            if (!isCalledFromReact) {
                //if the tool window is not shown already don't show it.
                // if react called changeToHome it's ok not to show the tool window, usually its on connection events.
                //if change to home was called because of user click then the window is already shown.
                ToolWindowShower.getInstance(project).showToolWindow()
            }

            val contentViewSwitcher = MainContentViewSwitcher.getInstance(project)
            if (contentViewSwitcher.getSelectedView() != View.Assets) {
                contentViewSwitcher.showInsights()
            }
        }

        fireScopeChangedEvent(null, CodeLocation(listOf(), listOf()), false)
    }

    //if changeView is true some logic wil run to decide if to change the view
    // to analytics or to insights. some callers will send false and change view according
    // to other logic
    fun changeScope(scope: Scope, changeView: Boolean = true) {

        EDT.assertNonDispatchThread()

        ActivityMonitor.getInstance(project).registerScopeChanged(scope.toString())

        try {
            when (scope) {
                is SpanScope -> changeToSpanScope(scope, changeView)

                else -> {
                    ErrorReporter.getInstance().reportError(
                        project, "ScopeManager.changeScope", "changeScope,unknown scope", mapOf(
                            "error hint" to "got unknown scope $scope",
                            SEVERITY_PROP_NAME to SEVERITY_HIGH_TRY_FIX
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ScopeManager.changeScope", e)
        }
    }


    private fun changeToSpanScope(scope: SpanScope, changeView: Boolean) {

        val spanScopeInfo = try {
            AnalyticsService.getInstance(project).getAssetDisplayInfo(scope.spanCodeObjectId)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ScopeManager.changeToSpanScope", e)
            null
        }

        val methodId = spanScopeInfo?.methodCodeObjectId ?: tryGetMethodIdForSpan(scope.spanCodeObjectId)
        methodId?.let {
            scope.methodId = CodeObjectsUtil.addMethodTypeToId(it)
        }
        scope.displayName = spanScopeInfo?.displayName ?: ""
        scope.role = spanScopeInfo?.role


        val codeLocation: CodeLocation =
            buildCodeLocation(project, scope.spanCodeObjectId, scope.displayName ?: "", scope.methodId)


        val hasErrors = tryPopulateErrors(scope)

        fireScopeChangedEvent(scope, codeLocation, hasErrors)

        val insightsStats = AnalyticsService.getInstance(project).getInsightsStats(scope.spanCodeObjectId)
        if (changeView) {
            showHomeView(insightsStats)
        }


        EDT.ensureEDT {
            MainToolWindowCardsController.getInstance(project).closeAllNotificationsIfShowing()
            MainToolWindowCardsController.getInstance(project).closeCoveringViewsIfNecessary()
            project.service<ErrorsViewOrchestrator>().closeErrorDetails()
            ToolWindowShower.getInstance(project).showToolWindow()
        }
    }


    private fun showHomeView(insightsStats: InsightsStatsResult?) {
        if (insightsStats != null) {
            if (insightsStats.analyticsInsightsCount > 0 && insightsStats.issuesInsightsCount == 0) {
                MainContentViewSwitcher.getInstance(project).showAnalytics()
                return
            }
        }

        MainContentViewSwitcher.getInstance(project).showInsights()
    }

    private fun tryPopulateErrors(scope: SpanScope): Boolean {
        return scope.methodId?.let {
            ErrorsViewService.getInstance(project).updateErrors(CodeObjectsUtil.stripMethodPrefix(it))
        } ?: ErrorsViewService.getInstance(project).empty()
    }

    private fun tryGetMethodIdForSpan(spanCodeObjectId: String): String? {
        return CodeNavigator.getInstance(project).findMethodCodeObjectId(spanCodeObjectId)
    }


    private fun fireScopeChangedEvent(
        scope: SpanScope?, codeLocation: CodeLocation, hasErrors: Boolean,
    ) {
        project.messageBus.syncPublisher(ScopeChangedEvent.SCOPE_CHANGED_TOPIC)
            .scopeChanged(scope, codeLocation, hasErrors)
    }

}
