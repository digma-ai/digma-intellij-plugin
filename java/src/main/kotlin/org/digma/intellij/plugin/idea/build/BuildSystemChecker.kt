package org.digma.intellij.plugin.idea.build

import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.module.Module
import org.jetbrains.annotations.NotNull
import org.jetbrains.idea.maven.utils.MavenUtil
import org.jetbrains.plugins.gradle.util.GradleConstants


class BuildSystemChecker {

    companion object {

        // if cannot determine will return UNKNOWN
        @NotNull
        fun determineBuildSystem(module: Module?): JavaBuildSystem {
            if (module == null) {
                return JavaBuildSystem.UNKNOWN
            }

            val moduleProperties = ExternalSystemModulePropertyManager.getInstance(module)
            val externalSystemId = moduleProperties.getExternalSystemId()

            if (GradleConstants.SYSTEM_ID.id.equals(externalSystemId, true)) {
                return JavaBuildSystem.GRADLE
            }

            if (MavenUtil.SYSTEM_ID.id.equals(externalSystemId, true)) {
                return JavaBuildSystem.MAVEN
            }

            return JavaBuildSystem.UNKNOWN
        }
    }

}