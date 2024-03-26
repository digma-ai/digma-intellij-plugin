package org.digma.intellij.plugin.buildsystem

interface BuildSystemHelperService {

    fun isBuildSystem(externalSystemId: String): Boolean

}