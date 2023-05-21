package org.digma.intellij.plugin.navigation.codeless

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService


private val logger = Logger.getInstance("org.digma.intellij.plugin.navigation.codeless.CodelessNavigation")

fun navigate(project: Project, spanInstLibrary: String?,
    spanName: String?, functionNamespace: String?, functionName: String?,
) {

    Log.log(logger::debug,project,"Got navigation request for {} {} {} {}", spanInstLibrary, spanName, functionNamespace, functionName)


    if (spanInstLibrary == null || spanName == null) {
        Log.log(logger::debug,project, "Not navigating because span instrumentation library or span name is null")
        return
    }

    val spanId = CodeObjectsUtil.createSpanId(spanInstLibrary, spanName)
    val methodId = if (functionNamespace != null && functionName != null) CodeObjectsUtil.createMethodCodeObjectId(
        functionNamespace,functionName) else null


    project.service<InsightsViewService>().updateInsightsModel(CodeLessSpan(
        spanId,
        spanInstLibrary,
        spanName,
        methodId,
        functionNamespace,
        functionName
    ))

    project.service<ErrorsViewService>().updateErrorsModel(CodeLessSpan(
        spanId,
        spanInstLibrary,
        spanName,
        methodId,
        functionNamespace,
        functionName
    ))
}


fun navigate(project: Project, spanCodeObjectId: String, methodCodeObjectId: String?) {
    val instLibrary = spanCodeObjectId.substringBefore("\$_$")
    val spanName = spanCodeObjectId.substringAfter("\$_$")
    val funcNamespace = methodCodeObjectId?.substringBefore("\$_$")
    val funcName = methodCodeObjectId?.substringAfter("\$_$")
    project.service<InsightsViewService>().updateInsightsModel(CodeLessSpan(
        spanCodeObjectId,
        instLibrary,
        spanName,
        methodCodeObjectId,
        funcNamespace,
        funcName
    ))

    project.service<ErrorsViewService>().updateErrorsModel(CodeLessSpan(
        spanCodeObjectId,
        instLibrary,
        spanName,
        methodCodeObjectId,
        funcNamespace,
        funcName
    ))

}