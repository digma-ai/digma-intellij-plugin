package org.digma.intellij.plugin.ui.model

interface PanelModel {

    fun count(): String

    fun isMethodScope(): Boolean

    fun classAndMethod(): String
}