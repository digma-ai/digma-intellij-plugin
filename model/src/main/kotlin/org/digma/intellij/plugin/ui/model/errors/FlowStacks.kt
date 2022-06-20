package org.digma.intellij.plugin.ui.model.errors

import org.digma.intellij.plugin.ui.model.listview.ListViewItem

class FlowStacks {
    var current = 0
    var stacks: List<List<ListViewItem<FrameListViewItem>>> = ArrayList()

    fun goBack() {
        if (stacks.size == 0){
            current = 0
        }else if (current == 0){
            current = stacks.size-1
        }else{
            current--
        }
    }

    fun goForward() {
        if (stacks.size == 0){
            current = 0
        }else if (current == stacks.size-1){
            current = 0
        }else{
            current++
        }
    }

    fun getCurrentStack(): List<ListViewItem<*>> {
        if (stacks.isEmpty()){
            return ArrayList()
        }
        return stacks[current]
    }

}