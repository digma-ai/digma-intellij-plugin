package org.digma.intellij.plugin.ui.model.environment


interface EnvironmentsListChangedListener {

    fun environmentsListChanged(newEnvironments: List<String>)
}
