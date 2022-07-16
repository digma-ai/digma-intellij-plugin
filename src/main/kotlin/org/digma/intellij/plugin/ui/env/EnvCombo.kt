@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.env

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.service.EnvComboModelService
import javax.swing.JPanel
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener


@Suppress("UNCHECKED_CAST")
fun envCombo(project: Project): JPanel {

    val envComboModelService = project.getService(EnvComboModelService::class.java)

    return panel {
        panel {
            row {
                label("Environment:")
                comboBox(envComboModelService.envComboModel)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .apply {
                        component.toolTipText = "Environment"
                        component.isOpaque = false
                        component.addPopupMenuListener(object: PopupMenuListener{
                            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                                envComboModelService.envComboModel.refreshEnvironments()
                            }

                            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {}

                            override fun popupMenuCanceled(e: PopupMenuEvent?) {}

                        })
                    }
            }
        }
    }.andTransparent()

}