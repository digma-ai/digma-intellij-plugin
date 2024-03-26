package org.digma.intellij.plugin.idea.maven.helper

import org.digma.intellij.plugin.buildsystem.BuildSystemHelperService
import org.jetbrains.idea.maven.utils.MavenUtil

//don't change to light service because it will register always. we want it to register only if gradle is enabled.
// see org.digma.intellij-with-gradle.xml
@Suppress("LightServiceMigrationCode")
class MavenService : BuildSystemHelperService {

    override fun isBuildSystem(externalSystemId: String): Boolean {
        return MavenUtil.SYSTEM_ID.id.equals(externalSystemId, true)
    }
}