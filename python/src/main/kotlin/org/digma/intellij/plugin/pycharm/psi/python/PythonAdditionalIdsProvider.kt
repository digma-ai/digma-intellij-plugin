package org.digma.intellij.plugin.pycharm.psi.python

import org.digma.intellij.plugin.model.discovery.MethodInfo

class PythonAdditionalIdsProvider:MethodInfo.AdditionalIdsProvider {
    override fun provideAdditionalIds(methodInfo: MethodInfo): List<String> {

        var id = methodInfo.id
        val additionalIds = mutableListOf<String>()
        var i = id.indexOf('/')
        while (i >= 0){
            id = id.substringAfter('/')
            additionalIds.add("method:$id")
            i = id.indexOf('/')
        }
        return additionalIds
    }
}