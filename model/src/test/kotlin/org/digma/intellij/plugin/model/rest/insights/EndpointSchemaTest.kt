package org.digma.intellij.plugin.model.rest.insights

import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.adjustHttpRouteIfNeeded
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.getRouteInfo
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EndpointSchemaTest {

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
        val normalUsageInsight = NormalUsageInsight("123", "get /yes", 3)
        adjustHttpRouteIfNeeded(normalUsageInsight)
        assertEquals("epHTTP:get /yes", normalUsageInsight.route)
    }

    @Test
    fun adjustHttpRouteIfNeededShouldSkipAdjustWhenHttpSchemaAlreadyExists() {
        val normalUsageInsight = NormalUsageInsight("456", "epHTTP:post /letsgo", 8)
        adjustHttpRouteIfNeeded(normalUsageInsight)
        assertEquals("epHTTP:post /letsgo", normalUsageInsight.route)
    }

    @Test
    fun adjustHttpRouteIfNeededShouldSkipAdjustWhenRpcSchemaAlreadyExists() {
        val normalUsageInsight = NormalUsageInsight("789", "epRPC:serviceA.methodB", 4)
        adjustHttpRouteIfNeeded(normalUsageInsight)
        assertEquals("epRPC:serviceA.methodB", normalUsageInsight.route)
    }

}
