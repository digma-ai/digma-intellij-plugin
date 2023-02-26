package org.digma.intellij.plugin.pycharm.psi.python

import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.junit.jupiter.api.Test
import kotlin.test.assertContains


internal class PythonAdditionalIdsProviderTests {

    @Test
    fun testAdditionalIdsWithType(){
        val methodInfo = MethodInfo("project-root/test/folder/main.py\$_\$myFunction","myFunction","myClass","my.namespace.MyClass","file://my/file.uri",10,listOf())
        val ids = PythonAdditionalIdsProvider().provideAdditionalIdsWithType(methodInfo)

        assert(ids.size == 3)
        assertContains(ids,"method:test/folder/main.py\$_\$myFunction")
        assertContains(ids,"method:folder/main.py\$_\$myFunction")
        assertContains(ids,"method:main.py\$_\$myFunction")
    }

    @Test
    fun testAdditionalIdsWithoutType(){
        val methodInfo = MethodInfo("project-root/test/folder/main.py\$_\$myFunction","myFunction","myClass","my.namespace.MyClass","file://my/file.uri",10,listOf())
        val ids = PythonAdditionalIdsProvider().provideAdditionalIdsWithoutType(methodInfo)

        assert(ids.size == 3)
        assertContains(ids,"test/folder/main.py\$_\$myFunction")
        assertContains(ids,"folder/main.py\$_\$myFunction")
        assertContains(ids,"main.py\$_\$myFunction")
    }

    @Test
    fun testAdditionalIdsMethodInfoWithType(){
        val methodInfo = MethodInfo("project-root/test/folder/main.py\$_\$myFunction","myFunction","myClass","my.namespace.MyClass","file://my/file.uri",10,listOf())
        methodInfo.additionalIdsProvider = PythonAdditionalIdsProvider()

        val ids = methodInfo.allIdsWithType()

        assert(ids.size == 4)
        assertContains(ids,"method:project-root/test/folder/main.py\$_\$myFunction")
        assertContains(ids,"method:test/folder/main.py\$_\$myFunction")
        assertContains(ids,"method:folder/main.py\$_\$myFunction")
        assertContains(ids,"method:main.py\$_\$myFunction")
    }

    @Test
    fun testAdditionalIdsMethodInfoWithoutType(){
        val methodInfo = MethodInfo("project-root/test/folder/main.py\$_\$myFunction","myFunction","myClass","my.namespace.MyClass","file://my/file.uri",10,listOf())
        methodInfo.additionalIdsProvider = PythonAdditionalIdsProvider()

        val ids = methodInfo.allIdsWithoutType()

        assert(ids.size == 4)
        assertContains(ids,"project-root/test/folder/main.py\$_\$myFunction")
        assertContains(ids,"test/folder/main.py\$_\$myFunction")
        assertContains(ids,"folder/main.py\$_\$myFunction")
        assertContains(ids,"main.py\$_\$myFunction")
    }

}