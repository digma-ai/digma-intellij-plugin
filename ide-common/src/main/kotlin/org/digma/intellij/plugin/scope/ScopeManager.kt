package org.digma.intellij.plugin.scope

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.setCurrentEnvironmentById
import org.digma.intellij.plugin.common.CodeObjectsUtil
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_HIGH_TRY_FIX
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.navigation.View
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower

@Service(Service.Level.PROJECT)
class ScopeManager(private val project: Project) {

    private val logger = Logger.getInstance(this::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ScopeManager {
            return project.service<ScopeManager>()
        }
    }

    fun changeToHome(
        isCalledFromReact: Boolean = false,
        forceNavigation: Boolean = false,
        scopeContext: ScopeContext? = null,
        environmentId: String? = null
    ) {

        EDT.assertNonDispatchThread()

        ActivityMonitor.getInstance(project).registerScopeChanged("home")

        //must happen before firing the event
        if (!environmentId.isNullOrBlank()){
            setCurrentEnvironmentById(project,environmentId)
        }

        fireScopeChangedEvent(null, CodeLocation(listOf(), listOf()), false, scopeContext,environmentId)

        if (!forceNavigation) {
            val contentViewSwitcher = MainContentViewSwitcher.getInstance(project)
            if (contentViewSwitcher.getSelectedView() != View.Assets) {
                contentViewSwitcher.showInsights()
            }
        }


        EDT.ensureEDT {
            //don't do that on first wizard launch to let user complete the installation wizard.
            if (!PersistenceService.getInstance().isFirstWizardLaunch()) {
                MainToolWindowCardsController.getInstance(project).closeAllNotificationsIfShowing()
                MainToolWindowCardsController.getInstance(project).closeCoveringViewsIfNecessary()

                if (!isCalledFromReact) {
                    // if react called changeToHome it's ok not to show the tool window, usually its on connection events.
                    ToolWindowShower.getInstance(project).showToolWindow()
                }
            }
        }

    }

    //preferredView is the preferred view to show after changing scope.
    fun changeScope(
        scope: Scope,
        changeView: Boolean = true,
        preferredView: View? = null,
        scopeContext: ScopeContext? = null,
        environmentId: String? = null
    ) {

        EDT.assertNonDispatchThread()

        ActivityMonitor.getInstance(project).registerScopeChanged(scope.toString())


        try {
            when (scope) {
                is SpanScope -> changeToSpanScope(scope, changeView, preferredView, scopeContext, environmentId)

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


    private fun changeToSpanScope(
        scope: SpanScope,
        changeView: Boolean = true,
        preferredView: View? = null,
        scopeContext: ScopeContext? = null,
        environmentId: String? = null
    ) {

        //must happen before anything else
        if (!environmentId.isNullOrBlank()){
            setCurrentEnvironmentById(project,environmentId)
        }

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


        //todo: this is temporary, the plugin loads the errors just so that the UI can show a red dot.
        // it should be removed at some point
        val hasErrors = checkIfHasErrors(scope)



        fireScopeChangedEvent(scope, codeLocation, hasErrors, scopeContext, environmentId)

        if (changeView) {
            changeViewAfterScopeChange(scope, preferredView)
        }

        EDT.ensureEDT {
            MainToolWindowCardsController.getInstance(project).closeAllNotificationsIfShowing()
            MainToolWindowCardsController.getInstance(project).closeCoveringViewsIfNecessary()
            ToolWindowShower.getInstance(project).showToolWindow()
        }

    }

    private fun checkIfHasErrors(scope: SpanScope): Boolean {
        val objectIds = listOfNotNull(scope.spanCodeObjectId, scope.methodId)
        val errors = AnalyticsService.getInstance(project).getErrors(objectIds)
        return !errors.isNullOrBlank() && errors != "[]"
    }


    private fun changeViewAfterScopeChange(scope: SpanScope, preferredView: View?) {

        //in both these cases, if there are no insights, show analytics
        if (preferredView == null || preferredView == View.Insights) {
            val insightsStats = AnalyticsService.getInstance(project).getInsightsStats(scope.spanCodeObjectId)
            if (insightsStats.issuesInsightsCount == 0) {
                MainContentViewSwitcher.getInstance(project).showAnalytics()
            } else {
                MainContentViewSwitcher.getInstance(project).showInsights()
            }
        } else {
            MainContentViewSwitcher.getInstance(project).showView(preferredView)
        }
    }


    private fun tryGetMethodIdForSpan(spanCodeObjectId: String): String? {
        return CodeNavigator.getInstance(project).findMethodCodeObjectId(spanCodeObjectId)
    }


    private fun fireScopeChangedEvent(
        scope: SpanScope?, codeLocation: CodeLocation, hasErrors: Boolean, scopeContext: ScopeContext?, environmentId: String?,
    ) {
        project.messageBus.syncPublisher(ScopeChangedEvent.SCOPE_CHANGED_TOPIC)
            .scopeChanged(scope, codeLocation, hasErrors, scopeContext,environmentId)
    }

}
