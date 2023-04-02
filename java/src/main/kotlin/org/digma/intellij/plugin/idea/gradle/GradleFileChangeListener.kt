package org.digma.intellij.plugin.idea.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class GradleFileChangeListener(val project: Project) : BulkFileListener {

    init {
        System.out.println("$javaClass been initialized")
    }

    override fun after(events: MutableList<out VFileEvent>) {
        events.forEach {
            if (it.file == null) {
                return // continue loop
            }
            val fileName = it.file!!.name

            if (fileName.equals("build.gradle") || fileName.endsWith(".gradle.kts", false)) {

            }
            System.out.println("abcd ${it.file}")
        }

    }
}