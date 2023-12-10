package org.digma.intellij.plugin.idea.psi.kotlin

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.lang.Language
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.Retries
import org.digma.intellij.plugin.common.allowSlowOperation
import org.digma.intellij.plugin.editor.EditorUtils
import org.digma.intellij.plugin.idea.psi.createMethodCodeObjectId
import org.digma.intellij.plugin.idea.psi.java.JavaEndpointNavigationProvider
import org.digma.intellij.plugin.idea.psi.java.JavaSpanNavigationProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.ui.CaretContextService
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.UClass
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.toUElementOfType

class KotlinLanguageService(private val project: Project) : LanguageService {

    private val logger: Logger = Logger.getInstance(KotlinLanguageService::class.java)

    private val projectFileIndex: ProjectFileIndex = project.getService(ProjectFileIndex::class.java)


    private val codeObjectDiscovery = KotlinCodeObjectDiscovery()


    override fun ensureStartupOnEDT(project: Project) {
        //nothing to do
    }

    override fun runWhenSmart(task: Runnable) {
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(task)
        } else {
            task.run()
        }
    }


    fun getLanguage(): com.intellij.lang.Language {
        return KotlinLanguage.INSTANCE
    }

    override fun getLanguageForMethodCodeObjectId(methodId: String): Language? {


        //try to parse the methodId as if it is java and try to find the language
        if (methodId.indexOf("\$_$") <= 0) {
            Log.log(logger::debug, "method id in getLanguageForMethodCodeObjectId does not contain \$_$ {}", methodId)
            return null
        }

        var className = methodId.substring(0, methodId.indexOf("\$_$"))

        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        className = className.replace('$', '.')

        return ReadAction.compute<Language?, java.lang.RuntimeException> {

            val psiClasses: Collection<KtClassOrObject> = KotlinFullClassNameIndex.get(className, project, GlobalSearchScope.allScope(project))

            if (!psiClasses.isEmpty()) {
                val psiClass = psiClasses.stream().findAny()
                if (psiClass.isPresent) {
                    return@compute psiClass.get().language
                }
            }
            null
        }
    }

    override fun isSupportedFile(project: Project, newFile: VirtualFile): Boolean {
        val psiFile = PsiManager.getInstance(project).findFile(newFile)
        return psiFile != null && isSupportedFile(project, psiFile)
    }

    override fun isSupportedFile(project: Project, psiFile: PsiFile): Boolean {
        return psiFile.viewProvider.baseLanguage == KotlinLanguage.INSTANCE
    }

    override fun detectMethodUnderCaret(project: Project, psiFile: PsiFile, selectedEditor: Editor?, caretOffset: Int): MethodUnderCaret {

        return Retries.retryWithResult<MethodUnderCaret>({

            ReadAction.compute<MethodUnderCaret, java.lang.RuntimeException> {

                allowSlowOperation<MethodUnderCaret> {

                    val fileUri = PsiUtils.psiFileToUri(psiFile)
                    if (!isSupportedFile(project, psiFile)) {
                        return@allowSlowOperation MethodUnderCaret("", "", "", "", fileUri, false)
                    }
                    return@allowSlowOperation org.digma.intellij.plugin.idea.psi.detectMethodUnderCaret(psiFile, fileUri, caretOffset)
                }
            }
        }, Throwable::class.java, 50, 5)
    }

    override fun navigateToMethod(methodId: String) {

        Log.log(logger::debug, "got navigate to method request {}", methodId);

        if (methodId.indexOf("\$_$") <= 0) {
            Log.log(logger::debug, "method id in navigateToMethod does not contain \$_$, can not navigate {}", methodId);

            return
        }

        ReadActions.ensureReadAction {
            allowSlowOperation {
                var className = methodId.substring(0, methodId.indexOf("\$_$"))
                //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
                className = className.replace('$', '.')
                //searching in project scope will find only project classes
                val psiClasses: Collection<KtClassOrObject> = KotlinFullClassNameIndex.get(className, project, GlobalSearchScope.allScope(project))
                if (!psiClasses.isEmpty()) {
                    //hopefully there is only one class by that name in the project
                    val psiClassOptional = psiClasses.stream().findAny()
                    val psiClass: KtClassOrObject = psiClassOptional.get()

                    val methods = psiClass.toUElementOfType<UClass>()?.methods ?: arrayOf()

                    for (method in methods) {
                        val id: String = createMethodCodeObjectId(method)
                        if (id == methodId && method.sourcePsi is Navigatable && (method.sourcePsi as Navigatable).canNavigate()) {
                            Log.log(logger::debug, "navigating to method {}", method);
                            EDT.ensureEDT {
                                (method.sourcePsi as Navigatable).navigate(true)
                            }
                            return@allowSlowOperation
                        }
                    }
                }
            }
        }
    }

    override fun isServiceFor(language: Language): Boolean {
        return KotlinLanguage::class.java == language.javaClass
    }

    override fun findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(methodCodeObjectIds: List<String>): MutableMap<String, String> {


        val workspaceUrls: MutableMap<String, String> = HashMap()

        methodCodeObjectIds.filter { s: String -> s.contains("\$_$") }.forEach { methodId ->

            var className = methodId.substring(0, methodId.indexOf("\$_$"))
            //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
            className = className.replace('$', '.')

            ReadActions.ensureReadAction {

                val psiClasses: Collection<KtClassOrObject> = KotlinFullClassNameIndex.get(className, project, GlobalSearchScope.allScope(project))

                if (!psiClasses.isEmpty()) {
                    val psiClass = psiClasses.stream().findAny()
                    if (psiClass.isPresent) {
                        val psiFile = PsiTreeUtil.getParentOfType(psiClass.get(), PsiFile::class.java)
                        psiFile?.let {
                            val url = PsiUtils.psiFileToUri(psiFile)
                            workspaceUrls.put(methodId, url)
                        }
                    }
                }
            }
        }

        return workspaceUrls
    }

    override fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: List<String>): Map<String, Pair<String, Int>> {

        val workspaceUrls = mutableMapOf<String, Pair<String, Int>>()

        methodCodeObjectIds.forEach { methodId ->
            ReadActions.ensureReadAction {
                allowSlowOperation {
                    var className = methodId.substring(0, methodId.indexOf("\$_$"))
                    //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
                    className = className.replace('$', '.')
                    //searching in project scope will find only project classes
                    val psiClasses: Collection<KtClassOrObject> =
                        KotlinFullClassNameIndex.get(className, project, GlobalSearchScope.projectScope(project))
                    if (!psiClasses.isEmpty()) {
                        //hopefully there is only one class by that name in the project
                        val psiClassOptional = psiClasses.stream().findAny()
                        val psiClass: KtClassOrObject = psiClassOptional.get()

                        val uClass = psiClass.toUElementOfType<UClass>()

                        uClass?.let { cls ->
                            val method = cls.methods.firstOrNull { methodId == createMethodCodeObjectId(it) }
                            method?.let { uMethod ->
                                val uFile = cls.getContainingUFile()
                                uFile?.let {
                                    val url = PsiUtils.psiFileToUri(it.sourcePsi)
                                    val offset = uMethod.sourcePsi?.textOffset ?: 0
                                    workspaceUrls.put(methodId, Pair(url, offset))
                                }
                            }
                        }
                    }
                }
            }
        }
        return workspaceUrls
    }


    override fun findWorkspaceUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>> {
        return JavaSpanNavigationProvider.getInstance(project).getUrisForSpanIds(spanIds)
    }

    override fun lookForDiscoveredEndpoints(endpointId: String?): Set<EndpointInfo> {
        return JavaEndpointNavigationProvider.getInstance(project).getEndpointInfos(endpointId)
    }

    override fun environmentChanged(newEnv: String?, refreshInsightsView: Boolean) {
        if (refreshInsightsView) {
            EDT.ensureEDT {
                val fileEditor = FileEditorManager.getInstance(project).selectedEditor
                if (fileEditor != null) {
                    val file = fileEditor.file
                    val psiFile = PsiManager.getInstance(project).findFile(file)
                    if (psiFile != null && isRelevant(psiFile.virtualFile)) {
                        val selectedTextEditor =
                            EditorUtils.getSelectedTextEditorForFile(file, FileEditorManager.getInstance(project))
                        if (selectedTextEditor != null) {
                            val offset = selectedTextEditor.caretModel.offset
                            val methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedTextEditor, offset)
                            CaretContextService.getInstance(project).contextChanged(methodUnderCaret)
                        }
                    }
                }
            }
        }

        //todo: kotlin
