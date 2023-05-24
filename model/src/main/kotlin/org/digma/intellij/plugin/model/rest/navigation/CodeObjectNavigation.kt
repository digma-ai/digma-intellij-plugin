package org.digma.intellij.plugin.model.rest.navigation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.insights.SpanInfo
import java.beans.ConstructorProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeObjectNavigation
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "navigationEntry"
)
constructor(val navigationEntry: NavigationEntry)


@JsonIgnoreProperties(ignoreUnknown = true)
data class NavigationEntry
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "spanInfo", "closestParentSpans"
)
constructor(val spanInfo: SpanInfo, val spanNavigationItems: List<SpanNavigationItem>)



//@JsonIgnoreProperties(ignoreUnknown = true)
//class CodeObjectNavigationSpanInfo
//@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
//@ConstructorProperties(
//    "instrumentationLibrary",
//    "name",
//    "spanCodeObjectId",
//    "displayName",
//    "methodCodeObjectId",
//    "kind",
//)
//constructor(
//    open val instrumentationLibrary: String?,
//    open val name: String?,
//    open val spanCodeObjectId: String,
//    open val displayName: String?,
//    open val methodCodeObjectId: String?,
//    open val kind: String?,
//)






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
): SpanInfo(instrumentationLibrary,name,spanCodeObjectId,displayName,methodCodeObjectId,kind)



enum class NavItemType {
    ClosestParentInternal,ClosestParentWithMethodCodeObjectId
}