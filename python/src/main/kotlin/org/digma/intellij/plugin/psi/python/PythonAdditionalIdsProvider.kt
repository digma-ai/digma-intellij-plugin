package org.digma.intellij.plugin.psi.python

import org.digma.intellij.plugin.model.discovery.MethodInfo

class PythonAdditionalIdsProvider:MethodInfo.AdditionalIdsProvider {


    override fun provideAdditionalIdsWithType(methodInfo: MethodInfo): List<String> {
        return provideAdditionalIds(methodInfo,true)
    }

    override fun provideAdditionalIdsWithoutType(methodInfo: MethodInfo): List<String> {
        return provideAdditionalIds(methodInfo,false)
    }

    private fun provideAdditionalIds(methodInfo: MethodInfo, withType: Boolean): List<String>{
        return getAdditionalIds(methodInfo.id,withType)
    }

    companion object {
        @JvmStatic
        fun getAdditionalIdsInclusive(codeObjectId: String, withType: Boolean): MutableList<String> {
            val ids = getAdditionalIds(codeObjectId,withType)
            ids.add(codeObjectId)
            return ids
        }

        @JvmStatic
        fun getAdditionalIds(codeObjectId: String, withType: Boolean): MutableList<String> {
            var id = codeObjectId
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
}