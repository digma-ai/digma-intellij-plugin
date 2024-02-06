package org.digma.intellij.plugin.ui.navigation

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.jcef.BaseResourceHandler
import java.io.InputStream

class NavigationResourceHandler(val project: Project, path: String) : BaseResourceHandler(path) {

    override fun isIndexHtml(path: String): Boolean {
        return path.endsWith("index.html", true)
    }

    override fun buildIndexFromTemplate(path: String): InputStream? {
        return NavigationIndexTemplateBuilder().build(project)
    }
}
