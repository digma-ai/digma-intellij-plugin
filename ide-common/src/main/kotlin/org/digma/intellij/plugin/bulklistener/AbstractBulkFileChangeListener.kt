package org.digma.intellij.plugin.bulklistener

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.log.Log

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

}