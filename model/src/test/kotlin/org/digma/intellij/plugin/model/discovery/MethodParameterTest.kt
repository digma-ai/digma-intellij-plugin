package org.digma.intellij.plugin.model.discovery

import kotlin.test.Test
import kotlin.test.assertEquals

class MethodParameterTest {

    @Test
    fun typeShortNameShouldWorkOnSimpleType() {
        assertTypeShortName("System.String", "String")
        assertTypeShortName("System.Int32", "Int32")
        assertTypeShortName("Hello.World", "World")
        assertTypeShortName("Abc", "Abc")
    }

    @Test
    fun typeShortNameShouldWorkOnArray() {
        assertTypeShortName("System.String[]", "String[]")
        assertTypeShortName("System.Int32[]", "Int32[]")
        assertTypeShortName("Hello.World[]", "World[]")
        assertTypeShortName("Abc[]", "Abc[]")
    }

    @Test
    fun typeShortNameShouldWorkOnMultiDimensionalArrays() {
        assertTypeShortName("System.String[,]", "String[,]")
        assertTypeShortName("System.Int32[,]", "Int32[,]")
        assertTypeShortName("Hello.World[,]", "World[,]")
        assertTypeShortName("Abc[,]", "Abc[,]")
    }

    @Test
    fun typeShortNameShouldWorkOnJaggedArrays() {
        assertTypeShortName("System.String[][]", "String[][]")
        assertTypeShortName("System.Int32[][][]", "Int32[][][]")
        assertTypeShortName("Hello.World[][][][]", "World[][][][]")
        assertTypeShortName("Abc[][][][][]", "Abc[][][][][]")
    }

    @Test
    fun typeShortNameShouldWorkOnMixOfJaggedAndMultiDimensionalArrays() {
        assertTypeShortName("System.String[,,][]", "String[,,][]")
        assertTypeShortName("System.Int32[][][,]", "Int32[][][,]")
        assertTypeShortName("Hello.World[][][,,,][]", "World[][][,,,][]")
        assertTypeShortName("Abc[,,][][][,][]", "Abc[,,][][][,][]")
    }

    @Test
    fun typeShortNameWithGenerics() {
        // parameter defined as "IList<string> names"
        assertTypeShortName("System.Collections.Generic.IList`1[T -> System.String]", "IList`1")
    }

    @Test
    fun typeShortNameWithListOfArray() {
        // parameter defined as "IList<string[]> listOfArray"
        assertTypeShortName("System.Collections.Generic.IList`1[T -> System.String[]]", "IList`1")
    }

    @Test
    fun typeShortNameWithArrayOfList() {
        // parameter defined as "IList<int>[] arrayOfList"
        assertTypeShortName("System.Collections.Generic.IList`1[T -> System.Int32][]", "IList`1[]")
    }

    private fun assertTypeShortName(typeFqn: String, expectedShortName: String) {
        val mp = MethodParameter(typeFqn, "whatever")
        assertEquals(expectedShortName, mp.typeShortName(), "typeShortName")
    }

}