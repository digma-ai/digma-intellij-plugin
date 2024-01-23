package org.digma.intellij.plugin.idea.runcfg

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(JavaToolOptionsMergeUtils::class)
class JavaToolOptionsMergeTests {

    @Test

    fun javaToolOptionsToMapTest1() {

        val javaToolOptions =
            "-javaagent:/my/path/my.jar=a=b,c=d -javaagent:/my/path/myOther.jar -DmyProp1=myValue1 -DmyProp2=myValue2 -DmyProp3 -DmyProp4= -Xmx10g"
        val optionsMap = javaToolOptionsToMap(javaToolOptions)

        assertEquals(optionsMap["-javaagent:/my/path/my.jar"], "a=b,c=d")
        assertEquals(optionsMap["-javaagent:/my/path/myOther.jar"], "")
        assertEquals(optionsMap["-DmyProp1"], "myValue1")
        assertEquals(optionsMap["-DmyProp2"], "myValue2")
        assertEquals(optionsMap["-DmyProp3"], "")
        assertEquals(optionsMap["-DmyProp4"], "")
        assertNull(optionsMap["-Xmx10g"])
    }

    @Test
    fun javaToolOptionsToMapTest2() {

        val javaToolOptions =
            "-javaagent:/my/path/my.jar=a=b,c=d -javaagent:/my/path/my.jar -DmyProp1=myValue1 -DmyProp2=myValue2 -DmyProp3 -DmyProp4= -Xmx10g"
        val optionsMap = javaToolOptionsToMap(javaToolOptions)

        assertEquals(optionsMap["-javaagent:/my/path/my.jar"], "") //it appears twice, last one wins
        assertEquals(optionsMap["-DmyProp1"], "myValue1")
        assertEquals(optionsMap["-DmyProp2"], "myValue2")
        assertEquals(optionsMap["-DmyProp3"], "")
        assertEquals(optionsMap["-DmyProp4"], "")
        assertNull(optionsMap["-Xmx10g"])
    }


    @Test
    fun javaToolOptionsToMapTest3() {

        val javaToolOptions =
            "-javaagent:/my/path/my.jar -agentpath:pathname=o=p -agentlib:libname --show-version -DmyProp1=myValue1 -DmyProp2=myValue2 -DmyProp3 -DmyProp4= -Xmx10g -Xdock:icon=path_to_icon_file -XX:AllocateHeapAt=path"
        val optionsMap = javaToolOptionsToMap(javaToolOptions)

        assertEquals(optionsMap["-javaagent:/my/path/my.jar"], "")
        assertEquals(optionsMap["-agentpath:pathname"], "o=p")
        assertEquals(optionsMap["-agentlib:libname"], "")
        assertEquals(optionsMap["-DmyProp1"], "myValue1")
        assertEquals(optionsMap["-DmyProp2"], "myValue2")
        assertEquals(optionsMap["-DmyProp3"], "")
        assertEquals(optionsMap["-DmyProp4"], "")
        assertNull(optionsMap["--show-version"])
        assertNull(optionsMap["-Xmx10g"])
        assertNull(optionsMap["-Xdock:icon=path_to_icon_file"])
        assertNull(optionsMap["-XX:AllocateHeapAt=path"])
    }


    @Test
    fun smartMergeTest1() {

        val myJavaToolOptions =
            "-javaagent:/my/path/my.jar=a=b -agentpath:pathname -agentlib:libname=a=b --show-version -DmyProp1=myValue1 -DmyProp2=myValue2 -DmyProp3 -DmyProp4 -Xmx10g -splash:imagepath"
        val otherJavaToolOptions =
            "-javaagent:/my/path/my.jar -agentpath:pathname=o=p -agentlib:libname --show-version -DmyProp1=otherValue1 -DmyProp2=myValue2 -DmyProp3=myValue3 -DmyProp4= -Xmx10g -Xdock:icon=path_to_icon_file -XX:AllocateHeapAt=path"

        val result = smartMergeJavaToolOptions(myJavaToolOptions, otherJavaToolOptions)

        assertEquals(
            result,
            "-javaagent:/my/path/my.jar=a=b -agentpath:pathname -agentlib:libname=a=b --show-version -DmyProp1=myValue1 -DmyProp2=myValue2 -DmyProp3 -DmyProp4 -Xmx10g -Xdock:icon=path_to_icon_file -XX:AllocateHeapAt=path -splash:imagepath"
        )

    }


}