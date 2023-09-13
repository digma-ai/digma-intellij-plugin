package org.digma.intellij.plugin.test.system

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.model.rest.insights.CodeObjectDecorator
import org.digma.intellij.plugin.model.rest.insights.Duration
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanResponse
import org.digma.intellij.plugin.model.rest.insights.MethodWithCodeObjects
import org.digma.intellij.plugin.model.rest.insights.MethodWithInsights
import org.digma.intellij.plugin.model.rest.insights.ShortDisplayInfo
import org.digma.intellij.plugin.model.rest.insights.SlowEndpointInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsInsight
import org.digma.intellij.plugin.model.rest.insights.SpanInfo
import org.digma.intellij.plugin.model.rest.recentactivity.EntrySpan
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityEntrySpanPayload
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.model.rest.recentactivity.SlimAggregatedInsight
import org.digma.intellij.plugin.test.system.framework.methodCodeObject3
import java.util.Date


val logger = Logger.getInstance("SoWMockDataStore")


fun createDigmaCodeObjectId(className: String, methodName: String): String {
    return "$className\$_\$$methodName"
}

object BulletOneData {

    const val DOC_NAME: String = "TestFile.java"
    val environmentList = listOf("env1")

    val className: String = "org.digma.intellij.plugin.test.system.TestFile"
    val methodName = "method1"
    val methodCodeObjectId: String = createDigmaCodeObjectId(className, methodName)
    val relatedSpansToMethod: List<String> = listOf(
        createDigmaCodeObjectId(className, "relatedSpan1"),
        createDigmaCodeObjectId(className, "relatedSpan2")
    )
    val relatedEndPointToMethod: List<String> = listOf(
        createDigmaCodeObjectId(className, "relatedEndpoint1"),
        createDigmaCodeObjectId(className, "relatedEndpoint2")
    )
    val expectedInsightsOfMethods: InsightsOfMethodsResponse
        get() {
            val methodWithCodeObjects = MethodWithCodeObjects(
                codeObjectId = methodCodeObjectId,
                relatedSpansCodeObjectIds = relatedSpansToMethod,
                relatedEndpointCodeObjectIds = relatedEndPointToMethod
            )

            // insights of method - method1

            //slowEndpoint Insight

            val slowEndpointInsight = SlowEndpointInsight(
                codeObjectId = methodWithCodeObjects.codeObjectId,
                environment = environmentList[0],
                scope = "scope1",
                importance = 1,
                actualStartTime = null,
                customStartTime = null,
                prefixedCodeObjectId = null,
                shortDisplayInfo = ShortDisplayInfo(
                    title = "Slow endpoint title for $methodName",
                    targetDisplayName = "target $methodName",
                    subtitle = "subtitle for $methodName",
                    description = "desctioption for $methodName"
                ),
                decorators = listOf(
                    CodeObjectDecorator(
                        title = "decorator title of method $methodName",
                        description = "decorator description of method $methodName"
                    ),

                    ),
                spanInfo = SpanInfo(
                    instrumentationLibrary = "instrumentationLibrary",
                    name = "span of Method $methodName",
                    spanCodeObjectId = relatedSpansToMethod[0],
                    displayName = "span display name of method $methodName",
                    methodCodeObjectId = methodWithCodeObjects.codeObjectId,
                    kind = "Slow Endpoint Kind"
                ),
                route = "route/subRoute/endpoint/id",
                isRecalculateEnabled = false,
                serviceName = "test ServiceName",
                endpointsMedian = Duration(value = 15.0, unit = "ms", raw = 15),
                endpointsMedianOfMedians = Duration(value = 20.0, unit = "ms", raw = 20),
                endpointsP75 = Duration(value = 25.0, unit = "ms", raw = 25),
                median = Duration(value = 15.0, unit = "ms", raw = 15)
            )

            // Span Duration Insight
            val spanDurationsInsight = SpanDurationsInsight(
                codeObjectId = methodWithCodeObjects.codeObjectId,
                environment = environmentList[0],
                scope = "scope1",
                importance = 1,
                actualStartTime = null,
                customStartTime = null,
                prefixedCodeObjectId = null,
                shortDisplayInfo = ShortDisplayInfo(
                    title = "Span duration title for $methodName",
                    targetDisplayName = "target $methodName",
                    subtitle = "subtitle for $methodName",
                    description = "desctioption for $methodName"
                ),
                decorators = listOf(
                    CodeObjectDecorator(
                        title = "Decorator - Span Duration of $methodName",
                        description = "Decorator 1 Span Duration of  $methodName"
                    ),
                ),
                spanInfo = SpanInfo(
                    instrumentationLibrary = "instrumentationLibrary",
                    name = "mySpanName methodInsight3",
                    spanCodeObjectId = methodWithCodeObjects.relatedSpansCodeObjectIds[0],
                    displayName = "methodInsight3",
                    methodCodeObjectId = methodWithCodeObjects.codeObjectId,
                    kind = "my kind"
                )
            )


            val methodWithSlowEndPointInsight = MethodWithInsights(
                methodWithIds = methodWithCodeObjects,
                insights = listOf(
                    slowEndpointInsight,
                    spanDurationsInsight
                )
            )

            return InsightsOfMethodsResponse(
                environment = environmentList[0],
                methodsWithInsights = listOf(
                    methodWithSlowEndPointInsight
                )
            )
        }

