package org.digma.intellij.plugin.model.rest.insights

import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.adjustHttpRouteIfNeeded
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema.Companion.getShortRouteName
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EndpointSchemaTest {

    @Test
    fun getShortRouteNameShouldWorkForHttpSchema() {
        assertEquals("get /hello", getShortRouteName("epHTTP:get /hello"))
    }

    @Test
    fun getShortRouteNameShouldWorkForRpcSchema() {
        assertEquals("hello.world", getShortRouteName("epRPC:hello.world"))
    }

    @Test
    fun getShortRouteNameShouldWorkEvenWhenNonRecognizedSchema() {
        assertEquals("whats.up.dude", getShortRouteName("whats.up.dude"))
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
