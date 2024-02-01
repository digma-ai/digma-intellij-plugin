package org.digma.intellij.plugin.ui.assets

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.jcef.BaseResourceHandler
import java.io.InputStream

class AssetsResourceHandler(val project: Project, path: String) : BaseResourceHandler(path) {

    override fun isIndexHtml(path: String): Boolean {
        return path.endsWith("index.html", true)
    }

    override fun buildIndexFromTemplate(path: String): InputStream? {
        return AssetsIndexTemplateBuilder().build(project)
    }
}
