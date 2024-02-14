package org.digma.intellij.plugin.scope

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.code.CodeDetails

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


    fun changeScope(scope: Scope) {

        when (scope) {
            is SpanScope -> changeToSpanScope(scope)
        }

    }

    private fun changeToSpanScope(scope: SpanScope) {

        fireScopeChangedEvent(scope, true, listOf(), listOf())
    }


    private fun fireScopeChangedEvent(
        scope: SpanScope,
        isAlreadyAtCode: Boolean,
        codeDetailsList: List<CodeDetails>,
        relatedCodeDetailsList: List<CodeDetails>,
    ) {
        project.messageBus.syncPublisher(ScopeChangedEvent.SCOPE_CHANGED_TOPIC)
            .scopeChanged(scope, isAlreadyAtCode, codeDetailsList, relatedCodeDetailsList)
    }


}
