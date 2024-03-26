package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfigurationBase

open class ConfigurationCleaner(
    protected val configuration: RunConfigurationBase<*>
) {
    //for most configuration we add our properties and env in a way that is not persistent
    // so there is no need to clean.
    //for gradle we add our properties in a way that is saved on the configuration, and we need to clean
    open fun cleanConfiguration() {
        //do nothing
    }
}