package org.digma.intellij.plugin.api

import com.intellij.openapi.project.Project
import io.netty.handler.codec.http.QueryStringDecoder
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.objectToJsonNode
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.scope.ScopeContext
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import org.jetbrains.ide.RestService.Companion.getStringParameter
import java.util.Objects

class GoToSpanCommand : AbstractApiCommand() {

    override fun execute(project: Project, urlDecoder: QueryStringDecoder) {
        val spanUid = getStringParameter(SPAN_UID_PARAM_NAME, urlDecoder)
        val targetTab = getStringParameter(TARGET_TAB_PARAM_NAME, urlDecoder)
        Objects.requireNonNull(spanUid, "spanUid parameter must not be null")
        Objects.requireNonNull(targetTab, "targetTab parameter must not be null")

        Log.log(
            logger::trace,
            "GoToSpan called with spanUid={}, targetTab={}, projectName={}, thread={}",
            spanUid,
            targetTab,
            project.name,
            Thread.currentThread().name
        )

        //checked non-null above
        spanUid!!

        val spanInfoByUid = AnalyticsService.getInstance(project).resolveSpanByUid(spanUid)
            ?: throw RuntimeException("could not find span by uid $spanUid")

        val scope = SpanScope(spanInfoByUid.spanCodeObjectId)
        val contextPayload = objectToJsonNode(GoToSpanScopeContextPayload(targetTab!!))
        val scopeContext = ScopeContext("IDE/REST_API_CALL", contextPayload)

        Log.log(
            logger::trace,
            "calling ScopeManager.changeScope with scope='{}',scopeContext='{}',environmentId='{}', thread='{}'",
            scope,
            scopeContext,
            spanInfoByUid.environmentId,
            Thread.currentThread().name
        )

        ScopeManager.getInstance(project).changeScope(scope, scopeContext, spanInfoByUid.environmentId)

    }


    data class GoToSpanScopeContextPayload(val targetTab: String)

}