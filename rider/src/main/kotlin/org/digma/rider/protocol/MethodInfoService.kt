package org.digma.rider.protocol

import com.intellij.openapi.project.Project
import com.jetbrains.rdclient.util.idea.callSynchronously
import com.jetbrains.rider.projectView.solution

@Deprecated("not used")
class MethodInfoService(project: Project) {
    private val model = project.solution.methodInfoModel

    fun getMethodUnderCaret(): MethodInfo? = model.getMethodUnderCaret.callSynchronously(Unit, model.protocol)
}