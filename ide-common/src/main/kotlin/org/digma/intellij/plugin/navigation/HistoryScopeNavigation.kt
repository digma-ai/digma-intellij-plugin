package org.digma.intellij.plugin.navigation

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.LimitConcurrentStack
import org.digma.intellij.plugin.model.ModelChangeListener
import org.digma.intellij.plugin.model.nav.MiniScope
import org.digma.intellij.plugin.model.nav.ScopeType
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.EndpointScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.Scope
import org.digma.intellij.plugin.ui.service.InsightsViewService
import org.jetbrains.annotations.VisibleForTesting
import java.util.Deque


@Service(Service.Level.PROJECT)
class HistoryScopeNavigation(val project: Project) {

    private val MAX_RECENT_NAV_ITEMS = 25
    val recentScopes: Deque<MiniScope> = LimitConcurrentStack(MAX_RECENT_NAV_ITEMS)

    init {
        project.messageBus.connect(project.service<InsightsViewService>()).subscribe(
            ModelChangeListener.MODEL_CHANGED_TOPIC, ModelChangeListener {
                onScopeChanged(it.getTheScope())
            }
        )
    }

    @VisibleForTesting
    protected fun onScopeChanged(scope: Scope) {
        val peekedScope = recentScopes.peek()

        val newMiniScope = when (scope) {

            is CodeLessSpanScope -> {
                MiniScope(ScopeType.Span, scope.getSpan())
            }

            is MethodScope -> {
                MiniScope(ScopeType.Method, scope.getMethodInfo())
            }

            is EndpointScope -> {
                MiniScope(ScopeType.Endpoint, scope.getEndpoint())
            }

            else -> null
        }

        if (newMiniScope != null) {
            if (peekedScope != null) {
                pushUnique(peekedScope)
            }
            pushUnique(newMiniScope)
        }

        //debug print
        //println("onScopeChange recentScopes = ${recentScopes}")
    }

    fun pushUnique(miniScope: MiniScope) {
        val peekedValue = recentScopes.peek()
        if (peekedValue != miniScope) {
            recentScopes.push(miniScope)
        }
    }

    fun getPreviousScopeToGoBackTo(): MiniScope? {
        val currentScope = recentScopes.peek()
        if (currentScope == null) return null

        do {
            val candidateScope = recentScopes.pop()
            if (candidateScope != currentScope) {
                return candidateScope
            }
        } while (!recentScopes.isEmpty())

        return null
    }

}