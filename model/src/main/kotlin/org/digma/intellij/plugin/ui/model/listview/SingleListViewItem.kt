package org.digma.intellij.plugin.ui.model.listview

import org.digma.intellij.plugin.model.discovery.CodeObjectInfo
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight

/**
 * ane item in a UI list.
 */
class SingleListViewItem(sortIndex: Int, val insight: CodeObjectInsight, val scope: CodeObjectInfo):ListViewItem<CodeObjectInsight>(sortIndex) {


    override fun getModel(): CodeObjectInsight {
        return insight
    }


    override fun equals(other: Any?): Boolean {
         return this === other
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}