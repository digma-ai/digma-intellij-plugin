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


fun create2025EAPMessagePanel(project:Project): JPanel {

    val mainPanel = JPanel(BorderLayout())
    mainPanel.isOpaque = false
    mainPanel.border = empty()
    mainPanel.background = listBackground()

    val htmlText = getMessageHtml()
    val textPane = createTextPaneWithHtml(htmlText)

    mainPanel.add(textPane, BorderLayout.CENTER)

    val slackPanel = createSlackLinkPanel(project)
    mainPanel.add(slackPanel,BorderLayout.SOUTH)

    return wrapWithScrollable(mainPanel)
}


fun getMessageHtml(): String {

    val title = "Digma 2025.1 EAP workaround"
    val paragraph = "The latest Jetbrains 2025.1 EAP has an issue with JCEF that prevents Digma from working," +
            "Please add the following system properties to fix, and restart your IDE:"
    val paragraph2 = "Open the Idea help menu, search for 'Edit Custom Properties', add 'jcef.remote.enabled=false' and restart your IDE"

    return "<html>" +
            "<head>" +
            "<style>" +
            "h3 {text-align: center;}" +
            "p {text-align: center;}" +
            "div {text-align: center;}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<h3>$title</h3>" +
            "<p>$paragraph</p>" +
            "<p>$paragraph2</p>" +
            "</body>" +
            "</html>"
}

