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
constructor(val spanInfo: SpanInfoCopyTemp, val closestParentSpans: List<SpanNavigationItem>)



//TODO: we should use SpanInfo here.
// this is a workaround for a bug in backend that sometimes can not find the span because of names mismatch.
// when that happens the fields in the span are null and jackson fails.
// when the backend bug is fixed we can change to use SpanInfo.
@JsonIgnoreProperties(ignoreUnknown = true)
class SpanInfoCopyTemp
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "instrumentationLibrary",
    "name",
    "spanCodeObjectId",
    "displayName",
    "methodCodeObjectId",
    "kind",
)
constructor(
    open val instrumentationLibrary: String?,
    open val name: String?,
    open val spanCodeObjectId: String?,
    open val displayName: String?,
    open val methodCodeObjectId: String?,
    open val kind: String?,
)







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