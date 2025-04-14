package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.common.statuspanels.createSlackLinkPanel
import org.digma.intellij.plugin.ui.common.statuspanels.createTextPaneWithHtml
import org.digma.intellij.plugin.ui.common.statuspanels.wrapWithScrollable
import org.digma.intellij.plugin.ui.list.listBackground
import java.awt.BorderLayout
import javax.swing.JPanel


fun is2025EAPWithJCEFRemoteEnabled(): Boolean {

    return if (ApplicationInfo.getInstance().build.baselineVersion == 251){
        //just touch it so it will initialize static variables
        val isJcefSupported = JBCefApp.isSupported()
        //do something with the variable so that the compiler will not optimize and remove this code
        println("isJCEFSupported = $isJcefSupported")

        //this is the return value
        java.lang.Boolean.getBoolean("jcef.remote.enabled")

    } else {
        false
    }
}


fun sendPosthogEvent(appName:String) {

    findActiveProject()?.let { project ->
        ActivityMonitor.getInstance(project).register251EAPPatchEvent(appName)
    }

}

