package org.digma.intellij.plugin.ui.runonce

import com.intellij.ide.util.RunOnceUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.DialogWrapper
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator
import javax.swing.Action
import javax.swing.JComponent

class LostDataMessage : StartupActivity {

    override fun runActivity(project: Project) {
        RunOnceUtil.runOnceForApp("org.digma.runonce.lostdatamessage") {
            project.getService(DumbService::class.java)
                .runWhenSmart {
                    EDT.ensureEDT {
                        openLostMessageDialog(project)
                    }
                }
        }
    }

    private fun openLostMessageDialog(project: Project) {
        LostDataMessageDialog(project).show()
    }
}


class LostDataMessageDialog(project: Project) : DialogWrapper(project) {

    init {
        super.init()
        title = "Your Digma Data Will Be Lost"
    }

    override fun createCenterPanel(): JComponent {
        return JBCefBrowserBuilderCreator.create()
            .setUrl("https://digma.ai/digma-update")
            .build().component
    }


    override fun createActions(): Array<Action> {
        return arrayOf(okAction)
    }

}









