package org.digma.intellij.plugin.discovery.model

import com.intellij.openapi.vfs.VirtualFile

class SpanLocation(val file: VirtualFile, val offset: Int, val methodCodeObjectId: String) {
    fun isAlive(): Boolean = file.isValid

    override fun toString(): String {
        return "SpanLocation(file=$file, offset=$offset, methodCodeObjectId='$methodCodeObjectId')"
    }


}