package org.digma.intellij.plugin.ui.list.summaries

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.SpanDurationChangeInsight
import org.digma.intellij.plugin.model.rest.insights.TopErrorFlowsInsight
import org.digma.intellij.plugin.ui.list.AbstractPanelListModel
import org.digma.intellij.plugin.ui.list.PanelList
import org.digma.intellij.plugin.ui.list.insights.needToShowDurationChange
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.Collections

class SummaryPanelList(project: Project, listViewItems: List<ListViewItem<*>>) : PanelList(project, Model(listViewItems)) {


    init {
        this.setCellRenderer(SummaryPanelListCellRenderer())
    }

    private class Model(listViewItems: List<ListViewItem<*>>) : AbstractPanelListModel() {
        private val LOGGER = Logger.getInstance(SummaryPanelList::Model::class.java)

        init {
            setListData(listViewItems)
        }

        override fun setListData(listViewItems: List<ListViewItem<*>>) {

            Collections.sort(listViewItems, Comparator.comparingInt(ListViewItem<*>::sortIndex))

            val newViewItems = ArrayList<ListViewItem<*>>()
            var index = 0
            for (value in listViewItems) {
                when (val model = value.modelObject) {
                    is TopErrorFlowsInsight -> {
                        newViewItems.add(ListViewItem(SummaryTypeTitle(model.type), index++))
                        for (error in model.errors) {
                            newViewItems.add(ListViewItem(error, index++))
                        }
                    }
                    is SpanDurationChangeInsight -> {
                        newViewItems.add(ListViewItem(SummaryTypeTitle(model.type), index++))
                        for (change in model.spanDurationChanges) {
                            val changedPercentiles = change.percentiles.filter { needToShowDurationChange(it) } // Should be server side?
                            if (changedPercentiles.isNotEmpty()) {
                                val item = ListViewItem(SpanDurationChangeInsight.Change(change.codeObjectId, change.span, changedPercentiles), index++)
                                item.moreData.putAll(value.moreData)
                                newViewItems.add(item)
                            }
                        }
                    }
                    else -> {
                        Log.log(LOGGER::warn, "Unknown global insight : {}", model!!::class)
                    }
                }
            }

            super.setListData(newViewItems)
        }
    }
}

class SummaryTypeTitle(val type: InsightType)