//        JavaCodeLensService.getInstance(project).environmentChanged(newEnv)
    }

    override fun buildDocumentInfo(psiFile: PsiFile): DocumentInfo {
        Log.log(logger::debug, "got buildDocumentInfo request for {}", psiFile)
        if (psiFile is KtFile) {
            return ProgressManager.getInstance().runProcess<DocumentInfo>({
                Retries.retryWithResult<DocumentInfo>(
                    {
                        ReadAction.compute(
                            ThrowableComputable<DocumentInfo, RuntimeException> {
                                codeObjectDiscovery.buildDocumentInfo(
                                    project,
                                    psiFile
                                )
                            })
                    },
                    Throwable::class.java, 50, 5
                )
            }, EmptyProgressIndicator())

        } else {
            Log.log(logger::debug, "psi file is not kotlin, returning empty DocumentInfo for {}", psiFile);
            return DocumentInfo(PsiUtils.psiFileToUri(psiFile), mutableMapOf())
        }
    }

    override fun buildDocumentInfo(psiFile: PsiFile, selectedTextEditor: FileEditor?): DocumentInfo {
        return buildDocumentInfo(psiFile)
    }

    override fun isRelevant(file: VirtualFile): Boolean {
        return allowSlowOperation<Boolean> {
            if (file.isDirectory || !file.isValid) {
                return@allowSlowOperation false
            }
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@allowSlowOperation false
            isRelevant(psiFile)
        }
    }

    override fun isRelevant(psiFile: PsiFile): Boolean {

        return allowSlowOperation<Boolean> {

            val isRelevant = psiFile.isWritable &&
                    projectFileIndex.isInSourceContent(psiFile.virtualFile) &&
                    !projectFileIndex.isInLibrary(psiFile.virtualFile) &&
                    !projectFileIndex.isExcluded(psiFile.virtualFile) &&
                    isSupportedFile(project, psiFile)

            return@allowSlowOperation isRelevant && !(psiFile as KtFile).isScript()
        }
    }


    override fun refreshMethodUnderCaret(project: Project, psiFile: PsiFile, selectedEditor: Editor?, offset: Int) {
        //todo
    }

    override fun isCodeVisionSupported(): Boolean {
        return true
    }

    override fun getCodeLens(psiFile: PsiFile): MutableList<Pair<TextRange, CodeVisionEntry>> {
        return mutableListOf()
    }
}