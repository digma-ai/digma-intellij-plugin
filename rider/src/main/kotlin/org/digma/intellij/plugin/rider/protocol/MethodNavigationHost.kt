package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.project.Project
import com.jetbrains.rider.projectView.solution
import kotlin.random.Random

class MethodNavigationHost(private val project: Project) {

    private var model: MethodNavigationModel = project.solution.methodNavigationModel


    fun navigateToMethod(codeObjectId: String) {
        //the message needs to be unique. if a message is the same as the previous one the event is not fired
        val message = "{${Random.nextInt()}}$codeObjectId"
        model.navigateToMethod.set(message)
    }


}