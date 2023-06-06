package org.digma.intellij.plugin.navigation.codeless

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.editor.CurrentContextUpdater
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.CodeLessSpanWithCodeLocation
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService


private val logger = Logger.getInstance("org.digma.intellij.plugin.navigation.codeless.CodelessNavigation")

fun showInsightsForSpan(project: Project, spanId: String,spanDisplayName: String?, methodId: String?) {

    Log.log(logger::debug,project,"Got showInsightsForSpan request for {} {}", spanId, methodId)

    val instLibrary = spanId.substringBefore("\$_$")
    val spanName = spanId.substringAfter("\$_$")
    val funcNamespace = methodId?.substringBefore("\$_$")
    val funcName = methodId?.substringAfter("\$_$")


    project.service<InsightsViewService>().updateInsightsModel(CodeLessSpan(
        spanId,
        instLibrary,
        spanName,
        spanDisplayName,
        methodId,
        funcNamespace,
        funcName
    ))

    project.service<ErrorsViewService>().updateErrorsModel(CodeLessSpan(
        spanId,
        instLibrary,
        spanName,
        spanDisplayName,
        methodId,
        funcNamespace,
        funcName
    ))


    project.service<ErrorsActionsService>().closeErrorDetailsBackButton()

    //clear the latest method so that if user clicks on the editor again after watching code less insights the context will change
    project.service<CurrentContextUpdater>().clearLatestMethod()

    ToolWindowShower.getInstance(project).showToolWindow()

}

fun showInsightsForSpanWithCodeLocation(
    project: Project,
    spanId: String,
    spanDisplayName: String?,
    methodId: String?,
    workspaceUri: Pair<String, Int>
) {

    Log.log(logger::debug,project,"Got showInsightsForSpanWithCodeLocation request for {} {}", spanId, methodId)

    val instLibrary = spanId.substringBefore("\$_$")
    val spanName = spanId.substringAfter("\$_$")
    val funcNamespace = methodId?.substringBefore("\$_$")
    val funcName = methodId?.substringAfter("\$_$")


    project.service<InsightsViewService>().updateInsightsModel(CodeLessSpanWithCodeLocation(
        spanId,
        instLibrary,
        spanName,
        spanDisplayName,
        methodId,
        funcNamespace,
        funcName,
        workspaceUri
    ))

    project.service<ErrorsViewService>().updateErrorsModel(CodeLessSpanWithCodeLocation(
        spanId,
        instLibrary,
        spanName,
        spanDisplayName,
        methodId,
        funcNamespace,
        funcName,
        workspaceUri
    ))


    project.service<ErrorsActionsService>().closeErrorDetailsBackButton()

    //clear the latest method so that if user clicks on the editor again after watching code less insights the context will change
    project.service<CurrentContextUpdater>().clearLatestMethod()

    ToolWindowShower.getInstance(project).showToolWindow()


}