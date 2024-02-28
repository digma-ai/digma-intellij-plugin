package org.digma.intellij.plugin.ui.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.jcef.BaseIndexTemplateBuilder

class InsightsIndexTemplateBuilder : BaseIndexTemplateBuilder(INSIGHTS_APP_RESOURCE_FOLDER_NAME, INSIGHTS_APP_INDEX_TEMPLATE_NAME) {

    override fun addAppSpecificEnvVariable(project: Project, data: MutableMap<String, Any>) {


    }

}