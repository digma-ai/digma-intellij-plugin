package org.digma.intellij.plugin.ui.model

import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier

interface PanelModel {

    fun count(): String

    fun isMethodScope(): Boolean

    fun isDocumentScope(): Boolean

    fun getScope(): String

    fun getScopeTooltip(): String

    fun getEnvironmentsSupplier(): EnvironmentsSupplier
}