    val expectedRecentActivityResult: RecentActivityResult
        get() {

            val slimAggregatedInsight1 = SlimAggregatedInsight(
                type = "slim Insight 1 type",
                importance = 2,
                codeObjectIds = listOf(
                    methodCodeObjectId,
                    *relatedEndPointToMethod.toTypedArray()
                )
            )

            val slimAggregatedInsight2 = SlimAggregatedInsight(
                type = "slim Insight 2 type",
                importance = 2,
                codeObjectIds = listOf(
                    methodCodeObjectId,
                    *relatedSpansToMethod.toTypedArray(),
                )
            )

            val expectedRecentActivityResponseEntries = listOf(
                RecentActivityResponseEntry(
                    environment = environmentList[0],
                    traceFlowDisplayName = " Trace Flow DisplayName for entry 1",
                    firstEntrySpan = EntrySpan(
                        displayText = "First Span Entry",
                        serviceName = "test serviceName",
                        scopeId = "method1 span ID", // todo: check what should be there
                        spanCodeObjectId = relatedSpansToMethod[0],
                        methodCodeObjectId = methodCodeObjectId
                    ),
                    lastEntrySpan = null,
                    latestTraceId = "traceId_method1",
                    latestTraceTimestamp = Date(),
                    latestTraceDuration = Duration(1.1, "ms", 1100),
                    slimAggregatedInsights = listOf(
                        slimAggregatedInsight1,
                        slimAggregatedInsight2
                    ),
                ),
            )

            return RecentActivityResult(
                accountId = "testAccount@test.com",
                entries = expectedRecentActivityResponseEntries
            )
        }
}

object BulletTwoData {
    const val DOC_NAME: String = "TestFile.java"
    val environmentList = listOf("env1", "env2")

    val className: String = "org.digma.intellij.plugin.test.system.TestFile"
    val methodName = "method1"
    val methodCodeObjectId: String = createDigmaCodeObjectId(className, methodName)
    val relatedSpansToMethod: List<String> = listOf(
        createDigmaCodeObjectId(className, "relatedSpan1"),
        createDigmaCodeObjectId(className, "relatedSpan2")
    )
    val relatedEndPointToMethod: List<String> = listOf(
        createDigmaCodeObjectId(className, "relatedEndpoint1"),
        createDigmaCodeObjectId(className, "relatedEndpoint2")
    )

    val expectedRecentActivityResult: RecentActivityResult
        get() {

            val slimAggregatedInsight1 = SlimAggregatedInsight(
                type = "slim Insight 1 type",
                importance = 2,
                codeObjectIds = listOf(
                    this.methodCodeObjectId,
                    *this.relatedEndPointToMethod.toTypedArray()
                )
            )

            val slimAggregatedInsight2 = SlimAggregatedInsight(
                type = "slim Insight 2 type",
                importance = 2,
                codeObjectIds = listOf(
                    methodCodeObjectId,
                    *relatedSpansToMethod.toTypedArray(),
                )
            )

            val slimAggregatedInsight1Env2 = SlimAggregatedInsight(
                type = "slim Insight 1 type",
                importance = 2,
                codeObjectIds = listOf(
                    this.methodCodeObjectId,
                    *this.relatedEndPointToMethod.toTypedArray()
                )
            )

            val slimAggregatedInsight2Env2 = SlimAggregatedInsight(
                type = "slim Insight 2 type",
                importance = 2,
                codeObjectIds = listOf(
                    methodCodeObjectId,
                    *relatedSpansToMethod.toTypedArray(),
                )
            )

            val expectedRecentActivityResponseEntries = listOf(
                RecentActivityResponseEntry(
                    environment = environmentList[0],
                    traceFlowDisplayName = " Trace Flow DisplayName for entry 1",
                    firstEntrySpan = EntrySpan(
                        displayText = "First Span Entry",
                        serviceName = "test serviceName",
                        scopeId = "method1 span ID", // todo: check what should be there
                        spanCodeObjectId = relatedSpansToMethod[0],
                        methodCodeObjectId = methodCodeObjectId
                    ),
                    lastEntrySpan = null,
                    latestTraceId = "traceId_method1",
                    latestTraceTimestamp = Date(),
                    latestTraceDuration = Duration(1.1, "ms", 1100),
                    slimAggregatedInsights = listOf(
                        slimAggregatedInsight1,
                        slimAggregatedInsight2
                    ),
                ),
                RecentActivityResponseEntry(
                    environment = environmentList[1],
                    traceFlowDisplayName = " Trace Flow DisplayName for entry 2 -- env2",
                    firstEntrySpan = EntrySpan(
                        displayText = "First Span Entry -- env2",
                        serviceName = "test serviceName",
                        scopeId = "span_endpoint1", // todo: check what should be there
                        spanCodeObjectId = relatedSpansToMethod[0],
                        methodCodeObjectId = methodCodeObjectId
                    ),
                    lastEntrySpan = null,
                    latestTraceId = "traceId_method1_env2",
                    latestTraceTimestamp = Date(),
                    latestTraceDuration = Duration(1.1, "ms", 1100),
                    slimAggregatedInsights = listOf(
                        slimAggregatedInsight1Env2,
                        slimAggregatedInsight2Env2
                    ),
                )
            )

            return RecentActivityResult(
                accountId = "testAccount@test.com",
                entries = expectedRecentActivityResponseEntries
            )
        }
}

