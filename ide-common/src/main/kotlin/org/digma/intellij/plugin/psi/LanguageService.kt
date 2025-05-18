package org.digma.intellij.plugin.psi

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret

interface LanguageService : OldLanguageService {


    companion object {

        //it could be useful and fast to use this list but i don't want to rely on hard coded naming
//        private val targetLanguages = listOfNotNull(
//            Language.findLanguageByID("C#"),
//            Language.findLanguageByID("JAVA"),
//            Language.findLanguageByID("kotlin"),
//            Language.findLanguageByID("Python")
//        )

        //needs to be a fast method. we could just check the file extension, but it's not as reliable for the long term as checking the language.
        //if this method becomes a bottleneck, we can change it to check the file extension strings.
        //check if the file is of any language supported by the plugin.
        @JvmStatic
        fun isSupportedLanguageFile(project: Project, virtualFile: VirtualFile): Boolean {
            val fileType = virtualFile.fileType
            if (fileType !is LanguageFileType) return false

            val supportedFileTypes = LanguageServiceProvider.getInstance(project).getFileTypes()
            return supportedFileTypes.any { it.name == fileType.name }

//            val language = fileType.language
//            val targetLanguages = LanguageServiceProvider.getInstance(project).getLanguages()
//            return targetLanguages.any { language.isKindOf(it) }
        }
    }


    fun getLanguage(): Language
    fun getFileType(): FileType

    fun isSupportedFile(virtualFile: VirtualFile): Boolean{
        val languageFileType = virtualFile.fileType as? LanguageFileType ?: return false
        return languageFileType.language.isKindOf(getLanguage())
    }

    //some language services need the editor. for example, CSharpLanguageService needs to take
    // getProjectModelId from the selected editor, which is the preferred way to find the IPsiSourceFile in resharper.
    // it's not possible to invoke getProjectModelId in regular intellij code because it's an extension method available only in Rider.
    suspend fun detectMethodUnderCaret(virtualFile: VirtualFile, editor: Editor, caretOffset: Int): MethodUnderCaret

    suspend fun buildDocumentInfo(virtualFile: VirtualFile): DocumentInfo?


}