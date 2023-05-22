package org.digma.intellij.plugin.ui.model

import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult

interface PanelModel {

    fun count(): String

    fun isMethodScope(): Boolean

    fun isDocumentScope(): Boolean

    fun isCodeLessSpanScope(): Boolean

    fun getScope(): String

    fun getScopeTooltip(): String

    fun getUsageStatus(): UsageStatusResult
}