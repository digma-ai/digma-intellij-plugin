package org.digma.intellij.plugin.ui.model.environment

interface EnvironmentsSupplier {
    fun getEnvironments(): List<String>
    fun setCurrent(selectedItem: String?)
    fun getCurrent():String?
    fun refresh()
    fun refreshNowOnBackground()
}