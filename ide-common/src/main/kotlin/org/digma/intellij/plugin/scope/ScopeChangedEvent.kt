package org.digma.intellij.plugin.scope

import com.intellij.util.messages.Topic
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation


interface ScopeChangedEvent {

    companion object {
        @JvmStatic
        @Topic.ProjectLevel
        val SCOPE_CHANGED_TOPIC: Topic<ScopeChangedEvent> = Topic.create(
            "SCOPE CHANGED",
            ScopeChangedEvent::class.java
        )
    }


    fun scopeChanged(scope: SpanScope?, codeLocation: CodeLocation)
}