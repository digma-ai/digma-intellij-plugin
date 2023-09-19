package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.jcef.BaseResourceHandler
import java.io.InputStream

class RecentActivityResourceHandler(val project: Project, path: String) :
    BaseResourceHandler(path) {


    override fun isIndexHtml(path: String): Boolean {
        return path.endsWith("index.html", true)
    }

    override fun buildIndexFromTemplate(path: String): InputStream? {
        return RecentActivityIndexTemplateBuilder().build(project)
    }
}