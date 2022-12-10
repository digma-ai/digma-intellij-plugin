package org.digma.intellij.plugin.ui.list

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.service.InsightsActionsService

fun openWorkspaceFileForSpan(
        project: Project,
        moreData: HashMap<String, Any>,
        spanId: String,
) {
    val actionListener: InsightsActionsService = project.getService(InsightsActionsService::class.java)

    @Suppress("UNCHECKED_CAST")
    val workspaceUri: Pair<String, Int> = moreData[spanId] as Pair<String, Int>
    actionListener.openWorkspaceFileForSpan(workspaceUri.first, workspaceUri.second)
}