package org.digma.intellij.plugin.test.system.framework

import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.CodeObjectDecorator
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsRequest
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse
import org.digma.intellij.plugin.model.rest.insights.MethodWithCodeObjects
import org.digma.intellij.plugin.model.rest.insights.MethodWithInsights
import org.digma.intellij.plugin.model.rest.insights.ShortDisplayInfo
import java.util.Date

val EnvironmentListMock = listOf(
    "env1_mock",
    "env2_mock",
)


val methodCodeObject1 =  MethodWithCodeObjects(
    codeObjectId = "org.digma.intellij.plugin.editor.EditorEventsHandler\$_\$isFileNotChangingContext",
    relatedSpansCodeObjectIds = listOf("relatedSpan1, relatedSpan2, relatedSpan3"),
    relatedEndpointCodeObjectIds = listOf("relatedEndpoint1, relatedEndpoint2, relatedEndpoint3")
)

val methodWithInsights1 = MethodWithInsights(
    methodWithIds = methodCodeObject1,
    insights = listOf(
        object : CodeObjectInsight {
            override val scope: String = "Span"
            override val importance: Int = 1
            override val decorators: List<CodeObjectDecorator>? = null
            override val actualStartTime: Date? = null
            override val customStartTime: Date? = null
            override val prefixedCodeObjectId: String? = null
            override val isRecalculateEnabled: Boolean = false
            override val shortDisplayInfo: ShortDisplayInfo? = null
            override val type: InsightType = InsightType.SpanUsages
            override val codeObjectId: String = methodCodeObject1.codeObjectId
            override val environment: String = "env1_mock"
        },

))

val methodCodeObject2 =  MethodWithCodeObjects(
    codeObjectId = "org.digma.intellij.plugin.editor.EditorEventsHandler\$_\$selectionChanged, ",
    relatedSpansCodeObjectIds = listOf("relatedSpan1, relatedSpan2, relatedSpan3"),
    relatedEndpointCodeObjectIds = listOf("relatedEndpoint1, relatedEndpoint2, relatedEndpoint3")
)

val methodWithInsights2 = MethodWithInsights(
    methodWithIds = methodCodeObject2,
    insights = listOf(
        object : CodeObjectInsight {
            override val scope: String = "Span"
            override val importance: Int = 1
            override val decorators: List<CodeObjectDecorator>? = null
            override val actualStartTime: Date? = null
            override val customStartTime: Date? = null
            override val prefixedCodeObjectId: String? = null
            override val isRecalculateEnabled: Boolean = false
            override val shortDisplayInfo: ShortDisplayInfo? = null
            override val type: InsightType = InsightType.SpanUsages
            override val codeObjectId: String = methodCodeObject2.codeObjectId
            override val environment: String = "env1_mock"
        },

        ))

val methodCodeObject3 =  MethodWithCodeObjects(
    codeObjectId = "org.digma.intellij.plugin.editor.EditorEventsHandler\$_\$isRelevantFile",
    relatedSpansCodeObjectIds = listOf("relatedSpan1, relatedSpan2, relatedSpan3"),
    relatedEndpointCodeObjectIds = listOf("relatedEndpoint1, relatedEndpoint2, relatedEndpoint3")
)
val methodWithInsights3 = MethodWithInsights(
    methodWithIds = methodCodeObject3,
    insights = listOf(
        object : CodeObjectInsight {
            override val scope: String = "Span"
            override val importance: Int = 1
            override val decorators: List<CodeObjectDecorator>? = null
            override val actualStartTime: Date? = null
            override val customStartTime: Date? = null
            override val prefixedCodeObjectId: String? = null
            override val isRecalculateEnabled: Boolean = false
            override val shortDisplayInfo: ShortDisplayInfo? = null
            override val type: InsightType = InsightType.SpanUsages
            override val codeObjectId: String = methodCodeObject3.codeObjectId
            override val environment: String = "env1_mock"
        },

        ))

val methodCodeObject4 =  MethodWithCodeObjects(
    codeObjectId = "org.digma.intellij.plugin.editor.EditorEventsHandler\$_\$isFileChangingContext",
    relatedSpansCodeObjectIds = listOf("relatedSpan1, relatedSpan2, relatedSpan3"),
    relatedEndpointCodeObjectIds = listOf("relatedEndpoint1, relatedEndpoint2, relatedEndpoint3")
)

val methodWithInsights4 = MethodWithInsights(
    methodWithIds = methodCodeObject4,
    insights = listOf(
        object : CodeObjectInsight {
            override val scope: String = "Span"
            override val importance: Int = 1
            override val decorators: List<CodeObjectDecorator>? = null
            override val actualStartTime: Date? = null
            override val customStartTime: Date? = null
            override val prefixedCodeObjectId: String? = null
            override val isRecalculateEnabled: Boolean = false
            override val shortDisplayInfo: ShortDisplayInfo? = null
            override val type: InsightType = InsightType.SpanUsages
            override val codeObjectId: String = methodCodeObject4.codeObjectId
            override val environment: String = "env1_mock"
        },

        ))

val MockInsightsOfMethodsRequestFactory: (String) -> InsightsOfMethodsRequest = { env: String ->
    InsightsOfMethodsRequest(
        environment = env,
        methods = listOf(
            methodCodeObject1,
            methodCodeObject2,
            methodCodeObject3,
            methodCodeObject4
        )
    )
}

val MockInsightsOfMethodsResponseFactory: (String) -> InsightsOfMethodsResponse = { env: String ->
    InsightsOfMethodsResponse(
        environment = env,
        methodsWithInsights = listOf(
            methodWithInsights1,
            methodWithInsights2,
            methodWithInsights3,
            methodWithInsights4,
        )
    )
}