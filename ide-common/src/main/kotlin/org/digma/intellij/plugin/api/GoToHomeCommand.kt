package org.digma.intellij.plugin.api

import com.intellij.openapi.project.Project
import io.netty.handler.codec.http.QueryStringDecoder
import org.digma.intellij.plugin.common.objectToJsonNode
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.scope.ScopeContext
import org.digma.intellij.plugin.scope.ScopeManager
import org.jetbrains.ide.RestService.Companion.getStringParameter
import java.util.Objects

class GoToHomeCommand : AbstractApiCommand(){

    override fun execute(project: Project, urlDecoder: QueryStringDecoder) {
        val environmentId = getStringParameter(ENVIRONMENT_ID_PARAM_NAME, urlDecoder)
        val targetTab = getStringParameter(TARGET_TAB_PARAM_NAME, urlDecoder)
        Objects.requireNonNull(environmentId, "environmentId parameter must not be null")
        Objects.requireNonNull(targetTab, "targetTab parameter must not be null")

        Log.log(
            logger::trace,
            "GoToHome called with environmentId={}, targetTab={}, projectName={}, thread={}",
            environmentId,
            targetTab,
            project.name,
            Thread.currentThread().name
        )

        val contextPayload = objectToJsonNode(GoToHomeContextPayload(targetTab!!))
        val scopeContext = ScopeContext("IDE/REST_API_CALL", contextPayload)

        ScopeManager.getInstance(project).changeToHome(true, scopeContext, environmentId)
    }


    data class GoToHomeContextPayload(val targetTab: String)
}