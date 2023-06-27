package org.digma.intellij.plugin.navigation

import com.intellij.openapi.components.Service
import javax.swing.JToggleButton

/**
 * switches between home (assets/dashboard) and insights (insights/errors)
 * doing that by changing state of the [org.digma.intellij.plugin.ui.common.HomeButton] which will react and change
 * card
 */
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