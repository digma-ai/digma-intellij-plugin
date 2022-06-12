@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.env

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.ui.model.environment.EnvComboModel
import javax.swing.JPanel
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener


//todo: how to persist the selected env between restarts

@Suppress("UNCHECKED_CAST")
fun envCombo(project: Project): JPanel {


    return panel {
        panel {
            row {
                label("Environment:")
                comboBox(EnvComboModel)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .apply {
                        component.toolTipText = "Environment"
                        horizontalAlign(HorizontalAlign.FILL)
//                        component.addActionListener {
//                            it.source.apply {
//                                val combo: ComboBox<String> = this as ComboBox<String>
//                                env.current = combo.item
//                            }
//                        }
//
                        component.addPopupMenuListener(object: PopupMenuListener{
                            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                                EnvComboModel.refreshEnvironments()
                            }

                            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {}

                            override fun popupMenuCanceled(e: PopupMenuEvent?) {}

                        })
                    }
            }.layout(RowLayout.LABEL_ALIGNED)
        }
    }

}