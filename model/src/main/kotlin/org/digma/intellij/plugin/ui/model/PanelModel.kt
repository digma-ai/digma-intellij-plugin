package org.digma.intellij.plugin.ui.model

import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult

interface PanelModel {

    fun count(): String

    fun getTheScope(): Scope

    fun isMethodScope(): Boolean

    fun isDocumentScope(): Boolean

    fun isCodeLessSpanScope(): Boolean

    fun getScopeString(): String

    fun getScopeTooltip(): String

    fun getUsageStatus(): UsageStatusResult
}