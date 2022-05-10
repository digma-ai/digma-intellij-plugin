@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.env

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.analytics.Environment
import javax.swing.JPanel


//todo: how to persist the selected env between restarts



@Suppress("UNCHECKED_CAST")
fun envCombo(project: Project): JPanel {

    val envService: Environment = project.getService(Environment::class.java)
    val environments = envService.environments

    return panel {
        panel {
            row {
                label("Env:")
                comboBox(environments.toTypedArray())
                    .horizontalAlign(HorizontalAlign.FILL)
                    .apply {
                        horizontalAlign(HorizontalAlign.FILL)
                        component.addActionListener {
                            it.source.apply {
                                val combo: ComboBox<String> = this as ComboBox<String>
                                envService.current = combo.item
                            }

                        }
                    }
            }.layout(RowLayout.LABEL_ALIGNED)
        }
    }

}