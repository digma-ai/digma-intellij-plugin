package org.digma.intellij.plugin.common

import com.intellij.openapi.vfs.VirtualFile


fun isValidVirtualFile(file: VirtualFile?): Boolean {
    return file?.let {
        it.isValid && !it.isDirectory
    } ?: false
}