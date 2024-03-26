package org.digma.intellij.plugin.idea.gradle.helper

import org.digma.intellij.plugin.buildsystem.BuildSystemHelperService
import org.jetbrains.plugins.gradle.util.GradleConstants

//don't change to light service because it will register always. we want it to register only if gradle is enabled.
// see org.digma.intellij-with-gradle.xml
@Suppress("LightServiceMigrationCode")
class GradleService : BuildSystemHelperService {

    override fun isBuildSystem(externalSystemId: String): Boolean {
        return GradleConstants.SYSTEM_ID.id.equals(externalSystemId, true)
    }
}