object BulletThreeData {

    val environmentList = listOf("env1", "env2")

    val className: String = "org.digma.intellij.plugin.test.system.TestFile"
    val methodName = "method1"
    val methodCodeObjectId: String = BulletTwoData.methodCodeObjectId
    val relatedSpansToMethod: List<String> = BulletTwoData.relatedSpansToMethod
    val relatedEndPointToMethod: List<String> = BulletTwoData.relatedEndPointToMethod

    val expectedInsightsOfMethods: InsightsOfMethodsResponse
        get() {
            val methodWithCodeObjects = MethodWithCodeObjects(
                codeObjectId = methodCodeObjectId,
                relatedSpansCodeObjectIds = relatedSpansToMethod,
                relatedEndpointCodeObjectIds = relatedEndPointToMethod
            )

            // insights of method - method1

            //slowEndpoint Insight

            val slowEndpointInsight = SlowEndpointInsight(
                codeObjectId = methodWithCodeObjects.codeObjectId,
                environment = environmentList[0],
                scope = "scope1",
                importance = 1,
                actualStartTime = null,
                customStartTime = null,
                prefixedCodeObjectId = null,
                shortDisplayInfo = ShortDisplayInfo(
                    title = "Slow endpoint title for ${methodName}",
                    targetDisplayName = "target ${methodName}",
                    subtitle = "subtitle for ${methodName}",
                    description = "desctioption for ${methodName}"
                ),
                decorators = listOf(
                    CodeObjectDecorator(
                        title = "decorator title of method ${methodName}",
                        description = "decorator description of method ${methodName}"
                    ),

                    ),
                spanInfo = SpanInfo(
                    instrumentationLibrary = "instrumentationLibrary",
                    name = "span of Method ${methodName}",
                    spanCodeObjectId = relatedSpansToMethod[0],
                    displayName = "span display name of method ${methodName}",
                    methodCodeObjectId = methodWithCodeObjects.codeObjectId,
                    kind = "Slow Endpoint Kind"
                ),
                route = "route/subRoute/endpoint/id",
                isRecalculateEnabled = false,
                serviceName = "test ServiceName",
                endpointsMedian = Duration(value = 15.0, unit = "ms", raw = 15),
                endpointsMedianOfMedians = Duration(value = 20.0, unit = "ms", raw = 20),
                endpointsP75 = Duration(value = 25.0, unit = "ms", raw = 25),
                median = Duration(value = 15.0, unit = "ms", raw = 15)
            )

            val slowEndpointInsightEnv2 = SlowEndpointInsight(
                codeObjectId = methodWithCodeObjects.codeObjectId,
                environment = environmentList[1],
                scope = "scope1",
                importance = 1,
                actualStartTime = null,
                customStartTime = null,
                prefixedCodeObjectId = null,
                shortDisplayInfo = ShortDisplayInfo(
                    title = "Slow endpoint title for ${methodName}",
                    targetDisplayName = "target ${methodName}",
                    subtitle = "subtitle for ${methodName}",
                    description = "desctioption for ${methodName}"
                ),
                decorators = listOf(
                    CodeObjectDecorator(
                        title = "decorator title of method ${methodName}",
                        description = "decorator description of method ${methodName}"
                    ),

                    ),
                spanInfo = SpanInfo(
                    instrumentationLibrary = "instrumentationLibrary",
                    name = "span of Method ${methodName}",
                    spanCodeObjectId = relatedSpansToMethod[0],
                    displayName = "span display name of method ${methodName}",
                    methodCodeObjectId = methodWithCodeObjects.codeObjectId,
                    kind = "Slow Endpoint Kind"
                ),
                route = "route/subRoute/endpoint/id",
                isRecalculateEnabled = false,
                serviceName = "test ServiceName",
                endpointsMedian = Duration(value = 15.0, unit = "ms", raw = 15),
                endpointsMedianOfMedians = Duration(value = 20.0, unit = "ms", raw = 20),
                endpointsP75 = Duration(value = 25.0, unit = "ms", raw = 25),
                median = Duration(value = 15.0, unit = "ms", raw = 15)
            )

            // Span Duration Insight
            val spanDurationsInsight = SpanDurationsInsight(
                codeObjectId = methodWithCodeObjects.codeObjectId,
                environment = environmentList[0],
                scope = "scope1",
                importance = 1,
                actualStartTime = null,
                customStartTime = null,
                prefixedCodeObjectId = null,
                shortDisplayInfo = ShortDisplayInfo(
                    title = "Span duration title for ${methodName}",
                    targetDisplayName = "target ${methodName}",
                    subtitle = "subtitle for ${methodName}",
                    description = "desctioption for ${methodName}"
                ),
                decorators = listOf(
                    CodeObjectDecorator(
                        title = "Decorator - Span Duration of ${methodName}",
                        description = "Decorator 1 Span Duration of  ${methodName}"
                    ),
                ),
                spanInfo = SpanInfo(
                    instrumentationLibrary = "instrumentationLibrary",
                    name = "mySpanName methodInsight3",
                    spanCodeObjectId = methodWithCodeObjects.relatedSpansCodeObjectIds[0],
                    displayName = "methodInsight3",
                    methodCodeObjectId = methodWithCodeObjects.codeObjectId,
                    kind = "my kind"
                )
            )


            val methodWithSlowEndPointInsight = MethodWithInsights(
                methodWithIds = methodWithCodeObjects,
                insights = listOf(
                    slowEndpointInsightEnv2,
                )
            )

            return InsightsOfMethodsResponse(
                environment = environmentList[1],
                methodsWithInsights = listOf(
                    methodWithSlowEndPointInsight
                )
            )
        }

