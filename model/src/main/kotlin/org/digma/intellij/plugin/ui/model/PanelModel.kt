package org.digma.intellij.plugin.ui.model

interface PanelModel {

    fun count(): String

    fun getTheScope(): Scope

    fun isMethodScope(): Boolean

    fun isEndpointScope(): Boolean

    fun isDocumentScope(): Boolean

    fun isCodeLessSpanScope(): Boolean

    fun getScopeString(): String

    fun getScopeTooltip(): String

}