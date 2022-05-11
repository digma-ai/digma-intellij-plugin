package org.digma.intellij.plugin.ui.model

data class InsightsModel(
    var insightsCount: Int = 0,
    var methodName: String = "",
    var className: String = ""
):PanelModel{


    override fun count(): String {
        return insightsCount.toString()
    }
    override fun classAndMethod(): String{
        return "$className.$methodName"
    }



    override fun isMethodScope(): Boolean {
        return true
    }
}
