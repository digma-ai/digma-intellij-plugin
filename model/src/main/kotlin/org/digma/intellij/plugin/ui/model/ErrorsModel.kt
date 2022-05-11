package org.digma.intellij.plugin.ui.model

data class ErrorsModel(
    var errorsCount: Int = 0,
    var methodName: String = "",
    var className: String = ""):PanelModel {


    override fun count(): String {
       return errorsCount.toString()
    }


    override fun isMethodScope(): Boolean {
        return true
    }

    override fun classAndMethod(): String {
        return "$className.$methodName"
    }
}
