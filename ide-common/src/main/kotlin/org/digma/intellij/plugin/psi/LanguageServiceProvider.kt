package org.digma.intellij.plugin.psi

import com.intellij.lang.Language
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class LanguageServiceProvider(private val project: Project) {

    private val languageServices = mutableSetOf<LanguageService>()
    private val supportedLanguages = mutableSetOf<Language>()
    private val supportedFileTypes = mutableSetOf<FileType>()
    private val languageServiceByLanguageId = mutableMapOf<String, LanguageService>()


    init {
        //Load the available language services in the current project/IDE, usually not all will be available.
        //In Idea only java and kotlin, and python if the python plugin is installed.
        //In rider only c#, and in pycharm only python.
        SupportedLanguages.entries.forEach { value ->
            try {
                @Suppress("UNCHECKED_CAST")
                val clazz = Class.forName(value.languageServiceClassName) as Class<out LanguageService>

                @Suppress("IncorrectServiceRetrieving")
                val languageService: LanguageService? = project.getService(clazz)
                languageService?.let {
                    languageServices.add(it)
                }
            } catch (_: Throwable) {
                //ignore: some classes will fail to load. for example, the CSharpLanguageService
                // will fail to load if it's not rider because it depends on rider classes.
                //don't log
            }
        }

        supportedLanguages.addAll(languageServices.map { it.getLanguage() })
        supportedFileTypes.addAll(languageServices.map { it.getFileType() })
        languageServiceByLanguageId.putAll(languageServices.associateBy { it.getLanguage().id })

    }


    companion object {
        @JvmStatic
        fun getInstance(project: Project): LanguageServiceProvider = project.service<LanguageServiceProvider>()
    }


    fun getLanguageServices(): Set<LanguageService> {
        return languageServices
    }

    fun getLanguages(): Set<Language> {
        return supportedLanguages
    }

    fun getFileTypes(): Set<FileType> {
        return supportedFileTypes
    }

    fun getLanguageService(language: Language): LanguageService? {
        return languageServiceByLanguageId[language.id]
    }

    fun getLanguageService(languageId: String): LanguageService? {
        return languageServiceByLanguageId[languageId]
    }

    fun getLanguageService(virtualFile: VirtualFile): LanguageService? {
        val languageFileType = virtualFile.fileType as? LanguageFileType ?: return null
        return languageServiceByLanguageId[languageFileType.language.id]
    }
}