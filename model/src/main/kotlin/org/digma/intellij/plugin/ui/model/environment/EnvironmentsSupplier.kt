package org.digma.intellij.plugin.ui.model.environment

import org.jetbrains.annotations.NotNull

interface EnvironmentsSupplier {
    fun getEnvironments(): List<String>
    fun setCurrent(@NotNull selectedItem: String?)
    fun getCurrent(): String?
    fun refresh()
    fun refreshNowOnBackground()
}