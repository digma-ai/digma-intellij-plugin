package org.digma.intellij.plugin.api

import com.intellij.openapi.project.Project
import io.netty.handler.codec.http.QueryStringDecoder
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.backendVersionOlderThen
import org.digma.intellij.plugin.common.objectToJsonNode
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.scope.ScopeContext
import org.digma.intellij.plugin.scope.ScopeManager
import org.jetbrains.ide.RestService.Companion.getStringParameter
import java.util.Objects

class GoToErrorCommand : AbstractApiCommand() {

    override suspend fun execute(project: Project, urlDecoder: QueryStringDecoder) {

        if (backendVersionOlderThen(project,"0.3.318")){
            throw RuntimeException("This api is not supported in this version of backend")
        }

        val errorId = getStringParameter(ERROR_ID_PARAM_NAME, urlDecoder)
        Objects.requireNonNull(errorId, "errorId parameter must not be null")

        Log.log(
            logger::trace,
            "GoToError called with errorId={}, projectName={}, thread={}",
            errorId,
            project.name,
            Thread.currentThread().name
        )

        //checked non-null above
        errorId!!

        val environmentByErrorIdResponse = AnalyticsService.getInstance(project).resolveEnvironmentByErrorId(errorId)
            ?: throw RuntimeException("could not find environment by error id $errorId")

        val environmentId = environmentByErrorIdResponse.environmentId
        if (environmentId.isBlank()) {
            throw RuntimeException("could not find environment id for error id $errorId")
        }
        val contextPayload = objectToJsonNode(GoToErrorContextPayload(targetTabPath = errorId))
        val scopeContext = ScopeContext("IDE/REST_API_CALL", contextPayload)

        Log.log(
            logger::trace,
            "calling ScopeManager.changeToHome with ,scopeContext='{}',environmentId='{}', thread='{}'",
            scopeContext,
            environmentId,
            Thread.currentThread().name
        )

        ScopeManager.getInstance(project).changeToHome(true, scopeContext, environmentId)

    }


    data class GoToErrorContextPayload(val targetTab: String = "errors", val targetTabPath: String)

}