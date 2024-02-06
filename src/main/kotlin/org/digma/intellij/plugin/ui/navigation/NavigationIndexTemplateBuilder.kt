package org.digma.intellij.plugin.ui.navigation

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.jcef.BaseIndexTemplateBuilder

class NavigationIndexTemplateBuilder : BaseIndexTemplateBuilder(NAVIGATION_APP_RESOURCE_FOLDER_NAME, NAVIGATION_APP_INDEX_TEMPLATE_NAME) {

    override fun addAppSpecificEnvVariable(project: Project, data: HashMap<String, Any>) {
        //add data here
    }
}