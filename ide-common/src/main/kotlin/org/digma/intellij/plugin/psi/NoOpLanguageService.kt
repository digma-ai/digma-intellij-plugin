package org.digma.intellij.plugin.psi

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret

//todo: see how to remove this class and support null where necessary
class NoOpLanguageService : OldNoOpLanguageService() {

    override fun getLanguage(): Language {
        return Language.ANY
    }

    override fun getFileType(): FileType {
        return object : FileType {
            override fun getName()=  "UNKNOWN"
            override fun getDescription()=  "UNKNOWN"
            override fun getDefaultExtension()=  "UNKNOWN"
            override fun getIcon()=  null
            override fun isBinary() = false
        }
    }

    override suspend fun detectMethodUnderCaret(
        virtualFile: VirtualFile,
        editor: Editor,
        caretOffset: Int
    ): MethodUnderCaret {
        return MethodUnderCaret.EMPTY
    }

    override suspend fun buildDocumentInfo(
        virtualFile: VirtualFile
    ): DocumentInfo? {
        throw UnsupportedOperationException("should not be called")
    }
}