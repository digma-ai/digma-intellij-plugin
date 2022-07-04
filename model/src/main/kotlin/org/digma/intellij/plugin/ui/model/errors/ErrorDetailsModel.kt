package org.digma.intellij.plugin.ui.model.errors

import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails

class ErrorDetailsModel {

    var delegate: CodeObjectErrorDetails? = null
    var flowStacks: FlowStacks = FlowStacks()
    var methodInfo: MethodInfo? = null


    fun getName(): String{
        if (delegate == null){
            return ""
        }
        val name = delegate?.name ?: ""
        return name.ifBlank { "" }
    }

    fun getFrom(): String {

        if (delegate == null){
            return ""
        }

        return if (delegate?.sourceCodeObjectId == methodInfo?.id){
            "me"
        }else{
            delegate?.sourceCodeObjectId?.substringAfterLast("\$_\$") ?: ""
        }
    }

}