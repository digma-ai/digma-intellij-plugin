package org.digma.intellij.plugin.scope

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.setCurrentEnvironmentById
import org.digma.intellij.plugin.common.CodeObjectsUtil
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.persistence.PersistenceService
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

    suspend fun changeToHome(
        openMainPanel: Boolean = false,
        scopeContext: ScopeContext? = null,
        environmentId: String? = null
    ) {

        EDT.assertNonDispatchThread()

        //must happen before firing the event
        if (!environmentId.isNullOrBlank()) {
            setCurrentEnvironmentById(project, environmentId)
        }

        fireScopeChangedEvent(null, CodeLocation(listOf(), listOf()), false, scopeContext, environmentId)

        withContext(Dispatchers.EDT) {
            //don't do that on first wizard launch to let user complete the installation wizard.
            if (!PersistenceService.getInstance().isFirstWizardLaunch()) {
                MainToolWindowCardsController.getInstance(project).closeCoveringViewsIfNecessary()

                if (openMainPanel) {
                    // if react called changeToHome it's ok not to show the tool window, usually its on connection events.
                    ToolWindowShower.getInstance(project).showToolWindow()
                }
            }
        }

    }

    suspend fun changeScope(
        scope: Scope,
        scopeContext: ScopeContext? = null,
        environmentId: String? = null
    ) {

        EDT.assertNonDispatchThread()

//        try {
        when (scope) {
            is SpanScope -> changeToSpanScope(scope, scopeContext, environmentId)

            else -> {
                ErrorReporter.getInstance().reportError(
                    project, "ScopeManager.changeScope", "changeScope,unknown scope", mapOf(
                        "error hint" to "got unknown scope $scope"
                    )
                )
            }
        }
//        } catch (e: Throwable) {
//            ErrorReporter.getInstance().reportError(project, "ScopeManager.changeScope", e)
//        }
    }


    private suspend fun changeToSpanScope(
        scope: SpanScope,
        scopeContext: ScopeContext? = null,
        environmentId: String? = null
    ) {

        //must happen before anything else
        if (!environmentId.isNullOrBlank()) {
            setCurrentEnvironmentById(project, environmentId)
        }

        val spanScopeInfo = try {
            AnalyticsService.getInstance(project).getAssetDisplayInfo(scope.spanCodeObjectId)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ScopeManager.changeToSpanScope.getAssetDisplayInfo", e)
            null
        }

        //maybe the scope has methodId already, if not try to find it.
        //in any case add a method type to scope.methodId.
        val methodId = scope.methodId ?: spanScopeInfo?.methodCodeObjectId ?: tryGetMethodIdForSpan(scope.spanCodeObjectId)
        scope.methodId = methodId?.let { CodeObjectsUtil.addMethodTypeToId(it) }
        scope.displayName = spanScopeInfo?.displayName ?: ""
        scope.role = spanScopeInfo?.role


        //never let buildCodeLocation fail this method, this method must finish
        val codeLocation: CodeLocation = try {
            buildCodeLocation(project, scope.spanCodeObjectId, scope.displayName ?: "", scope.methodId)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "ScopeManager.changeToSpanScope.buildCodeLocation", e)
            CodeLocation()
        }


        //todo: this is temporary, the plugin loads the errors just so that the UI can show a red dot.
        // it should be removed at some point
        val hasErrors = checkIfHasErrors(scope)


        fireScopeChangedEvent(scope, codeLocation, hasErrors, scopeContext, environmentId)

        withContext(Dispatchers.EDT) {
            MainToolWindowCardsController.getInstance(project).closeCoveringViewsIfNecessary()
            ToolWindowShower.getInstance(project).showToolWindow()
        }

    }

    private fun checkIfHasErrors(scope: SpanScope): Boolean {
        return try {
            val objectIds = listOfNotNull(scope.spanCodeObjectId, scope.methodId)
            val errors = AnalyticsService.getInstance(project).getErrors(objectIds)
            !errors.isNullOrBlank() && errors != "[]"
        } catch (e: Throwable) {
            false
        }
    }


    private suspend fun tryGetMethodIdForSpan(spanCodeObjectId: String): String? {
        return CodeNavigator.getInstance(project).findMethodCodeObjectId(spanCodeObjectId)
    }


    private fun fireScopeChangedEvent(
        scope: SpanScope?, codeLocation: CodeLocation, hasErrors: Boolean, scopeContext: ScopeContext?, environmentId: String?,
    ) {
        project.messageBus.syncPublisher(ScopeChangedEvent.SCOPE_CHANGED_TOPIC)
            .scopeChanged(scope, codeLocation, hasErrors, scopeContext, environmentId)
    }


}