    val expectedRecentActivityResult: RecentActivityResult = BulletTwoData.expectedRecentActivityResult
    val goToSpanRequestPayload: RecentActivityEntrySpanPayload = RecentActivityEntrySpanPayload(
        span = expectedRecentActivityResult.entries[1].firstEntrySpan,
        environment = expectedRecentActivityResult.entries[1].environment
    )
}

object BulletFourData {
    val methodName = "method1"
    val methodCodeObjectId: String = BulletTwoData.methodCodeObjectId
    val relatedSpansToMethod: List<String> = BulletTwoData.relatedSpansToMethod
    val relatedEndPointToMethod: List<String> = BulletTwoData.relatedEndPointToMethod


    val expectedInsightOfSingleSpan: InsightsOfSingleSpanResponse
        get() {
            val methodName = "relatedSpan1"
            val codeObjectId = relatedSpansToMethod[0]
            val spanInfo = SpanInfo(
                instrumentationLibrary = "instrumentationLibrary",
                name = "Span1 - related",
                spanCodeObjectId = relatedSpansToMethod[0],
                displayName = "Span 1",
                methodCodeObjectId = relatedSpansToMethod[0],
                kind = "my kind"
            )
            val durationInsight = SpanDurationsInsight(
                codeObjectId = codeObjectId,
                environment = BulletThreeData.environmentList[0],
                scope = "scope2",
                importance = 1,
                actualStartTime = null,
                customStartTime = null,
                prefixedCodeObjectId = null,
                shortDisplayInfo = ShortDisplayInfo(
                    title = "Span duration title for $methodName",
                    targetDisplayName = "target $methodName",
                    subtitle = "subtitle for $methodName",
                    description = "description for $methodName"
                ),
                decorators = listOf(
                    CodeObjectDecorator(
                        title = "Decorator - Span Duration of $methodName",
                        description = "Decorator 1 Span Duration of  $methodName"
                    ),
                ),
                spanInfo = spanInfo
            )



            return InsightsOfSingleSpanResponse(
                spanCodeObjectId = relatedSpansToMethod[0],
                spanInfo = spanInfo,
                insights = listOf(durationInsight)
            )
        }
}