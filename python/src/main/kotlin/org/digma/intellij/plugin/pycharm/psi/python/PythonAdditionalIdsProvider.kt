package org.digma.intellij.plugin.pycharm.psi.python

import org.digma.intellij.plugin.model.discovery.MethodInfo

class PythonAdditionalIdsProvider:MethodInfo.AdditionalIdsProvider {


    override fun provideAdditionalIdsWithType(methodInfo: MethodInfo): List<String> {
        return provideAdditionalIds(methodInfo,true)
    }

    override fun provideAdditionalIdsWithoutType(methodInfo: MethodInfo): List<String> {
        return provideAdditionalIds(methodInfo,false)
    }



    private fun provideAdditionalIds(methodInfo: MethodInfo, withType: Boolean): List<String>{
        var id = methodInfo.id
        val additionalIds = mutableListOf<String>()
        var i = id.indexOf('/')
        while (i >= 0){
            id = id.substringAfter('/')
            val additionalId = if(withType) "method:$id" else id
            additionalIds.add(additionalId)
            i = id.indexOf('/')
        }
        return additionalIds
    }
}