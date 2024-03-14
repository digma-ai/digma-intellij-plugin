package org.digma.intellij.plugin.ui.assets

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.text.VersionComparatorUtil
import org.digma.intellij.plugin.persistence.PersistenceService


private const val ASSET_SEARCH_ENV_NAME = "assetsSearch"
private const val SELECTED_SERVICES_VARIABLE = "assetsSelectedServices"

class AssetsIndexTemplateData {

    fun addAppSpecificEnvVariable(project: Project, data: MutableMap<String, Any>) {

        val assetSearchEnabledForLinux: Boolean = VersionComparatorUtil.compare(ApplicationInfo.getInstance().majorVersion, "2023") >= 0
        data[ASSET_SEARCH_ENV_NAME] = if (SystemInfo.isLinux) assetSearchEnabledForLinux.toString() else "true"

        val selectedServices = PersistenceService.getInstance().getSelectedServices()[project.name]
        if (selectedServices.isNullOrEmpty()) {
            data[SELECTED_SERVICES_VARIABLE] = "[]"
        } else {
            data[SELECTED_SERVICES_VARIABLE] = "['${selectedServices.joinToString("','")}']"
        }
    }
}