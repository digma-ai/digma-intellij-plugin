package org.digma.intellij.plugin.ui.model.errors

import org.digma.intellij.plugin.ui.model.PanelModel

data class ErrorsModel(
    var errorsCount: Int = 0,
    var methodName: String = "",
    var className: String = ""): PanelModel {


    override fun count(): String {
        return errorsCount.toString()
    }


    override fun isMethodScope(): Boolean {
        return true
    }

    override fun isDocumentScope(): Boolean {
        return false
    }

    override fun getScope(): String {
        return classAndMethod()
    }

    fun classAndMethod(): String {
        return "$className.$methodName"
    }
}
