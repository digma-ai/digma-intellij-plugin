package org.digma.intellij.plugin.ui.model.errors

import org.digma.intellij.plugin.model.rest.errordetails.Frame
import org.digma.intellij.plugin.model.rest.errordetails.FrameStack

interface FrameListViewItem{}

class FrameStackTitle(val frameStack: FrameStack): FrameListViewItem{

}

class SpanTitle(val spanName: String): FrameListViewItem{

}


class FrameItem(val frameStack: FrameStack,
                val frame: Frame,
                val first: Boolean): FrameListViewItem {

    var workspaceUrl: String? = null

}



//enum class FrameListViewItemType{
//    FrameStackTitle,SpanTitle,Frame
//}