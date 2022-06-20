package org.digma.intellij.plugin.ui.model.errors

import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails

class ErrorDetailsModel {

    var delegate: CodeObjectErrorDetails? = null
    var flowStacks: FlowStacks? = FlowStacks()
    var methodInfo: MethodInfo? = null



    fun createErrorName(): String{
        val name = delegate?.name +" "+ getFrom()
        return name.ifBlank { "" }
    }



    private fun getFrom(): String {

        val from: String?

        if (delegate?.sourceCodeObjectId == methodInfo?.id){
            from = "From me"
        }else{
            from = "From "+delegate?.sourceCodeObjectId?.substringAfterLast("\$_\$")
        }

        return from
    }


}