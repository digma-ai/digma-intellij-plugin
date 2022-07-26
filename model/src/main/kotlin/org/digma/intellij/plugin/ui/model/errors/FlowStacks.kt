package org.digma.intellij.plugin.ui.model.errors

import org.digma.intellij.plugin.ui.model.listview.ListViewItem

class FlowStacks {
    var current = 0
    var stacks: List<List<ListViewItem<FrameListViewItem>>> = ArrayList()
    var isWorkspaceOnly = false


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
        return if (isWorkspaceOnly){
            filterWorkspaceOnly(stacks[current])
        }else {
            stacks[current]
        }
    }

    private fun filterWorkspaceOnly(currentStack: List<ListViewItem<FrameListViewItem>>): List<ListViewItem<*>> {

        val result = ArrayList<ListViewItem<*>>()

        for (n in currentStack.withIndex()){

            val toAdd =
                when (n.value.modelObject) {
                    is FrameStackTitle -> {
                        //always add FrameStackTitle
                        n.value
                    }
                    is SpanTitle -> {
                        //check if we need to add this span title , if the frame following it has workspaceUri
                        val index = n.index
                        //element after span title must be FrameItem, if that fails then we have a bug when the list was built.
                        //if it fails on index out of bounds then we have a bug too
                        val frame: FrameItem = currentStack[index+1].modelObject as FrameItem
                        if(frame.isInWorkspace()) n.value else null
                    }
                    is FrameItem -> {
                        val frame: FrameItem = n.value.modelObject as FrameItem
                        if(frame.isInWorkspace()) n.value else null
                    }
                    else -> throw RuntimeException("Unknown modelObject ${n.value.modelObject}")
                }

            if (toAdd != null){
                result.add(toAdd)
            }

        }

        return result

    }


}