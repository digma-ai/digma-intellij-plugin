package org.digma.intellij.plugin.common

import kotlin.test.Test
import kotlin.test.assertEquals

class FileUtilsTest {

    @Test
    fun convertWinToWslPath() {
        assertEquals("/mnt/c/Users/Default/file.txt", FileUtils.convertWinToWslPath("C:\\Users\\Default\\file.txt"))
        assertEquals("/mnt/d/file.txt", FileUtils.convertWinToWslPath("D:\\file.txt"))

        // Do nothing when cannot convert
        assertEquals("ftp:\\file.txt", FileUtils.convertWinToWslPath("ftp:\\file.txt"))
    }
}