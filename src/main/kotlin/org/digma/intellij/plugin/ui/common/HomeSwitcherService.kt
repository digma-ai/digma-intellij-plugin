package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class HomeSwitcherService {

    private var homeButton:HomeButton? = null

    fun setButton(homeButton: HomeButton) {
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