package org.digma.intellij.plugin.ui.model.environment

interface EnvironmentsSupplier {
    fun getEnvironments(): List<String>
    fun addEnvironmentsListChangeListener(listener: EnvironmentsListChangedListener?)
    fun setCurrent(selectedItem: String?)
    fun getCurrent():String?
    fun refresh()
}