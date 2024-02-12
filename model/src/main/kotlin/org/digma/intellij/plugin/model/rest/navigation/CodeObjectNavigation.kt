package org.digma.intellij.plugin.model.rest.navigation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.insights.SpanInfo
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeObjectNavigation
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "spanCodeObjectId",
    "navigationEntry"
)
constructor(val spanCodeObjectId: String, val navigationEntry: NavigationEntry)


@JsonIgnoreProperties(ignoreUnknown = true)
data class NavigationEntry
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "spanInfo", "closestParentSpans", "navEndpointEntry", "closestParentEntries"
)
//todo:
// if spanInfo has discoverable methodId = direct link
// if navEndpointEntry is not null and has only one discoverable endpoint, only one method has this rout = direct link
// if navEndpointEntry has more then one method = show list
// else each entry in closestParentSpans and closestParentEntries will produce 0 or 1 link sorted by distance
constructor(val spanInfo: SpanInfo?, val closestParentSpans: List<SpanNavigationItem>, val navEndpointEntry: NavEndpointEntry?, val closestParentEntries: List<NavEndpointEntry>)


@JsonIgnoreProperties(ignoreUnknown = true)
data class SpanNavigationItem
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "name",
    "displayName",
    "instrumentationLibrary",
    "spanCodeObjectId",
    "methodCodeObjectId",
    "kind",
    "navItemType",
    "distance"
)
constructor(
    override val name: String,
    override val displayName: String,
    override val instrumentationLibrary: String,
    override val spanCodeObjectId: String,
    override val methodCodeObjectId: String?,
    override val kind: String,
    val navItemType: NavItemType,
    val distance: Int,
) : SpanInfo(instrumentationLibrary, name, spanCodeObjectId, displayName, methodCodeObjectId, kind)


@JsonIgnoreProperties(ignoreUnknown = true)
data class NavEndpointEntry
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "endpointCodeObjectId",
    "methodCodeObjectId",
    "displayName"
)
constructor(
    val endpointCodeObjectId: String,
    val methodCodeObjectId: String?,
    val displayName: String
)

enum class NavItemType {
    ClosestParentInternal, ClosestParentWithMethodCodeObjectId
}
