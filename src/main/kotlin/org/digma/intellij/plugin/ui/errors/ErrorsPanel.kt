@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.ui.env.envCombo
import org.digma.intellij.plugin.ui.model.ErrorsModel
import org.digma.intellij.plugin.ui.model.InsightsModel

val errorsModel: ErrorsModel = ErrorsModel()


fun errorsPanel(project: Project): DialogPanel {

    var result = panel {

        row {
            cell(envCombo(project))
        }
        panel {
            row {
                label("Scope: some scope..")
            }

        }
    }


    return result
}

