package org.digma.intellij.plugin.navigation.codeless

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.editor.CurrentContextUpdater
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService


private val logger = Logger.getInstance("org.digma.intellij.plugin.navigation.codeless.CodelessNavigation")

fun showInsightsForSpan(project: Project, spanId: String, methodId: String?) {

    Log.log(logger::debug,project,"Got showInsightsForSpan request for {} {}", spanId, methodId)

    val instLibrary = spanId.substringBefore("\$_$")
    val spanName = spanId.substringAfter("\$_$")
    val funcNamespace = methodId?.substringBefore("\$_$")
    val funcName = methodId?.substringAfter("\$_$")


    project.service<InsightsViewService>().updateInsightsModel(CodeLessSpan(
        spanId,
        instLibrary,
        spanName,
        methodId,
        funcNamespace,
        funcName
    ))

    project.service<ErrorsViewService>().updateErrorsModel(CodeLessSpan(
        spanId,
        instLibrary,
        spanName,
        methodId,
        funcNamespace,
        funcName
    ))


    project.service<ErrorsActionsService>().closeErrorDetailsBackButton()

    //clear the latest method so that if user clicks on the editor again after watching code less insights the context will change
    project.service<CurrentContextUpdater>().clearLatestMethod()

    ToolWindowShower.getInstance(project).showToolWindow()

}