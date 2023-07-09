package org.digma.intellij.plugin.semanticversion

import com.glovoapp.versioning.SemanticVersion
import com.intellij.ide.plugins.PluginManagerCore
import org.digma.intellij.plugin.PluginId


class SemanticVersionUtil {


    companion object {

        @JvmStatic
        fun isValidVersion(version: String): Boolean {
            return try {
                SemanticVersion.parse(version)
                true
            } catch (e: Exception) {
                false
            }
        }

        @JvmStatic
        fun from(version: String): SemanticVersion {
            return SemanticVersion.parse(version)
        }


        @JvmStatic
        fun removeBuildNumberAndPreRelease(version: String): String {
            val semanticVersion = from(version)
            val withoutBuildNumber = SemanticVersion(semanticVersion.major, semanticVersion.minor, semanticVersion.patch)
            return withoutBuildNumber.toString()
        }


        @JvmStatic
        fun getPluginVersion(defaultIfCantBeFound: String): String {
            val plugin = PluginManagerCore.getPlugin(com.intellij.openapi.extensions.PluginId.getId(PluginId.PLUGIN_ID))
            if (plugin != null)
                return plugin.version

            return defaultIfCantBeFound
        }


        @JvmStatic
        fun getPluginVersionWithoutBuildNumberAndPreRelease(defaultIfCantBeFound: String): String {
            val plugin = PluginManagerCore.getPlugin(com.intellij.openapi.extensions.PluginId.getId(PluginId.PLUGIN_ID))
            if (plugin != null)
                return removeBuildNumberAndPreRelease(plugin.version)

            return defaultIfCantBeFound
        }


    }


}