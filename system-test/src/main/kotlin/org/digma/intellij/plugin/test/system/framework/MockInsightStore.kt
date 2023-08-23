package org.digma.intellij.plugin.test.system.framework

import io.ktor.util.reflect.typeInfo
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.CodeObjectDecorator
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.Duration
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsRequest
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse
import org.digma.intellij.plugin.model.rest.insights.MethodWithCodeObjects
import org.digma.intellij.plugin.model.rest.insights.MethodWithInsights
import org.digma.intellij.plugin.model.rest.insights.ShortDisplayInfo
import org.digma.intellij.plugin.model.rest.insights.SlowEndpointInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsPercentile
import org.digma.intellij.plugin.model.rest.insights.SpanInfo
import java.util.Date


val methodCodeObject1 = MethodWithCodeObjects(
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
            override val shortDisplayInfo: ShortDisplayInfo = ShortDisplayInfo(
                title = "Mock insight for codeObj 1 title",
                targetDisplayName = "Mock insight for codeObj 1 targetDisplayName",
                subtitle = "Mock insight for codeObj 1 subtitle",
                description = "Mock insight for codeObj 1 description"
            )
            override val type: InsightType = InsightType.SpanUsages
            override val codeObjectId: String = methodCodeObject1.codeObjectId
            override val environment: String = "env1_mock"
        },

        )
)

val methodCodeObject2 = MethodWithCodeObjects(
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
            override val shortDisplayInfo: ShortDisplayInfo = ShortDisplayInfo(
                title = "Mock insight for codeObj 2 title",
                targetDisplayName = "Mock insight for codeObj 2 targetDisplayName",
                subtitle = "Mock insight for codeObj 2 subtitle",
                description = "Mock insight for codeObj 2 description"
            )
            override val type: InsightType = InsightType.SpanUsages
            override val codeObjectId: String = methodCodeObject2.codeObjectId
            override val environment: String = "env1_mock"
        },

        )
)

val methodCodeObject3 = MethodWithCodeObjects(
    codeObjectId = "org.digma.intellij.plugin.editor.EditorEventsHandler\$_\$isRelevantFile",
    relatedSpansCodeObjectIds = listOf("relatedSpan1, relatedSpan2, relatedSpan3"),
    relatedEndpointCodeObjectIds = listOf("relatedEndpoint1, relatedEndpoint2, relatedEndpoint3")
)
val methodWithSpanDurationInsight =
    SpanDurationsInsight(
        codeObjectId = methodCodeObject3.codeObjectId,
        environment = "env1_mock",
        scope = "Span",
        importance = 1,
        actualStartTime = null,
        customStartTime = null,
        prefixedCodeObjectId = null,
        shortDisplayInfo = ShortDisplayInfo(
            title = "ShortDisplayInfo Span Duration title",
            targetDisplayName = "ShortDisplayInfo Span Duration targetDisplayName",
            subtitle = "ShortDisplayInfo Span Duration subtitle",
            description = "ShortDisplayInfo Span Duration description"
        ),
        decorators = listOf(
            CodeObjectDecorator(
                title = "Decorator - Duration Span",
                description = "Decorator 1 description"
            ),
        ),
        spanInfo = SpanInfo(
            instrumentationLibrary = "instrumentationLibrary",
            name = "mySpanName methodInsight3",
            spanCodeObjectId = "",
            displayName = "methodInsight3",
            methodCodeObjectId = methodCodeObject3.codeObjectId,
            kind = "my kind"
        ),
    )

val slowEndpointInsight = SlowEndpointInsight(
    codeObjectId = methodCodeObject3.codeObjectId,
    environment = "env1_mock",
    scope = "Endpoint",
    importance = 1,
    actualStartTime = null,
    customStartTime = null,
    prefixedCodeObjectId = null,
    shortDisplayInfo = ShortDisplayInfo(
        title = "ShortDisplayInfo Slow Endpoint title",
        targetDisplayName = "ShortDisplayInfo Slow Endpoint targetDisplayName",
        subtitle = "ShortDisplayInfo Slow Endpoint subtitle",
        description = "ShortDisplayInfo Slow Endpoint description"
    ),
    decorators = listOf(
        CodeObjectDecorator(
            title = "Decorator - Slow Endpoint",
            description = "Decorator 1 description"
        ),
    ),
    spanInfo = SpanInfo(
        instrumentationLibrary = "instrumentationLibrary",
        name = "mySpanName methodInsight3",
        spanCodeObjectId = "",
        displayName = "methodInsight3",
        methodCodeObjectId = methodCodeObject3.codeObjectId,
        kind = "my kind"
    ),
    route = "/route/subRoute/endpoint/id",
    isRecalculateEnabled = false,
    serviceName = "serviceName",
    endpointsMedian = Duration(value = 15.0, unit = "ms", raw = 15),
    endpointsMedianOfMedians = Duration(value = 15.0, unit = "ms", raw = 15),
    endpointsP75 = Duration(value = 15.0, unit = "ms", raw = 15),
    median = Duration(value = 15.0, unit = "ms", raw = 15)
)


val methodWithInsights3 = MethodWithInsights(
    methodWithIds = methodCodeObject3,
    insights = listOf(
        object : CodeObjectInsight {
            override val scope: String = "Span"
            override val importance: Int = 1
            override val decorators: List<CodeObjectDecorator> = listOf(
                CodeObjectDecorator(
                    title = "Decorator1",
                    description = "Decorator1 description"
                ),
                CodeObjectDecorator(
                    title = "Decorator2",
                    description = "Decorator2 description"
                ),
                CodeObjectDecorator(
                    title = "Decorator3",
                    description = "Decorator3 description"
                ),
            )
            override val actualStartTime: Date? = null
            override val customStartTime: Date? = null
            override val prefixedCodeObjectId: String? = null
            override val isRecalculateEnabled: Boolean = false
            override val shortDisplayInfo: ShortDisplayInfo? = ShortDisplayInfo(
                title = "ShortDisplayInfo title",
                targetDisplayName = "ShortDisplayInfo targetDisplayName",
                subtitle = "ShortDisplayInfo subtitle",
                description = "ShortDisplayInfo description"
            )
            override val type: InsightType = InsightType.HighUsage
            override val codeObjectId: String = methodCodeObject3.codeObjectId
            override val environment: String = "env1_mock"
        },
        methodWithSpanDurationInsight,
        slowEndpointInsight
    )
)


val methodCodeObject4 = MethodWithCodeObjects(
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

        )
)

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