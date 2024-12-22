package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.jcef.BaseEnvJsTemplateBuilder

private const val RECENT_EXPIRATION_LIMIT_VARIABLE = "recentActivityExpirationLimit"


class RecentActivityEnvJsTemplateBuilder(templatePath: String) : BaseEnvJsTemplateBuilder(templatePath) {

    override fun addAppSpecificEnvVariable(project: Project, data: MutableMap<String, Any>) {
        data[RECENT_EXPIRATION_LIMIT_VARIABLE] = RECENT_EXPIRATION_LIMIT_MILLIS
    }

}