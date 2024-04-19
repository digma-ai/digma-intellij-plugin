package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfiguration

object ExternalSystemConfigurationTempStorage {

    //used to keep existing env of configuration, so we can restore them after program runs
    val orgConfigurationEnvironmentVars = mutableMapOf<RunConfiguration, Map<String, String>>()

}