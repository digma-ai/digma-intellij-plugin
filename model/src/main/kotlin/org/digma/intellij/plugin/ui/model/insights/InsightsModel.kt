package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*

data class InsightsModel(
    var insightsCount: Int = 0,
    var methodName: String = "",
    var className: String = "",
    var listViewItems: List<ListViewItem<*>> = Collections.emptyList(),
    var previewListViewItems: List<ListViewItem<*>> = Collections.emptyList(),
    var card: InsightsTabCard = InsightsTabCard.INSIGHTS,
    var scope: Scope = EmptyScope("")
) : PanelModel {

    override fun count(): String {
        return insightsCount.toString()
    }

    override fun isMethodScope(): Boolean {
        return scope is MethodScope
    }

    override fun isDocumentScope(): Boolean {
        return scope is DocumentScope
    }

    override fun getScope(): String {
        return scope.getScope()
    }

    fun getPreviewListMessage():String {
        if (previewListViewItems.isEmpty()){
            return "No code objects found for this document"
        }else{
            return "Try to click one of the following code objects"
        }
    }
}


enum class InsightsTabCard{
    INSIGHTS,PREVIEW
}


interface Scope{
    fun getScope():String
}

class EmptyScope(val text:String): Scope{
    override fun getScope(): String {
        return text
    }

}

class MethodScope(val methodInfo: MethodInfo): Scope{
    override fun getScope(): String {
        return "${methodInfo.containingClass}.${methodInfo.name}"
    }

}

class DocumentScope(val documentInfo: DocumentInfo): Scope{
    override fun getScope(): String {
        return "${documentInfo.path.substringAfterLast('/')}"
    }

}