package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.SystemInfo
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


/*
this file is utilities to work around jcef issues in 2025.1 and above until the issues are fixed by jetbrains.
https://youtrack.jetbrains.com/issue/IJPL-187065/Out-of-process-JCEF-http-requests-are-corrupted
 */


class DisableJcefRemoteProjectActivity : ProjectActivity {

    /*
        Note: this is aggressive! we change the behavior that jetbrains devs want for intellij 2025, without user consent.

        to try to set jcef.remote.enabled=false early on startup.
        in 251 and above JBCefApp will set jcef.remote.enabled=true for Mac and Windows and probably for
        linux that don't use wayland.
        after JBCefApp is initialized, it is not possible to change that anymore because it will
        initialize a static variable and not use the system property anymore.
        when jcef.remote.enabled=true jcef runs out-of-process. there are rendering issues and http issues.
        to disable out-of-process jcef users need to set jcef.remote.enabled=false,
        our tool windows will show a message to users to do that when jcef.remote.enabled=true,
        but it's annoying and some users may not want to do it.
        here we try to set jcef.remote.enabled=false in case it is not set yet.
        it is not guaranteed to succeed, it depends if JBCefApp already initialized,
        it will initialize if another plugin or intellij code already called some code on JBCefApp like
        JBCefApp.isSupported().
        if that didn't succeed the user will see our workaround message.
    */

    override suspend fun execute(project: Project) {
        tryDisableJcefRemote()
    }
}


fun tryDisableJcefRemote(){
    if (ApplicationInfo.getInstance().build.baselineVersion >= 251) {
        if (System.getProperty("jcef.remote.enabled") == null) {
            if (SystemInfo.isMac || SystemInfo.isWindows ||
                (SystemInfo.isLinux && !SystemInfo.isWayland)
            ) {
                System.setProperty("jcef.remote.enabled", "false")
            }
        }
    }
}



fun is2025EAPWithJCEFRemoteEnabled(): Boolean {

    //try to disable jcef remote before checking it, this is called from our tool windows
    tryDisableJcefRemote()

    return if (ApplicationInfo.getInstance().build.baselineVersion >= 251) {
        //just touch JBCefApp so it will initialize static variables and will probably set jcef.remote.enabled=true
        val isJcefSupported = JBCefApp.isSupported()
        //do something with the variable so that the compiler will not optimize and remove this code
        println("isJCEFSupported = $isJcefSupported")

        //this is the return value
        java.lang.Boolean.getBoolean("jcef.remote.enabled")

    } else {
        false
    }
}


fun sendPosthogEvent(appName: String) {

    findActiveProject()?.let { project ->
        ActivityMonitor.getInstance(project).register251EAPPatchEvent(appName)
    }

}


fun create2025EAPMessagePanel(project: Project): JPanel {

    val mainPanel = JPanel(BorderLayout())
    mainPanel.isOpaque = false
    mainPanel.border = empty()
    mainPanel.background = listBackground()

    val htmlText = getMessageHtml()
    val textPane = createTextPaneWithHtml(htmlText)

    mainPanel.add(textPane, BorderLayout.CENTER)

    val slackPanel = createSlackLinkPanel(project)
    mainPanel.add(slackPanel, BorderLayout.SOUTH)

    return wrapWithScrollable(mainPanel)
}


fun getMessageHtml(): String {

    val title = "Digma 2025.* workaround"
    val paragraph = "The latest Jetbrains 2025.* has an issue with JCEF that prevents Digma from working," +
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

