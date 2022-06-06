@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.env

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.Environment
import javax.swing.JPanel


//todo: how to persist the selected env between restarts

@Suppress("UNCHECKED_CAST")
fun envCombo(project: Project): JPanel {

    val env: Environment = project.getService(AnalyticsService::class.java).environment
    val environments = env.environments

    //out design is a bit strange, we have two combobox UI components, one on each panel.
    //to make them use the same list and trigger the same selection they both use the same
    //singleton combobox model
    EnvComboModel.updateEnvironments(environments)

    return panel {
        panel {
            row {
                label("Environment:")
                comboBox(EnvComboModel)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .apply {
                        component.toolTipText = "Environment"
                        horizontalAlign(HorizontalAlign.FILL)
                        component.addActionListener {
                            it.source.apply {
                                val combo: ComboBox<String> = this as ComboBox<String>
                                env.current = combo.item
                            }

                        }
                    }
            }.layout(RowLayout.LABEL_ALIGNED)
        }
    }

}