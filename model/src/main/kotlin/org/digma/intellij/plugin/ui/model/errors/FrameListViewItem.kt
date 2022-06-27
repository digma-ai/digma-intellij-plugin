package org.digma.intellij.plugin.ui.model.errors

import org.digma.intellij.plugin.model.rest.errordetails.Frame
import org.digma.intellij.plugin.model.rest.errordetails.FrameStack
import java.net.URL

interface FrameListViewItem{}

class FrameStackTitle(val frameStack: FrameStack): FrameListViewItem{

}

class SpanTitle(val spanName: String): FrameListViewItem{

}


class FrameItem(val frameStack: FrameStack,
                val frame: Frame,
                val first: Boolean,
                private val workspaceUri: String?,
                val lastInstanceCommitId: String?) : FrameListViewItem {

    fun isInWorkspace(): Boolean {
        if (workspaceUri == null) {
            return false
        }
        return true
    }

    fun getWorkspaceUrl(): URL? {
        return workspaceUri?.let {
            URL(workspaceUri)
        }
    }

}
