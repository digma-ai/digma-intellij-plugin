package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.list.AbstractPanelListModel
import org.digma.intellij.plugin.ui.list.PanelList
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

class InsightsList(project: Project, listViewItems: List<ListViewItem<*>>) :
    PanelList(project, MyModel(listViewItems)) {

    init {
        this.setCellRenderer(InsightsListCellRenderer())
    }

    private class MyModel(listViewItems: List<ListViewItem<*>>) : AbstractPanelListModel() {
        init {
            setListData(listViewItems)
        }

        //flatten the original list to single ListViewItem so InsightsListCellRenderer only deals with
        //insights panels and title panels
        override fun setListData(listViewItems: List<ListViewItem<*>>) {
            val newViewItems = ArrayList<ListViewItem<*>>()
            var index = 0
            listViewItems.forEach { value ->
                if (value is InsightGroupListViewItem) {
                    val groupTitle = ListViewItem(GroupTitleModel(value.groupId,value.type),index++)
                    groupTitle.moreData.putAll(value.moreData)
                    newViewItems.add(groupTitle)// the group title item

                    value.modelObject.forEach{item ->
                        addItem(item, index++, newViewItems)
                    }
                } else {
                    addItem(value, index++, newViewItems)
                }
            }

            super.setListData(newViewItems)
        }

        private fun addItem(item: ListViewItem<*>,
                            index: Int,
                            newViewItems: ArrayList<ListViewItem<*>>) {
            val newItem = ListViewItem(item.modelObject, index)
            newItem.moreData.putAll(item.moreData)
            newViewItems.add(newItem)
        }
    }



    class GroupTitleModel(val groupId: String, val type: InsightGroupType)

}