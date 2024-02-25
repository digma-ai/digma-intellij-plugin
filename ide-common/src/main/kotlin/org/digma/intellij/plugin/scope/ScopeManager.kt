package org.digma.intellij.plugin.scope

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.CodeObjectsUtil
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_HIGH_TRY_FIX
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.service.ErrorsViewOrchestrator
import org.digma.intellij.plugin.ui.service.ErrorsViewService

@Service(Service.Level.PROJECT)
class ScopeManager(private val project: Project) {


    companion object {
        @JvmStatic
        fun getInstance(project: Project): ScopeManager {
            return project.service<ScopeManager>()
        }
    }

    fun changeToHome() {

        EDT.assertNonDispatchThread()

        ErrorsViewService.getInstance(project).empty()

        EDT.ensureEDT {
            MainToolWindowCardsController.getInstance(project).closeAllNotificationsIfShowing()
            MainToolWindowCardsController.getInstance(project).closeCoveringViewsIfNecessary()
            project.service<ErrorsViewOrchestrator>().closeErrorDetails()
            ToolWindowShower.getInstance(project).showToolWindow()
            MainContentViewSwitcher.getInstance(project).showInsights()
        }

        fireScopeChangedEvent(null, CodeLocation(listOf(), listOf()), false)
    }

    fun changeScope(scope: Scope) {

        EDT.assertNonDispatchThread()

        try {
            when (scope) {
                is SpanScope -> changeToSpanScope(scope)

                else -> {
                    ErrorReporter.getInstance().reportError(
                        project, "ScopeManager.changeScope", mapOf(
                            "error" to "got unknown scope $scope",
                            SEVERITY_PROP_NAME to SEVERITY_HIGH_TRY_FIX
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ScopeManager.changeScope", e)
        }

    }

    private fun changeToSpanScope(scope: SpanScope) {

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


        EDT.ensureEDT {
            MainToolWindowCardsController.getInstance(project).closeAllNotificationsIfShowing()
            MainToolWindowCardsController.getInstance(project).closeCoveringViewsIfNecessary()
            project.service<ErrorsViewOrchestrator>().closeErrorDetails()
            ToolWindowShower.getInstance(project).showToolWindow()
            MainContentViewSwitcher.getInstance(project).showInsights()
        }

        val hasErrors = tryPopulateErrors(scope)

        fireScopeChangedEvent(scope, codeLocation, hasErrors)


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
