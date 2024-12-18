package org.digma.intellij.plugin.ui.mainapp

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.jcef.BaseEnvJsTemplateBuilder

class MainAppEnvJsTemplateBuilder(templatePath: String) : BaseEnvJsTemplateBuilder(templatePath) {

    override fun addAppSpecificEnvVariable(project: Project, data: MutableMap<String, Any>) {

    }
}