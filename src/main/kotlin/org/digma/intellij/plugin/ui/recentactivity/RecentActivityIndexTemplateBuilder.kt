package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.recentactivity.RecentActivityLogic
import org.digma.intellij.plugin.ui.jcef.BaseIndexTemplateBuilder

private const val RECENT_EXPIRATION_LIMIT_VARIABLE = "recentActivityExpirationLimit"


class RecentActivityIndexTemplateBuilder :
    BaseIndexTemplateBuilder(RECENT_ACTIVITY_RESOURCE_FOLDER_NAME, RECENT_ACTIVITY_INDEX_TEMPLATE_NAME) {

    override fun addAppSpecificEnvVariable(project: Project, data: HashMap<String, Any>) {
        data[RECENT_EXPIRATION_LIMIT_VARIABLE] = RecentActivityLogic.RECENT_EXPIRATION_LIMIT_MILLIS
    }
}