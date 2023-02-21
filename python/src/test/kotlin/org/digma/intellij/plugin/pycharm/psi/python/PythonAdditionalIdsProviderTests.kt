package org.digma.intellij.plugin.pycharm.psi.python

import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.junit.jupiter.api.Test
import kotlin.test.assertContains


internal class PythonAdditionalIdsProviderTests {

    @Test
    fun testAdditionalIds(){
        val methodInfo = MethodInfo("project-root/test/folder/main.py\$_\$myFunction","myFunction","myClass","my.namespace.MyClass","file://my/file.uri",10,listOf())
        val ids = PythonAdditionalIdsProvider().provideAdditionalIds(methodInfo)

        assert(ids.size == 3)
        assertContains(ids,"method:test/folder/main.py\$_\$myFunction")
        assertContains(ids,"method:folder/main.py\$_\$myFunction")
        assertContains(ids,"method:main.py\$_\$myFunction")
    }

    @Test
    fun testAdditionalIdsMethodInfo(){
        val methodInfo = MethodInfo("project-root/test/folder/main.py\$_\$myFunction","myFunction","myClass","my.namespace.MyClass","file://my/file.uri",10,listOf())
        methodInfo.additionalIdsProvider = PythonAdditionalIdsProvider()

        val ids = methodInfo.allIds()

        assert(ids.size == 4)
        assertContains(ids,"method:project-root/test/folder/main.py\$_\$myFunction")
        assertContains(ids,"method:test/folder/main.py\$_\$myFunction")
        assertContains(ids,"method:folder/main.py\$_\$myFunction")
        assertContains(ids,"method:main.py\$_\$myFunction")
    }

}