package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser

const val JCEF_PROJECT_PROPERTY_NAME = "Digma.PROJECT_PROPERTY_NAME"
const val JCEF_JAEGER_UI_FILE_PROPERTY_NAME = "Digma.JAEGER_UI_FILE_PROPERTY_NAME"
const val JCEF_DOCUMENTATION_FILE_PROPERTY_NAME = "Digma.DOCUMENTATION_FILE_PROPERTY_NAME"


fun setProject(jbCefBrowser: JBCefBrowser, project: Project) {
    jbCefBrowser.setProperty(JCEF_PROJECT_PROPERTY_NAME, project)
}

fun getProject(cefBrowser: CefBrowser?): Project? {
    return cefBrowser?.let { cb ->
        JBCefBrowser.getJBCefBrowser(cb)?.let { jbCefBrowser ->
            getProject(jbCefBrowser)
        }
    }
}

fun getProject(jbCefBrowser: JBCefBrowser): Project {
    return jbCefBrowser.getProperty(JCEF_PROJECT_PROPERTY_NAME) as Project
}

fun setProperty(cefBrowser: CefBrowser, key: String, value: Any) {
    JBCefBrowser.getJBCefBrowser(cefBrowser)?.let { jbCefBrowser ->
        setProperty(jbCefBrowser,key, value)
    }
}

fun setProperty(jbCefBrowser: JBCefBrowser, key: String, value: Any) {
    jbCefBrowser.setProperty(key, value)
}

fun getProperty(cefBrowser: CefBrowser, key: String): Any? {
    return JBCefBrowser.getJBCefBrowser(cefBrowser)?.let { jbCefBrowser ->
        getProperty(jbCefBrowser,key)
    }
}

fun getProperty(jbCefBrowser: JBCefBrowser, key: String): Any? {
    return jbCefBrowser.getProperty(key)
}

