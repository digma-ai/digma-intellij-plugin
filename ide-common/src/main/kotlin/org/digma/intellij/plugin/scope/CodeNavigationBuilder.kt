package org.digma.intellij.plugin.scope

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.CodeObjectsUtil
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.model.code.CodeDetails
import org.digma.intellij.plugin.model.rest.navigation.AssetCodeLocation
import org.digma.intellij.plugin.model.rest.navigation.AssetNavigationResponse
import org.digma.intellij.plugin.model.rest.navigation.AssetRelatedCodeLocation
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.psi.LanguageService


fun buildCodeLocation(
    project: Project,
    spanCodeObjectId: String,
    displayName: String,
    methodCodeObjectId: String?,
): CodeLocation {

    val codeNavigator = CodeNavigator.getInstance(project)

    if (codeNavigator.canNavigateToSpan(spanCodeObjectId)) {
        val codeDetails = CodeDetails(displayName, spanCodeObjectId)
        //direct navigation
        return CodeLocation(listOf(codeDetails), listOf())
    }

    if (methodCodeObjectId != null && codeNavigator.canNavigateToMethod(methodCodeObjectId)) {
        val codeDetails = CodeDetails(displayName, methodCodeObjectId)
        //direct navigation
        return CodeLocation(listOf(codeDetails), listOf())
    }


    val assetNavigation = try {
        AnalyticsService.getInstance(project).getAssetNavigation(spanCodeObjectId)
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError(project, "ScopeManager.changeToSpanScope", e)
        null
    }

    return assetNavigation?.let { an ->

        buildCodeLocationFromAssetNavigation(project, an)

    } ?: CodeLocation(listOf(), listOf())
}


private fun buildCodeLocationFromAssetNavigation(
    project: Project,
    assetNavigation: AssetNavigationResponse,
): CodeLocation {

    val codeDetailsList = buildFromCodeLocation(project, assetNavigation.codeLocation)
    //if has code locations no need to continue
    if (codeDetailsList.isNotEmpty()) {
        return CodeLocation(codeDetailsList, listOf())
    }

    val relatedCodeDetailsList = buildFromRelatedCodeLocation(project, assetNavigation)


    return CodeLocation(listOf(), relatedCodeDetailsList)

}


private fun buildFromCodeLocation(project: Project, codeLocation: AssetCodeLocation): List<CodeDetails> {

    val codeNavigator = CodeNavigator.getInstance(project)

    val codeDetailsList = mutableListOf<CodeDetails>()

    //its actually the same span as the original and we checked it already
//    if (codeNavigator.canNavigateToSpan(assetNavigation.codeLocation.spanCodeObjectId)) {
//        codeDetailsList.add(
//            CodeDetails(
////                getSpanDisplayName(project, assetNavigation.codeLocation.spanCodeObjectId),
//                assetNavigation.codeLocation.displayName,
//                assetNavigation.codeLocation.spanCodeObjectId
//            )
//        )
//    }

    val methodId = codeLocation.methodCodeObjectId
    if (methodId != null && codeNavigator.canNavigateToMethod(methodId)) {
//        codeDetailsList.add(CodeDetails(getMethodDisplayName(methodId), methodId))
        codeDetailsList.add(CodeDetails(codeLocation.displayName, methodId))
        //direct navigation
        return codeDetailsList
    }


    val endpointCodeObjectId = codeLocation.endpointAssetCodeLocation?.endpointCodeObjectId
    if (endpointCodeObjectId != null) {

        val endpointInfos = LanguageService.getEndpointInfos(project, endpointCodeObjectId)
        if (!endpointInfos.isNullOrEmpty()) {

            //may be one or more endpoints
            endpointInfos.forEach { ei ->
                val endpointMethodId = ei.containingMethodId
                if (codeNavigator.canNavigateToMethod(endpointMethodId)) {
//                    codeDetailsList.add(CodeDetails(getMethodDisplayName(methodId), methodId))
                    codeDetailsList.add(CodeDetails(codeLocation.displayName, endpointMethodId))
                }
            }
        }
    }

    return codeDetailsList
}


private fun buildFromRelatedCodeLocation(project: Project, assetNavigation: AssetNavigationResponse): List<CodeDetails> {

    val relatedCodeLocation = assetNavigation.relatedCodeLocations

    val relatedLocationsMap = mutableMapOf<Int, MutableList<Pair<AssetRelatedCodeLocation, List<CodeDetails>>>>()

    relatedCodeLocation.forEach { relatedLocation ->

        val codeDetailsList = buildFromCodeLocation(project, relatedLocation.spanCodeLocation)

        if (codeDetailsList.isNotEmpty()) {
            val flowList = relatedLocationsMap.computeIfAbsent(relatedLocation.flowIndex) {
                mutableListOf<Pair<AssetRelatedCodeLocation, List<CodeDetails>>>().sortedWith { a, b -> a.first.distance.compareTo(b.first.distance) }
                    .toMutableList()
            }
            flowList.add(Pair(relatedLocation, codeDetailsList))
        }

    }

    val soredLocations = relatedLocationsMap.values.sortedWith { a, b -> a[0].first.distance.compareTo(b[0].first.distance) }.toMutableList()

    val result: MutableList<CodeDetails> = mutableListOf()
    soredLocations.forEach {
        result.addAll(it[0].second)
    }

    return if (result.size > 8) {
        result.subList(0, 8)
    } else {
        result
    }
}


private fun getMethodDisplayName(methodId: String): String {
    val methodAndClassName = CodeObjectsUtil.getMethodClassAndName(methodId)
    return methodAndClassName.first.plus(".").plus(methodAndClassName.second)
}


private fun getSpanDisplayName(project: Project, spanCodeObjectId: String): String {
    val spanScopeInfo = try {
        AnalyticsService.getInstance(project).getAssetDisplayInfo(spanCodeObjectId)
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError(project, "ScopeManager.changeToSpanScope", e)
        null
    }

    return spanScopeInfo?.displayName ?: spanCodeObjectId
}


//private fun isAlreadyAtCodeLocation(project: Project, spanCodeObjectId: String, methodCodeObjectId: String?): Boolean {
//
//    val codeNavigator = CodeNavigator.getInstance(project)
//
//    val spanLocation: Pair<String, Int>? = codeNavigator.getSpanLocation(spanCodeObjectId)
//    val latestMethodInfo = LatestMethodUnderCaretHolder.getInstance(project).latestMethodInfo
//    if (latestMethodInfo != null && spanLocation != null) {
//        if (spanLocation.second >= latestMethodInfo.startOffset && spanLocation.second <= latestMethodInfo.endOffset) {
//            return true
//        }
//    }
//
//    if (latestMethodInfo != null && methodCodeObjectId != null) {
//        return latestMethodInfo.methodId == CodeObjectsUtil.stripMethodPrefix(methodCodeObjectId)
//    }
//    return false
//
//}