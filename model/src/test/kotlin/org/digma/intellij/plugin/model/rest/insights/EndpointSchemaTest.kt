package org.digma.intellij.plugin.model.rest.insights

import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.adjustHttpRouteIfNeeded
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.getRouteInfo
import java.time.temporal.ChronoUnit
import java.util.*
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
        val normalUsageInsight = NormalUsageInsight(codeObjectId, "env1", "scope1", 3, null, "get /yes", "endpointSpan1", actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(codeObjectId), null, 3)
        adjustHttpRouteIfNeeded(normalUsageInsight)
        assertEquals("get /yes", normalUsageInsight.route)
    }

    @Test
    fun adjustHttpRouteIfNeededShouldSkipAdjustWhenHttpSchemaAlreadyExists() {
        val codeObjectId = "456"
        val normalUsageInsight = NormalUsageInsight(codeObjectId, "env2", "scope2", 2, null, "epHTTP:post /letsgo", "endpointSpan2", actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(codeObjectId), null, 8)
        adjustHttpRouteIfNeeded(normalUsageInsight)
        assertEquals("epHTTP:post /letsgo", normalUsageInsight.route)
    }

    @Test
    fun adjustHttpRouteIfNeededShouldSkipAdjustWhenRpcSchemaAlreadyExists() {
        val codeObjectId = "789"
        val normalUsageInsight = NormalUsageInsight(codeObjectId, "env3", "scope3", 5, null, "epRPC:serviceA.methodB", "endpointSpan3", actualStartTimeNow, customStartTimeFiveDaysBefore, addPrefixToCodeObjectId(codeObjectId), null, 4)
        adjustHttpRouteIfNeeded(normalUsageInsight)
        assertEquals("epRPC:serviceA.methodB", normalUsageInsight.route)
    }

    private fun addPrefixToCodeObjectId(codeObjectId: String): String {
        return "method:$codeObjectId"
    }
}
