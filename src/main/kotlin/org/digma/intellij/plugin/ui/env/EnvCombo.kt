@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.env

import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.ui.model.environment.EnvComboModel
import javax.swing.JPanel
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener


@Suppress("UNCHECKED_CAST")
fun envCombo(): JPanel {

    return panel {
        panel {
            row {
                label("Environment:")
                comboBox(EnvComboModel)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .apply {
                        component.toolTipText = "Environment"
                        component.isOpaque = false
                        component.addPopupMenuListener(object: PopupMenuListener{
                            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                                EnvComboModel.refreshEnvironments()
                            }

                            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {}

                            override fun popupMenuCanceled(e: PopupMenuEvent?) {}

                        })
                    }
            }
        }
    }.andTransparent()

}