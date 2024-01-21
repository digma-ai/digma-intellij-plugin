package org.digma.intellij.plugin.model.rest.insights

import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.adjustHttpRouteIfNeeded
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.getRouteInfo
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EndpointSchemaTest {

    private var actualStartTimeNow = Date()
    private var customStartTimeFiveDaysBefore = Date.from(actualStartTimeNow.toInstant().minus(5, ChronoUnit.DAYS))

    @Test
    fun getShortRouteNameShouldWorkForHttpSchema() {
        assertEquals("get /hello", getRouteInfo("epHTTP:get /hello").shortName)
    }

    @Test
    fun getShortRouteNameShouldWorkForRpcSchema() {
        assertEquals("hello.world", getRouteInfo("epRPC:hello.world").shortName)
    }

    @Test
    fun getShortRouteNameShouldWorkEvenWhenNonRecognizedSchema() {
        assertEquals("whats.up.dude", getRouteInfo("whats.up.dude").shortName)
    }

    @Test
    fun adjustHttpRouteIfNeededShouldAdjustWhenNoSchema() {
        val codeObjectId = "123"
        val normalUsageInsight = NormalUsageInsight(codeObjectId, "env1", "scope1", 3, null, actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(codeObjectId), false,null,
                createSpanInfo("endpointSpan1"), "get /yes", "myService",
                3,0.0,0.0,0.0, null, null, null, 0, null)
        adjustHttpRouteIfNeeded(normalUsageInsight)
        assertEquals("get /yes", normalUsageInsight.route)
    }

    @Test
    fun adjustHttpRouteIfNeededShouldSkipAdjustWhenHttpSchemaAlreadyExists() {
        val codeObjectId = "456"
        val normalUsageInsight = NormalUsageInsight(codeObjectId, "env2", "scope2", 2, null, actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(codeObjectId), false,null,
                createSpanInfo("endpointSpan1"), "epHTTP:post /letsgo", "myService",
                8,0.0,0.0,0.0, null, null, null, 0, null)
        adjustHttpRouteIfNeeded(normalUsageInsight)
        assertEquals("epHTTP:post /letsgo", normalUsageInsight.route)
    }

    @Test
    fun adjustHttpRouteIfNeededShouldSkipAdjustWhenRpcSchemaAlreadyExists() {
        val codeObjectId = "789"
        val normalUsageInsight = NormalUsageInsight(codeObjectId, "env3", "scope3", 5, null, actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(codeObjectId), false,null,
                createSpanInfo("endpointSpan3"), "epRPC:serviceA.methodB", "MyService",
                4,0.0,0.0,0.0, null, null, null, 0, null)
        adjustHttpRouteIfNeeded(normalUsageInsight)
        assertEquals("epRPC:serviceA.methodB", normalUsageInsight.route)
    }

    private fun createSpanInfo(spanName: String): SpanInfo {
        val instLib = "il"
        return SpanInfo(instLib, spanName, "span:$instLib\$_\$$spanName", "disp_$spanName", null, "Internal")
    }

    private fun addPrefixToCodeObjectId(codeObjectId: String): String {
        return "method:$codeObjectId"
    }
}
