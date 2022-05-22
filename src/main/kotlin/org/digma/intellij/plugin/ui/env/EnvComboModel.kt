package org.digma.intellij.plugin.ui.env

import org.jetbrains.annotations.NotNull
import javax.swing.DefaultComboBoxModel

internal object EnvComboModel: DefaultComboBoxModel<String>(){

     var initialized: Boolean = false

    @Synchronized fun updateEnvironments(@NotNull envs: List<String>){
        if (initialized)
            return

        initialized = true
        envs.forEach {
            this.addElement(it)
        }

        envs.firstOrNull()?.let {
            selectedItem = envs.first()
        }

    }

}