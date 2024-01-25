package org.digma.intellij.plugin.insights

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.model.insights.InsightsModelReact


@Service(Service.Level.PROJECT)
class InsightsModelReactHolder(val project: Project) {
    val model = InsightsModelReact()
}