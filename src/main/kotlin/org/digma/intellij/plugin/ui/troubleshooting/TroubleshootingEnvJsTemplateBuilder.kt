package org.digma.intellij.plugin.ui.troubleshooting

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.jcef.BaseEnvJsTemplateBuilder

class TroubleshootingEnvJsTemplateBuilder(templatePath: String) : BaseEnvJsTemplateBuilder(templatePath) {

    override fun addAppSpecificEnvVariable(project: Project, data: MutableMap<String, Any>) {

    }
}