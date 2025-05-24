package org.digma.intellij.plugin.idea.psi

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.buildsystem.model.unified.UnifiedDependency
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.lang.Language
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.coroutines.ensureActive
import org.digma.intellij.plugin.WITH_SPAN_ANNOTATION_FQN
import org.digma.intellij.plugin.WITH_SPAN_DEPENDENCY_DESCRIPTION
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.common.CodeObjectsUtil
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.Retries
import org.digma.intellij.plugin.common.allowSlowOperation
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.common.runInReadAccess
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.common.suspendableRetry
import org.digma.intellij.plugin.document.DocumentInfoStorage
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.buildsystem.JvmBuildSystemHelperService
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.idea.navigation.JvmEndpointNavigationProvider
import org.digma.intellij.plugin.idea.navigation.JvmSpanNavigationProvider
import org.digma.intellij.plugin.idea.psi.discovery.AbstractCodeObjectDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.MicrometerTracingFramework
import org.digma.intellij.plugin.instrumentation.MethodObservabilityInfo
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.model.discovery.TextRange
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiFileNotFountException
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElementOfType
import java.util.function.Consumer
import kotlin.coroutines.coroutineContext

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractJvmLanguageService(protected val project: Project, protected val codeObjectDiscovery: AbstractCodeObjectDiscovery) :
    LanguageService, JvmLanguageService {

    protected val logger: Logger = Logger.getInstance(this::class.java)


    private val otelDependencyVersion = "1.26.0"
    private val otelCoordinates =
        UnifiedCoordinates("io.opentelemetry.instrumentation", "opentelemetry-instrumentation-annotations", otelDependencyVersion)
    private val mapBuildSystem2Dependency: Map<BuildSystem, UnifiedDependency> = mapOf(
        BuildSystem.INTELLIJ to UnifiedDependency(otelCoordinates, "compile"),
        BuildSystem.MAVEN to UnifiedDependency(otelCoordinates, null),
        BuildSystem.GRADLE to UnifiedDependency(otelCoordinates, "implementation")
    )


    companion object {
        val fileNamesToExclude = mutableSetOf("package-info.java", "MavenWrapperDownloader.java")
    }


    init {
        Log.log(logger::trace, "Initializing language service $javaClass")
    }

    override fun dispose() {

    }

    //It's a different search for each jvm language.
    abstract fun findClassByClassName(className: String, scope: GlobalSearchScope): UClass?

    abstract fun findParentMethod(psiElement: PsiElement): UMethod?


    override fun isSupportedFile(virtualFile: VirtualFile): Boolean {
        return super.isSupportedFile(virtualFile) && !virtualFile.name.contains("package-info.java")
    }

    /**
     * Builds a DocumentInfo.
     * This method doesn't handle exceptions and may throw any kind of exception
     */
    override suspend fun buildDocumentInfo(virtualFile: VirtualFile): DocumentInfo? {

        /*
            Important notice:
            building document info may be a long operation for large files.
            Holding read access for the whole operation may cause short freezes in the UI if the user is typing.
            Holding cancelable read access for the whole operation may cause many cancellations and eventually may
            fail to build the document info.
            So the strategy here is to hold read access for short periods, only for code that really needs read access,
            that way the chance for freezes or too many cancellations is minimized.
         */

        //don't call this method on EDT or in read access, read access is acquired when needed
        EDT.assertNonDispatchThread()
        ReadActions.assertNotInReadAccess()

        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "got buildDocumentInfo request for {}", virtualFile)
        }

        val psiFile = smartReadAction(project) {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            if (psiFile == null) {
                Log.log(logger::trace, "buildDocumentInfo: could not find psiFile for {}", virtualFile)
                null
            } else if (!PsiUtils.isValidPsiFile(psiFile)) {
                Log.log(logger::trace, "buildDocumentInfo: psiFile is not valid for {}", virtualFile)
                null
            } else if (!isSupportedFile(psiFile)) {
                Log.log(logger::trace, "buildDocumentInfo: psiFile is not supported for {}", virtualFile)
                null
            } else if (!isProjectValid(project)) {
                Log.log(logger::trace, "buildDocumentInfo: project is not valid for {}", virtualFile)
                null
            } else {
                psiFile
            }
        }
        coroutineContext.ensureActive()
        return psiFile?.let {
            suspendableRetry {
                codeObjectDiscovery.buildDocumentInfo(project, it, virtualFile.url, getLanguage())
            }
        }
    }

    override fun isSupportedFile(project: Project, newFile: VirtualFile): Boolean {
        return runInReadAccessWithResult {
            return@runInReadAccessWithResult if (isValidVirtualFile(newFile)) {
                val psiFile = PsiManager.getInstance(project).findFile(newFile)
                psiFile != null && PsiUtils.isValidPsiFile(psiFile) && isSupportedFile(psiFile)
            } else {
                false
            }
        }
    }


    override fun isRelevant(psiFile: PsiFile): Boolean {

        if (!PsiUtils.isValidPsiFile(psiFile)) {
            return false
        }

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

        if (!isValidVirtualFile(file)) {
            return false
        }

        return allowSlowOperation<Boolean> {
            runInReadAccessWithResult {
                if (file.isDirectory || !isValidVirtualFile(file)) {
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

        return runInReadAccessWithResult {

            val uClass = findClassByClassName(className, GlobalSearchScope.allScope(project))

            uClass?.let {
                it.sourcePsi?.language
            }
        }
    }

    override fun getLanguageForClass(className: String): Language? {

        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        val classNameToFind = className.replace('$', '.')

        return runInReadAccessWithResult {

            val uClass = findClassByClassName(classNameToFind, GlobalSearchScope.allScope(project))

            uClass?.let {
                it.sourcePsi?.language
            }
        }
    }


    override fun findWorkspaceUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>> {
        return JvmSpanNavigationProvider.getInstance(project).getUrisForSpanIds(spanIds)
    }

    override fun lookForDiscoveredEndpoints(endpointId: String): Set<EndpointInfo> {
        return JvmEndpointNavigationProvider.getInstance(project).getEndpointInfos(endpointId)
    }


    override fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: List<String>): Map<String, Pair<String, Int>> {
        val workspaceUrls: MutableMap<String, Pair<String, Int>> = HashMap()

        methodCodeObjectIds.forEach(Consumer { methodId: String ->
            runInReadAccess {
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


    override fun detectMethodBySpan(project: Project, spanCodeObjectId: String): String? {

        val urisForSpanIds: Map<String, Pair<String, Int>> =
            JvmSpanNavigationProvider.getInstance(project).getUrisForSpanIds(listOf(spanCodeObjectId))

        val pair = urisForSpanIds[spanCodeObjectId]
        return pair?.let {
            val fileUri: String = it.first
            val offset: Int = it.second
            try {

                val psiFile = PsiUtils.uriToPsiFile(fileUri, project)
                if (PsiUtils.isValidPsiFile(psiFile)) {
                    val psiElement = psiFile.findElementAt(offset)
                    psiElement?.let { element ->
                        val psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
                        psiMethod?.let { method ->
                            createPsiMethodCodeObjectId(method)
                        }
                    }
                } else {
                    null
                }

            } catch (_: PsiFileNotFountException) {
                null
            }
        }
    }


    override fun findMethodByMethodCodeObjectId(methodId: String?): UMethod? {
        if (methodId == null) return null

        if (!methodId.contains("\$_$")) {
            Log.log(logger::debug, "method id in findWorkspaceUrisForMethodCodeObjectIds does not contain \$_$ {}", methodId)
            return null
        }

        return runInReadAccessWithResult {
            if (!methodId.contains("\$_$")) {
                Log.log(logger::debug, "method id in netoFindPsiMethodByMethodCodeObjectId does not contain \$_$ {}", methodId)
                return@runInReadAccessWithResult null
            }

            //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
            val className = methodId.substring(0, methodId.indexOf("\$_$")).replace('$', '.')

            return@runInReadAccessWithResult allowSlowOperation<UMethod?> {
                val uClass = findClassByClassName(className, GlobalSearchScope.projectScope(project))

                return@allowSlowOperation uClass?.let { cls ->
                    findMethodInClass(cls, methodId)
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

            runInReadAccess {

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


    override suspend fun detectMethodUnderCaret(virtualFile: VirtualFile, editor: Editor, caretOffset: Int): MethodUnderCaret {

        return try {
            smartReadAction(project) {
                if (!isSupportedFile(virtualFile)) {
                    return@smartReadAction MethodUnderCaret.empty(virtualFile.url)
                }
                return@smartReadAction detectMethodUnderCaretImpl(virtualFile, caretOffset)
            }
        } catch (e: Exception) {
            ErrorReporter.getInstance().reportError(project, "${this::class.java.simpleName}.detectMethodUnderCaret", e)
            MethodUnderCaret.empty(virtualFile.url)
        }
    }


    private fun detectMethodUnderCaretImpl(virtualFile: VirtualFile, caretOffset: Int): MethodUnderCaret {

        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return MethodUnderCaret.empty(virtualFile.url)
        val packageName = psiFile.toUElementOfType<UFile>()?.packageName ?: ""
        val underCaret: PsiElement = psiFile.findElementAt(caretOffset) ?: return MethodUnderCaret.empty(virtualFile.url)
        val uMethod = findParentMethod(underCaret)
        val className: String = uMethod?.getParentOfType<UClass>()?.let {
            getClassSimpleName(it)
        } ?: ""

        if (uMethod != null) {

            val methodId = createMethodCodeObjectId(uMethod)
            val endpointTextRange = findEndpointTextRange(virtualFile, caretOffset, methodId)

            return MethodUnderCaret(
                methodId,
                uMethod.name,
                className,
                packageName,
                virtualFile.url,
                caretOffset,
                endpointTextRange
            )
        }
        return MethodUnderCaret.empty(virtualFile.url)
    }


    private fun findEndpointTextRange(virtualFile: VirtualFile, caretOffset: Int, methodId: String): TextRange? {
        val documentInfo = DocumentInfoStorage.getInstance(project).getDocumentInfo(virtualFile)
        if (documentInfo != null) {
            val methodInfo = documentInfo.methods[methodId]
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

        runInReadAccess {
            allowSlowOperation {
                try {
                    var className = methodId.substring(0, methodId.indexOf("\$_$"))
                    //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
                    className = className.replace('$', '.')

                    val cls = findClassByClassName(className, GlobalSearchScope.allScope(project))
                    cls?.let {

                        val method = findMethodInClass(it, methodId)
                        if (method?.sourcePsi is Navigatable && (method.sourcePsi as Navigatable).canNavigate()) {
                            Log.log(logger::debug, "navigating to method {}", method)
                            EDT.ensureEDT {
                                (method.sourcePsi as Navigatable).navigate(true)
                            }
                            return@allowSlowOperation
                        }
                    }
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError(project, "${this::class.java.simpleName}.navigateToMethod", e)
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


        return runInReadAccessWithResult {

            return@runInReadAccessWithResult allowSlowOperation<PsiElement?> {

                return@allowSlowOperation try {

                    val methodAndClass: Pair<String, String> = CodeObjectsUtil.getMethodClassAndName(methodId)
                    //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
                    val className = methodAndClass.first.replace('$', '.')
                    val cls = findClassByClassName(className, GlobalSearchScope.allScope(project))
                    cls?.let {
                        val method = findMethodInClass(it, methodId)
                        method?.sourcePsi
                    }
                } catch (e: Exception) {
                    ErrorReporter.getInstance().reportError(project, "${this::class.java.simpleName}.getPsiElementForMethod", e)
                    null
                }
            }
        }
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

        return runInReadAccessWithResult {

            return@runInReadAccessWithResult allowSlowOperation<PsiElement?> {

                return@allowSlowOperation try {

                    //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
                    val classNameToFind = className.replace('$', '.')
                    val uClass = findClassByClassName(classNameToFind, GlobalSearchScope.allScope(project))
                    return@allowSlowOperation uClass?.sourcePsi
                } catch (e: Exception) {
                    ErrorReporter.getInstance().reportError(project, "${this::class.java.simpleName}.getPsiElementForClassByName", e)
                    null
                }
            }
        }
    }


    override fun canInstrumentMethod(methodId: String): MethodObservabilityInfo {

        class MyComputable : Computable<MethodObservabilityInfo> {

            val progressIndicator: ProgressIndicator = EmptyProgressIndicator()

            override fun compute(): MethodObservabilityInfo {

                try {
                    val uMethod = findMethodByMethodCodeObjectId(methodId)
                    if (uMethod?.sourcePsi == null) {
                        Log.log(logger::trace, "Failed to get Method from method id '{}'", methodId)
                        return MethodObservabilityInfo(methodId, hasMissingDependency = false, canInstrumentMethod = false, hasAnnotation = false)
                    }

                    progressIndicator.checkCanceled()

                    val psiFile = uMethod.getContainingUFile()
                    if (psiFile == null || !isSupportedFile(psiFile.sourcePsi)) {
                        Log.log(logger::trace, "Method's file is not supported file (methodId: {})", methodId)
                        return MethodObservabilityInfo(methodId, hasMissingDependency = false, canInstrumentMethod = false, hasAnnotation = false)
                    }

                    progressIndicator.checkCanceled()

                    val module = ModuleUtilCore.findModuleForPsiElement(uMethod.sourcePsi!!)
                    if (module == null) {
                        Log.log(logger::trace, "Failed to get module from PsiMethod '{}'", methodId)
                        return MethodObservabilityInfo(methodId, hasMissingDependency = false, canInstrumentMethod = false, hasAnnotation = false)
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
                            Log.log(logger::trace, "Failed to not lookup module ext by module name='{}'", module.name)
                            return MethodObservabilityInfo(methodId, hasMissingDependency = false, canInstrumentMethod = false, hasAnnotation = false)
                        }
                        val hasDeps = modulesDepsService.isModuleHasNeededDependenciesForSpringBootWithMicrometer(moduleExt.metadata)
                        if (!hasDeps) {
                            return MethodObservabilityInfo(methodId, hasMissingDependency = true, canInstrumentMethod = false, hasAnnotation = false)
                        }
                    }

                    progressIndicator.checkCanceled()

                    val annotationPsiClass = JavaPsiFacade.getInstance(project).findClass(
                        annotationClassFqn,
                        GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
                    )
                    if (annotationPsiClass == null) {
                        Log.log(logger::trace, "Cannot find WithSpan PsiClass (methodId: {}) (module:{})", methodId, module)
                        return MethodObservabilityInfo(methodId, hasMissingDependency = true, canInstrumentMethod = false, hasAnnotation = false)
                    }

                    val hasAnnotation = uMethod.uAnnotations.any { uAnnotation -> uAnnotation.qualifiedName == annotationClassFqn }

                    return MethodObservabilityInfo(
                        methodId,
                        hasMissingDependency = false,
                        canInstrumentMethod = true,
                        annotationClassFqn = annotationClassFqn,
                        hasAnnotation = hasAnnotation
                    )

                } catch (e: ProcessCanceledException) {
                    throw e
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError(project, "${this::class.java.simpleName}.canInstrumentMethod", e)
                }

                return MethodObservabilityInfo(methodId, hasMissingDependency = false, canInstrumentMethod = false, hasAnnotation = false)
            }

        }


        return Retries.retryWithResultAndDefault(
            {
                val myComputable = MyComputable()
                val result = ProgressManager.getInstance().runProcess(Computable {
                    runInReadAccessWithResult {
                        myComputable.compute()
                    }
                }, myComputable.progressIndicator)
                return@retryWithResultAndDefault result
            },
            Throwable::class.java,
            50,
            5,
            MethodObservabilityInfo(methodId, hasMissingDependency = false, canInstrumentMethod = false, hasAnnotation = false)
        )
    }


    protected fun isSpringBootAndMicrometer(module: Module): Boolean {
        val modulesDepsService = ModulesDepsService.getInstance(project)

        return (modulesDepsService.isSpringBootModule(module)
                && SpringBootMicrometerConfigureDepsService.isSpringBootWithMicrometer())
    }


    @Suppress("UnstableApiUsage")
    override fun addDependencyToOtelLib(methodId: String) {
        val module = getModuleOfMethodId(methodId)
        if (module == null) {
            Log.log(logger::warn, "Failed to add dependencies OTEL lib since could not lookup module by methodId='{}'", methodId)
            return
        }

        if (isSpringBootAndMicrometer(module)) {
            addDepsForSpringBootAndMicrometer(module)
        } else {
            val moduleBuildSystem = project.service<JvmBuildSystemHelperService>().determineBuildSystem(module)
            val dependencyLib = mapBuildSystem2Dependency[moduleBuildSystem]

            val dependencyModifierService = DependencyModifierService.getInstance(project)

            if (dependencyLib != null) {
                dependencyModifierService.addDependency(module, dependencyLib)
            }
        }

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