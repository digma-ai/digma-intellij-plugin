package org.digma.intellij.plugin.idea.psi

import com.google.common.collect.ImmutableMap
import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.buildsystem.model.unified.UnifiedDependency
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.lang.Language
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.digma.intellij.plugin.WITH_SPAN_ANNOTATION_FQN
import org.digma.intellij.plugin.WITH_SPAN_DEPENDENCY_DESCRIPTION
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.Retries
import org.digma.intellij.plugin.common.allowSlowOperation
import org.digma.intellij.plugin.common.runWIthRetryWithResult
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.editor.EditorUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.buildsystem.BuildSystemChecker.Companion.determineBuildSystem
import org.digma.intellij.plugin.idea.buildsystem.JavaBuildSystem
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.idea.psi.discovery.AbstractCodeObjectDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.MicrometerTracingFramework
import org.digma.intellij.plugin.idea.psi.navigation.JavaEndpointNavigationProvider
import org.digma.intellij.plugin.idea.psi.navigation.JavaSpanNavigationProvider
import org.digma.intellij.plugin.instrumentation.CanInstrumentMethodResult
import org.digma.intellij.plugin.instrumentation.JvmCanInstrumentMethodResult
import org.digma.intellij.plugin.instrumentation.MissingDependencyCause
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.model.discovery.TextRange
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiFileNotFountException
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.ui.CaretContextService
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElementOfType
import java.util.Collections
import java.util.Objects
import java.util.function.Consumer
import java.util.function.Supplier

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractJvmLanguageService(protected val project: Project, protected val codeObjectDiscovery: AbstractCodeObjectDiscovery) :
    LanguageService, JvmLanguageService {

    protected val logger: Logger = Logger.getInstance(this::class.java)


    private val otelDependencyVersion = "1.26.0"
    private val otelCoordinates =
        UnifiedCoordinates("io.opentelemetry.instrumentation", "opentelemetry-instrumentation-annotations", otelDependencyVersion)
    private val mapBuildSystem2Dependency: ImmutableMap<JavaBuildSystem, UnifiedDependency>

    init {
        val builder: ImmutableMap.Builder<JavaBuildSystem, UnifiedDependency> = ImmutableMap.Builder()
        builder.put(JavaBuildSystem.UNKNOWN, UnifiedDependency(otelCoordinates, "compile"))
        builder.put(JavaBuildSystem.MAVEN, UnifiedDependency(otelCoordinates, null))
        builder.put(JavaBuildSystem.GRADLE, UnifiedDependency(otelCoordinates, "implementation"))
        mapBuildSystem2Dependency = builder.build()
    }


    companion object {
        val fileNamesToExclude = mutableSetOf("package-info.java", "MavenWrapperDownloader.java")
    }


    //It's a different search for each jvm language.
    abstract fun findClassByClassName(className: String, scope: GlobalSearchScope): UClass?

    abstract fun refreshCodeLens()

    abstract fun findParentMethod(psiElement: PsiElement): UMethod?

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



    override fun buildDocumentInfo(psiFile: PsiFile, selectedTextEditor: FileEditor?): DocumentInfo {
        EDT.assertNonDispatchThread()
        return buildDocumentInfo(psiFile)
    }

    override fun buildDocumentInfo(psiFile: PsiFile): DocumentInfo {

        EDT.assertNonDispatchThread()

        Log.log(logger::debug, "got buildDocumentInfo request for {}", psiFile)
        if (!project.isDisposed && PsiUtils.isValidPsiFile(psiFile) && isSupportedFile(psiFile)) {
            try {
                //retry the whole operation.
                return runWIthRetryWithResult({
                    codeObjectDiscovery.buildDocumentInfo(
                        project,
                        psiFile
                    )
                }, backOffMillis = 100, maxRetries = 3)

            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.buildDocumentInfo", e)
                return DocumentInfo(PsiUtils.psiFileToUri(psiFile), mutableMapOf())
            }
        } else {
            Log.log(logger::debug, "psi file is not supported or not valid, returning empty DocumentInfo for {}", psiFile)
            return DocumentInfo(PsiUtils.psiFileToUri(psiFile), mutableMapOf())
        }
    }


    override fun isSupportedFile(project: Project, newFile: VirtualFile): Boolean {
        return ReadActions.ensureReadAction(Supplier {
            val psiFile = PsiManager.getInstance(project).findFile(newFile)
            PsiUtils.isValidPsiFile(psiFile) && isSupportedFile(psiFile)
        })
    }


    override fun environmentChanged(newEnv: String, refreshInsightsView: Boolean) {
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

        refreshCodeLens()
    }


    override fun isRelevant(psiFile: PsiFile): Boolean {

        val projectFileIndex: ProjectFileIndex = project.getService(ProjectFileIndex::class.java)

        return allowSlowOperation<Boolean> {
            runInReadAccessWithResult {
                val isRelevant = psiFile.isWritable &&
                        projectFileIndex.isInSourceContent(psiFile.virtualFile) &&
                        !projectFileIndex.isInLibrary(psiFile.virtualFile) &&
                        !projectFileIndex.isExcluded(psiFile.virtualFile) &&
                        isSupportedFile(psiFile) &&
                        !fileNamesToExclude.contains(psiFile.virtualFile.name)

                return@runInReadAccessWithResult isRelevant
            }
        }
    }


    override fun isRelevant(file: VirtualFile): Boolean {
        return allowSlowOperation<Boolean> {
            runInReadAccessWithResult {
                if (file.isDirectory || !file.isValid) {
                    return@runInReadAccessWithResult false
                }
                val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@runInReadAccessWithResult false
                isRelevant(psiFile)
            }
        }
    }

    override fun isCodeVisionSupported(): Boolean {
        return true
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


            val uClass = findClassByClassName(className, GlobalSearchScope.allScope(project))

            return@compute uClass?.let {
                it.sourcePsi?.language
            }
        }
    }

    override fun getLanguageForClass(className: String): Language? {

        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        val classNameToFind = className.replace('$', '.')

        return ReadAction.compute<Language?, java.lang.RuntimeException> {

            val uClass = findClassByClassName(classNameToFind, GlobalSearchScope.allScope(project))

            return@compute uClass?.let {
                it.sourcePsi?.language
            }
        }
    }


    override fun findWorkspaceUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>> {
        return JavaSpanNavigationProvider.getInstance(project).getUrisForSpanIds(spanIds)
    }

    override fun lookForDiscoveredEndpoints(endpointId: String): Set<EndpointInfo> {
        return JavaEndpointNavigationProvider.getInstance(project).getEndpointInfos(endpointId)
    }


    override fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: List<String>): Map<String, Pair<String, Int>> {
        val workspaceUrls: MutableMap<String, Pair<String, Int>> = HashMap()

        methodCodeObjectIds.forEach(Consumer { methodId: String ->
            ReadActions.ensureReadAction {
                allowSlowOperation {
                    val uMethod = findMethodByMethodCodeObjectId(methodId)
                    uMethod?.getContainingUFile()?.sourcePsi?.let { psiFile ->
                        val url = PsiUtils.psiFileToUri(psiFile)
                        workspaceUrls[methodId] = Pair(url, uMethod.sourcePsi?.textOffset ?: 0)
                    }
                }
            }
        })

        return workspaceUrls
    }


    override fun detectMethodBySpan(project: Project, spanCodeObjectId: String?): String? {

        val urisForSpanIds: Map<String, Pair<String, Int>> =
            JavaSpanNavigationProvider.getInstance(project).getUrisForSpanIds(Collections.singletonList(spanCodeObjectId))

        val pair = urisForSpanIds[spanCodeObjectId]
        return pair?.let {
            val fileUri: String = it.first
            val offset: Int = it.second
            try {

                val psiFile = PsiUtils.uriToPsiFile(fileUri, project)
                val psiElement = psiFile.findElementAt(offset)
                psiElement?.let { element ->
                    val psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
                    psiMethod?.let { method ->
                        createPsiMethodCodeObjectId(method)
                    }
                }

            } catch (e: PsiFileNotFountException) {
                null
            }
        }
    }



    fun findMethodByMethodCodeObjectId(methodId: String?): UMethod? {
        if (methodId == null) return null

        if (!methodId.contains("\$_$")) {
            Log.log(logger::debug, "method id in findWorkspaceUrisForMethodCodeObjectIds does not contain \$_$ {}", methodId)
            return null
        }

        return ReadActions.ensureReadAction<UMethod> {
            if (!methodId.contains("\$_$")) {
                Log.log(logger::debug, "method id in netoFindPsiMethodByMethodCodeObjectId does not contain \$_$ {}", methodId)
                return@ensureReadAction null
            }

            //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
            val className = methodId.substring(0, methodId.indexOf("\$_$")).replace('$', '.')

            return@ensureReadAction allowSlowOperation<UMethod?> {
                val uClass = findClassByClassName(className, GlobalSearchScope.projectScope(project))

                return@allowSlowOperation uClass?.let { cls ->
                    findMethodInClass(project, cls, methodId)
                }
            }
        }
    }


    override fun findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(methodCodeObjectIds: List<String>): Map<String, String> {

        val workspaceUrls: MutableMap<String, String> = HashMap()

        methodCodeObjectIds.filter { s: String -> s.contains("\$_$") }.forEach { methodId ->

            var className = methodId.substring(0, methodId.indexOf("\$_$"))
            //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
            className = className.replace('$', '.')

            ReadActions.ensureReadAction {

                val uClass = findClassByClassName(className, GlobalSearchScope.projectScope(project))
                uClass?.let {

                    val psiFile = it.getContainingUFile()?.sourcePsi
                    psiFile?.let {
                        val url = PsiUtils.psiFileToUri(psiFile)
                        workspaceUrls[methodId] = url
                    }

                }
            }
        }

        return workspaceUrls
    }


    override fun detectMethodUnderCaret(project: Project, psiFile: PsiFile, selectedEditor: Editor?, caretOffset: Int): MethodUnderCaret {

        return Retries.retryWithResult({

            ReadAction.compute<MethodUnderCaret, java.lang.RuntimeException> {

                allowSlowOperation<MethodUnderCaret> {

                    val fileUri = PsiUtils.psiFileToUri(psiFile)
                    if (!isSupportedFile(psiFile)) {
                        return@allowSlowOperation MethodUnderCaret("", "", "", "", fileUri, caretOffset, null, false)
                    }
                    return@allowSlowOperation detectMethodUnderCaret(psiFile, fileUri, caretOffset)
                }
            }
        }, Throwable::class.java, 50, 5)
    }


    private fun detectMethodUnderCaret(psiFile: PsiFile, fileUri: String, caretOffset: Int): MethodUnderCaret {

        val packageName = psiFile.toUElementOfType<UFile>()?.packageName ?: ""
        val underCaret: PsiElement =
            psiFile.findElementAt(caretOffset) ?: return MethodUnderCaret("", "", "", packageName, fileUri, caretOffset)
        val uMethod = findParentMethod(underCaret)
        val className: String = uMethod?.getParentOfType<UClass>()?.let {
            getClassSimpleName(it)
        } ?: ""

        if (uMethod != null) {

            val methodId = createMethodCodeObjectId(uMethod)
            val endpointTextRange = findEndpointTextRange(fileUri, caretOffset, methodId)

            return MethodUnderCaret(
                methodId,
                uMethod.name,
                className,
                packageName,
                fileUri,
                caretOffset,
                endpointTextRange
            )
        }
        return MethodUnderCaret("", "", className, packageName, fileUri, caretOffset)
    }


    fun findEndpointTextRange(fileUri: String, caretOffset: Int, methodId: String): TextRange? {
        val documentInfo = DocumentInfoService.getInstance(project).getDocumentInfo(fileUri)
        if (documentInfo != null) {
            val methodInfo = documentInfo.getMethodInfo(methodId)
            if (methodInfo != null) {
                val endpointInfo = methodInfo.endpoints.firstOrNull { endpointInfo: EndpointInfo ->
                    endpointInfo.textRange?.contains(caretOffset) ?: false
                }

                if (endpointInfo != null) {
                    return endpointInfo.textRange
                }
            }
        }
        return null
    }






    override fun navigateToMethod(methodId: String) {

        Log.log(logger::debug, "got navigate to method request {}", methodId)

        if (methodId.indexOf("\$_$") <= 0) {
            Log.log(logger::debug, "method id in navigateToMethod does not contain \$_$, can not navigate {}", methodId)
            return
        }

        ReadActions.ensureReadAction {
            allowSlowOperation {
                try {
                    var className = methodId.substring(0, methodId.indexOf("\$_$"))
                    //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
                    className = className.replace('$', '.')

                    val cls = findClassByClassName(className, GlobalSearchScope.allScope(project))
                    cls?.let {

                        val method = findMethodInClass(project, it, methodId)
                        if (method?.sourcePsi is Navigatable && (method.sourcePsi as Navigatable).canNavigate()) {
                            Log.log(logger::debug, "navigating to method {}", method)
                            EDT.ensureEDT {
                                (method.sourcePsi as Navigatable).navigate(true)
                            }
                            return@allowSlowOperation
                        }
                    }
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError("AbstractJvmLanguageService.navigateToMethod", e)
                }
            }
        }
    }


    override fun getPsiElementForMethod(methodId: String): PsiElement? {

        Log.log(logger::debug, "got getPsiElementForMethod request {}", methodId)

        if (methodId.indexOf("\$_$") <= 0) {
            Log.log(logger::debug, "method id in getPsiElementForMethod does not contain \$_$, can not navigate {}", methodId)
            return null
        }


        return ReadActions.ensureReadAction(Supplier {

            return@Supplier allowSlowOperation<PsiElement?> {

                return@allowSlowOperation try {

                    val methodAndClass: Pair<String, String> = CodeObjectsUtil.getMethodClassAndName(methodId)
                    //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
                    val className = methodAndClass.first.replace('$', '.')
                    val cls = findClassByClassName(className, GlobalSearchScope.allScope(project))
                    cls?.let {
                        val method = findMethodInClass(project, it, methodId)
                        method?.sourcePsi
                    }
                } catch (e: Exception) {
                    ErrorReporter.getInstance().reportError("AbstractJvmLanguageService.getPsiElementForMethod", e)
                    null
                }
            }
        })
    }

    override fun getPsiElementForClassByMethodId(methodId: String): PsiElement? {

        Log.log(logger::debug, "got getPsiElementForClassByMethodId request {}", methodId)

        if (methodId.indexOf("\$_$") <= 0) {
            Log.log(logger::debug, "method id in getPsiElementForClassByMethodId does not contain \$_$, can not navigate {}", methodId)
            return null
        }

        val methodAndClass: Pair<String, String> = CodeObjectsUtil.getMethodClassAndName(methodId)
        return getPsiElementForClassByName(methodAndClass.first)
    }


    override fun getPsiElementForClassByName(className: String): PsiElement? {

        Log.log(logger::debug, "got getPsiElementForClassByName request {}", className)

        return ReadActions.ensureReadAction(Supplier {

            return@Supplier allowSlowOperation<PsiElement?> {

                return@allowSlowOperation try {

                    //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
                    val classNameToFind = className.replace('$', '.')
                    val uClass = findClassByClassName(classNameToFind, GlobalSearchScope.allScope(project))
                    return@allowSlowOperation uClass?.sourcePsi
                } catch (e: Exception) {
                    ErrorReporter.getInstance().reportError("AbstractJvmLanguageService.getPsiElementForClassByName", e)
                    null
                }
            }
        })
    }


    override fun refreshMethodUnderCaret(project: Project, psiFile: PsiFile, selectedEditor: Editor?, offset: Int) {
        val methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedEditor, offset)
        CaretContextService.getInstance(project).contextChanged(methodUnderCaret)
    }


    override fun canInstrumentMethod(project: Project, methodId: String?): CanInstrumentMethodResult {

        if (methodId == null) {
            return CanInstrumentMethodResult.failure()
        }

        class MyRunnable : Runnable {
            val progressIndicator: ProgressIndicator = EmptyProgressIndicator()

            var result: CanInstrumentMethodResult? = null

            override fun run() {
                try {
                    val uMethod = findMethodByMethodCodeObjectId(methodId)
                    if (uMethod?.sourcePsi == null) {
                        Log.log(logger::warn, "Failed to get Method from method id '{}'", methodId)
                        result = CanInstrumentMethodResult.failure()
                        return
                    }

                    progressIndicator.checkCanceled()

                    val psiFile = uMethod.getContainingUFile()
                    if (psiFile == null || !isSupportedFile(psiFile.sourcePsi)) {
                        Log.log(logger::warn, "Method's file is not supported file (methodId: {})", methodId)
                        result = CanInstrumentMethodResult.failure()
                        return
                    }

                    progressIndicator.checkCanceled()

                    val module = ModuleUtilCore.findModuleForPsiElement(uMethod.sourcePsi!!)
                    if (module == null) {
                        Log.log(logger::warn, "Failed to get module from PsiMethod '{}'", methodId)
                        result = CanInstrumentMethodResult.failure()
                        return
                    }

                    progressIndicator.checkCanceled()

                    var annotationClassFqn = WITH_SPAN_ANNOTATION_FQN
                    var dependencyCause = WITH_SPAN_DEPENDENCY_DESCRIPTION
                    if (isSpringBootAndMicrometer(module)) {
                        progressIndicator.checkCanceled()

                        annotationClassFqn = MicrometerTracingFramework.OBSERVED_FQN
                        dependencyCause = MicrometerTracingFramework.OBSERVED_DEPENDENCY_DESCRIPTION

                        val modulesDepsService = ModulesDepsService.getInstance(project)
                        val moduleExt = modulesDepsService.getModuleExt(module.name)
                        if (moduleExt == null) {
                            Log.log(logger::warn, "Failed to not lookup module ext by module name='{}'", module.name)
                            result = CanInstrumentMethodResult.failure()
                            return
                        }
                        val hasDeps = modulesDepsService.isModuleHasNeededDependenciesForSpringBootWithMicrometer(moduleExt.metadata)
                        if (!hasDeps) {
                            result = CanInstrumentMethodResult(MissingDependencyCause(dependencyCause))
                            return
                        }
                    }

                    progressIndicator.checkCanceled()

                    val annotationPsiClass = JavaPsiFacade.getInstance(project).findClass(
                        annotationClassFqn,
                        GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
                    )
                    if (annotationPsiClass == null) {
                        Log.log(logger::warn, "Cannot find WithSpan PsiClass (methodId: {}) (module:{})", methodId, module)
                        result = CanInstrumentMethodResult(MissingDependencyCause(dependencyCause))
                        return
                    }

                    result = JvmCanInstrumentMethodResult(methodId, uMethod, annotationPsiClass, psiFile)
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError("AbstractJvmLanguageService.canInstrumentMethod", e)
                }
            }
        }


        return Retries.retryWithResultAndDefault({
            val myRunnable = MyRunnable()
            runInReadAccessInSmartMode(project, myRunnable, myRunnable.progressIndicator)
            Objects.requireNonNullElseGet(myRunnable.result) { CanInstrumentMethodResult.failure() }
        }, Throwable::class.java, 50, 5, CanInstrumentMethodResult.failure())
    }


    protected fun isSpringBootAndMicrometer(module: Module): Boolean {
        val modulesDepsService = ModulesDepsService.getInstance(project)

        return (modulesDepsService.isSpringBootModule(module)
                && SpringBootMicrometerConfigureDepsService.isSpringBootWithMicrometer())
    }


    @Suppress("UnstableApiUsage")
    override fun addDependencyToOtelLib(project: Project, methodId: String) {
        val module = getModuleOfMethodId(methodId)
        if (module == null) {
            Log.log(logger::warn, "Failed to add dependencies OTEL lib since could not lookup module by methodId='{}'", methodId)
            return
        }
        if (!isSpringBootAndMicrometer(module)) {
            val moduleBuildSystem = determineBuildSystem(module)
            val dependencyLib = mapBuildSystem2Dependency[moduleBuildSystem]

            val dependencyModifierService = DependencyModifierService.getInstance(project)

            if (dependencyLib != null) {
                dependencyModifierService.addDependency(module, dependencyLib)
            }
            return
        }

        // handling spring boot with micrometer tracing
        addDepsForSpringBootAndMicrometer(module)
    }


    private fun getModuleOfMethodId(methodCodeObjectId: String): Module? {
        val uMethod = findMethodByMethodCodeObjectId(methodCodeObjectId)
        if (uMethod?.sourcePsi == null) {
            Log.log(logger::warn, "Failed to get PsiMethod from method id '{}'", methodCodeObjectId)
            return null
        }

        val module = ModuleUtilCore.findModuleForPsiElement(uMethod.sourcePsi!!)
        if (module == null) {
            Log.log(logger::warn, "Failed to get module from PsiMethod '{}'", methodCodeObjectId)
            return null
        }

        return module
    }


    private fun addDepsForSpringBootAndMicrometer(module: Module) {
        val modulesDepsService = ModulesDepsService.getInstance(project)
        val moduleExt = modulesDepsService.getModuleExt(module.name)
        if (moduleExt == null) {
            Log.log(
                logger::warn,
                "Failed add dependencies of Spring Boot Micrometer since could not lookup module ext by module name='{}'",
                module.name
            )
            return
        }
        val project = module.project
        val springBootMicrometerConfigureDepsService = SpringBootMicrometerConfigureDepsService.getInstance(project)
        springBootMicrometerConfigureDepsService.addMissingDependenciesForSpringBootObservability(moduleExt)
    }
}