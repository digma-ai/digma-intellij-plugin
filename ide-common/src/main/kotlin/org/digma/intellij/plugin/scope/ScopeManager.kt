package org.digma.intellij.plugin.scope

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.insights.ErrorsViewOrchestrator
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower

@Service(Service.Level.PROJECT)
class ScopeManager(private val project: Project) : Disposable {


    companion object {
        @JvmStatic
        fun getInstance(project: Project): ScopeManager {
            return project.service<ScopeManager>()
        }
    }


    override fun dispose() {
        //nothing to do, used as parent disposable
    }


    fun changeToHome() {

        fireScopeChangedEvent(null, CodeLocation(true, listOf(), listOf()))
    }

    fun changeScope(scope: Scope) {

        when (scope) {
            is SpanScope -> changeToSpanScope(scope)
        }

    }

    private fun changeToSpanScope(scope: SpanScope) {

        val spanScopeInfo = try {
            AnalyticsService.getInstance(project).getAssetDisplayInfo(scope.spanCodeObjectId)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("ScopeManager.changeToSpanScope", e)
            null
        }


        val codeLocation: CodeLocation = buildCodeLocation(project, scope.spanCodeObjectId, spanScopeInfo?.displayName ?: "")


        scope.displayName = spanScopeInfo?.displayName ?: ""
        scope.role = spanScopeInfo?.role


        EDT.ensureEDT {
            MainToolWindowCardsController.getInstance(project).closeAllNotificationsIfShowing()
            MainToolWindowCardsController.getInstance(project).closeCoveringViewsIfNecessary()
            project.service<ErrorsViewOrchestrator>().closeErrorDetails()
            ToolWindowShower.getInstance(project).showToolWindow()
            MainContentViewSwitcher.getInstance(project).showInsights()
        }

        fireScopeChangedEvent(scope, codeLocation)
    }


    private fun fireScopeChangedEvent(
        scope: SpanScope?, codeLocation: CodeLocation,
    ) {
        project.messageBus.syncPublisher(ScopeChangedEvent.SCOPE_CHANGED_TOPIC)
            .scopeChanged(scope, codeLocation)
    }

}
