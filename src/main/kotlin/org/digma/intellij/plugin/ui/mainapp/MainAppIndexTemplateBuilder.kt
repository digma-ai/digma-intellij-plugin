package org.digma.intellij.plugin.ui.mainapp

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.assets.AssetsIndexTemplateData
import org.digma.intellij.plugin.ui.jcef.BaseIndexTemplateBuilder

class MainAppIndexTemplateBuilder : BaseIndexTemplateBuilder(MAIN_APP_RESOURCE_FOLDER_NAME, MAIN_APP_INDEX_TEMPLATE_NAME) {

    override fun addAppSpecificEnvVariable(project: Project, data: MutableMap<String, Any>) {
        AssetsIndexTemplateData().addAppSpecificEnvVariable(project, data)
    }
}