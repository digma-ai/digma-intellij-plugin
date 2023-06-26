package org.digma.intellij.plugin.navigation

import com.intellij.openapi.components.Service
import javax.swing.JToggleButton

@Service(Service.Level.PROJECT)
class HomeSwitcherService {

    private var homeButton:JToggleButton? = null

    fun setButton(homeButton: JToggleButton) {
        this.homeButton = homeButton
    }


    fun switchToInsights(){
        homeButton?.let {
            it.isSelected = false
        }
    }

    fun switchToHome(){
        homeButton?.let {
            it.isSelected = true
        }
    }


}