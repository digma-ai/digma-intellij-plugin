package org.digma.intellij.plugin.bulklistener

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

abstract class AbstractBulkFileChangeListener : BulkFileListener {

    override fun after(events: MutableList<out VFileEvent>) {
        ProjectUtil.getOpenProjects().forEach { project ->
            if (!project.isDisposed) {
                processEvents(project, events)
            }
        }
    }

    abstract fun processEvents(project: Project, events: List<VFileEvent>)


    protected fun isRelevantFile(project: Project, file: VirtualFile?): Boolean {
        return file != null && file.isValid && !file.isDirectory && ProjectRootManager.getInstance(project).fileIndex.isInContent(file)
    }
}