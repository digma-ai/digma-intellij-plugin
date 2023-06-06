package org.digma.intellij.plugin.navigation.codeless

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.document.CodeObjectsUtil
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

    val spanIdWithoutType = CodeObjectsUtil.stripSpanPrefix(spanId)
    val methodIdWithoutType:String? = methodId?.let {
        CodeObjectsUtil.stripMethodPrefix(it)
    }

    val instLibrary = spanIdWithoutType.substringBefore("\$_$")
    val spanName = spanIdWithoutType.substringAfter("\$_$")
    val funcNamespace = methodIdWithoutType?.substringBefore("\$_$")
    val funcName = methodIdWithoutType?.substringAfter("\$_$")


    project.service<InsightsViewService>().updateInsightsModel(CodeLessSpan(
        spanIdWithoutType,
        instLibrary,
        spanName,
        spanDisplayName,
        methodIdWithoutType,
        funcNamespace,
        funcName
    ))

    project.service<ErrorsViewService>().updateErrorsModel(CodeLessSpan(
        spanIdWithoutType,
        instLibrary,
        spanName,
        spanDisplayName,
        methodIdWithoutType,
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

    val spanIdWithoutType = CodeObjectsUtil.stripSpanPrefix(spanId)
    val methodIdWithoutType:String? = methodId?.let {
        CodeObjectsUtil.stripMethodPrefix(it)
    }


    val instLibrary = spanIdWithoutType.substringBefore("\$_$")
    val spanName = spanIdWithoutType.substringAfter("\$_$")
    val funcNamespace = methodIdWithoutType?.substringBefore("\$_$")
    val funcName = methodIdWithoutType?.substringAfter("\$_$")


    project.service<InsightsViewService>().updateInsightsModel(CodeLessSpanWithCodeLocation(
        spanIdWithoutType,
        instLibrary,
        spanName,
        spanDisplayName,
        methodIdWithoutType,
        funcNamespace,
        funcName,
        workspaceUri
    ))

    project.service<ErrorsViewService>().updateErrorsModel(CodeLessSpanWithCodeLocation(
        spanIdWithoutType,
        instLibrary,
        spanName,
        spanDisplayName,
        methodIdWithoutType,
        funcNamespace,
        funcName,
        workspaceUri
    ))


    project.service<ErrorsActionsService>().closeErrorDetailsBackButton()

    //clear the latest method so that if user clicks on the editor again after watching code less insights the context will change
    project.service<CurrentContextUpdater>().clearLatestMethod()

    ToolWindowShower.getInstance(project).showToolWindow()


}