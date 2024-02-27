package org.digma.intellij.plugin.bulklistener

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.log.Log
import java.util.function.Supplier

abstract class AbstractBulkFileChangeListener : BulkFileListener {


    protected val logger: Logger = Logger.getInstance(this::class.java)

    override fun after(events: MutableList<out VFileEvent>) {

        Log.log(logger::trace, "got after with {} events", events.size)

        Backgroundable.executeOnPooledThread {
            ProjectUtil.getOpenProjects().forEach { project ->
                if (isProjectValid(project)) {
                    processEvents(project, events)
                }
            }
        }
    }

    abstract fun processEvents(project: Project, events: List<VFileEvent>)


    protected fun isRelevantFile(project: Project, file: VirtualFile?): Boolean {
        return isProjectValid(project) &&
                file != null &&
                !file.isDirectory


    }

    protected fun isValidRelevantFile(project: Project, file: VirtualFile?): Boolean {
        return isRelevantFile(project, file) &&
                isValidVirtualFile(file) &&
                file != null &&
                isInContent(project, file)

    }


    protected fun isInContent(project: Project, file: VirtualFile): Boolean {
        return ReadActions.ensureReadAction(Supplier {
            ProjectFileIndex.getInstance(project).isInContent(file)
        })
    }
}