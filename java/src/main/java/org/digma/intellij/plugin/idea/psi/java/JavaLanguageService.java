package org.digma.intellij.plugin.idea.psi.java;

import com.google.common.collect.ImmutableMap;
import com.intellij.buildsystem.model.unified.UnifiedCoordinates;
import com.intellij.buildsystem.model.unified.UnifiedDependency;
import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.externalSystem.DependencyModifierService;
import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Pair;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.common.ReadActions;
import org.digma.intellij.plugin.editor.EditorUtils;
import org.digma.intellij.plugin.idea.build.BuildSystemChecker;
import org.digma.intellij.plugin.idea.build.JavaBuildSystem;
import org.digma.intellij.plugin.idea.deps.ModulesDepsService;
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.EndpointInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.CanInstrumentMethodResult;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JavaLanguageService implements LanguageService {

    private static final Logger LOGGER = Logger.getInstance(JavaLanguageService.class);


    private final Project project;

    private final ProjectFileIndex projectFileIndex;

    private final MicronautFramework micronautFramework;
    private final GrpcFramework grpcFramework;
    private final SpringBootFramework springBootFramework;
    private final List<IEndpointDiscovery> endpointDiscoveryList;

    private static final String OtelDependencyVersion = "1.26.0";
    private static final UnifiedCoordinates OtelCoordinates = new UnifiedCoordinates("io.opentelemetry.instrumentation", "opentelemetry-instrumentation-annotations", OtelDependencyVersion);
    private static final ImmutableMap<JavaBuildSystem, UnifiedDependency> MapBuildSystem2Dependency;

    static {
        var builder = new ImmutableMap.Builder<JavaBuildSystem, UnifiedDependency>();
        builder.put(JavaBuildSystem.UNKNOWN, new UnifiedDependency(OtelCoordinates, "compile"));
        builder.put(JavaBuildSystem.MAVEN, new UnifiedDependency(OtelCoordinates, null));
        builder.put(JavaBuildSystem.GRADLE, new UnifiedDependency(OtelCoordinates, "implementation"));
        MapBuildSystem2Dependency = builder.build();
    }

    /*
    It's better, as much as possible, in language services especially, not to initialize service dependencies in the constructor but use
    a getInstance for services when they are first needed. that will minimize the possibility for cyclic dependencies.
     */
    public JavaLanguageService(Project project) {
        this.project = project;
        this.projectFileIndex = project.getService(ProjectFileIndex.class);
        this.micronautFramework = new MicronautFramework(project);
        var jaxrsJavaxFramework = new JaxrsJavaxFramework(project);
        var jaxrsJakartaFramework = new JaxrsJakartaFramework(project);
        this.grpcFramework = new GrpcFramework(project);
        this.springBootFramework = new SpringBootFramework(project);
        this.endpointDiscoveryList = List.of(micronautFramework, jaxrsJavaxFramework, jaxrsJakartaFramework, grpcFramework, springBootFramework);
    }

    public List<IEndpointDiscovery> getListOfEndpointDiscovery() {
        return endpointDiscoveryList;
    }

    @Override
    public void ensureStartupOnEDT(@NotNull Project project) {
        //nothing to do
    }

    @Override
    public void runWhenSmart(Runnable task) {
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(task);
        } else {
            task.run();
        }
    }

    @Nullable
    @Override
    public Language getLanguageForMethodCodeObjectId(@NotNull String methodId) {

        //try to parse the methodId as if it is java and try to find the language
        if (methodId.indexOf("$_$") <= 0) {
            Log.log(LOGGER::debug, "method id in getLanguageForMethodCodeObjectId does not contain $_$ {}", methodId);
            return null;
        }

        var className = methodId.substring(0, methodId.indexOf("$_$"));

        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        className = className.replace('$', '.');

        var finalClassName = className;
        return ReadAction.compute(() -> {
            Collection<PsiClass> psiClasses =
                    JavaFullClassNameIndex.getInstance().get(finalClassName, project, GlobalSearchScope.projectScope(project));
            if (!psiClasses.isEmpty()) {
                Optional<PsiClass> psiClass = psiClasses.stream().findAny();
                //noinspection ConstantConditions
                if (psiClass.isPresent()) {
                    return psiClass.get().getLanguage();
                }
            }
            return null;
        });
    }


    @Override
    public boolean isSupportedFile(@NotNull Project project, @NotNull VirtualFile newFile) {
        //maybe more correct to find view provider and find a java psi file
        PsiFile psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(newFile);
        return psiFile != null && JavaLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    public boolean isSupportedFile(@NotNull Project project, @NotNull PsiFile psiFile) {
        return JavaLanguage.INSTANCE.equals(psiFile.getLanguage()) && !psiFile.getName().contains("package-info");
    }

    @Override
    @NotNull
    public MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor selectedEditor, int caretOffset) {
        String fileUri = PsiUtils.psiFileToUri(psiFile);
        if (!isSupportedFile(project, psiFile)) {
            return new MethodUnderCaret("", "", "", "", fileUri, false);
        }
        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
        String packageName = psiJavaFile.getPackageName();
        PsiElement underCaret = psiFile.findElementAt(caretOffset);
        if (underCaret == null) {
            return new MethodUnderCaret("", "", "", packageName, fileUri, true);
        }
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(underCaret, PsiMethod.class);
        String className = safelyTryGetClassName(underCaret);
        if (psiMethod != null) {
            return new MethodUnderCaret(JavaLanguageUtils.createJavaMethodCodeObjectId(psiMethod), psiMethod.getName(), className, packageName, fileUri);
        }

        return new MethodUnderCaret("", "", className, packageName, fileUri);
    }

    private String safelyTryGetClassName(PsiElement element) {
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass != null) {
            String className = psiClass.getName();
            if (className != null) return className;
        }
        return "";
    }

    protected boolean isSpringBootAndMicrometer(@NotNull Module module) {
        var modulesDepsService = ModulesDepsService.getInstance(project);
        var springBootMicrometerConfigureDepsService = SpringBootMicrometerConfigureDepsService.getInstance(project);

        var retVal = (modulesDepsService.isSpringBootModule(module)
                && springBootMicrometerConfigureDepsService.isSpringBootWithMicrometer());
        return retVal;
    }

    @NotNull
    public CanInstrumentMethodResult canInstrumentMethod(@NotNull Project project, @Nullable String methodId) {

        var psiMethod = findPsiMethodByMethodCodeObjectId(methodId);
        if (psiMethod == null) {
            Log.log(LOGGER::warn, "Failed to get PsiMethod from method id '{}'", methodId);
            return CanInstrumentMethodResult.Failure();
        }

        var psiFile = psiMethod.getContainingFile();
        if (!(psiFile instanceof PsiJavaFile psiJavaFile)) {
            Log.log(LOGGER::warn, "PsiMethod's file is not java file (methodId: {})", methodId);
            return CanInstrumentMethodResult.Failure();
        }

        var module = ModuleUtilCore.findModuleForPsiElement(psiMethod);
        if (module == null) {
            Log.log(LOGGER::warn, "Failed to get module from PsiMethod '{}'", methodId);
            return CanInstrumentMethodResult.Failure();
        }

        var annotationClassFqn = Constants.WITH_SPAN_FQN;
        var dependencyCause = Constants.WITH_SPAN_DEPENDENCY_DESCRIPTION;
        if (isSpringBootAndMicrometer(module)) {
            annotationClassFqn = MicrometerTracingFramework.OBSERVED_FQN;
            dependencyCause = MicrometerTracingFramework.OBSERVED_DEPENDENCY_DESCRIPTION;

            var modulesDepsService = ModulesDepsService.getInstance(project);
            var moduleExt = modulesDepsService.getModuleExt(module.getName());
            if (moduleExt == null) {
                Log.log(LOGGER::warn, "Failed to not lookup module ext by module name='{}'", module.getName());
                return CanInstrumentMethodResult.Failure();
            }
            boolean hasDeps = modulesDepsService.isModuleHasNeededDependenciesForSpringBootWithMicrometer(moduleExt.getMetadata());
            if (!hasDeps) {
                return new CanInstrumentMethodResult(new CanInstrumentMethodResult.MissingDependencyCause(dependencyCause));
            }
        }

        var annotationPsiClass = JavaPsiFacade.getInstance(project).findClass(annotationClassFqn,
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false));
        if (annotationPsiClass == null) {
            Log.log(LOGGER::warn, "Cannot find WithSpan PsiClass (methodId: {}) (module:{})", methodId, module);
            return new CanInstrumentMethodResult(new CanInstrumentMethodResult.MissingDependencyCause(dependencyCause));
        }

        return new JavaCanInstrumentMethodResult(methodId, psiMethod, annotationPsiClass, psiJavaFile);
    }

    public boolean instrumentMethod(@NotNull CanInstrumentMethodResult result) {

        if (!(result instanceof JavaCanInstrumentMethodResult goodResult)) {
            Log.log(LOGGER::warn, "instrumentMethod was called with failing result from canInstrumentMethod");
            return false;
        }

        var psiJavaFile = goodResult.psiJavaFile;
        var psiMethod = goodResult.psiMethod;
        var methodId = goodResult.methodId;
        var withSpanClass = goodResult.withSpanClass;

        var importList = psiJavaFile.getImportList();
        if (importList == null) {
            Log.log(LOGGER::warn, "Failed to get ImportList from PsiFile (methodId: {})", methodId);
            return false;
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            var psiFactory = PsiElementFactory.getInstance(project);

            var shortClassNameAnnotation = withSpanClass.getName();
            psiMethod.getModifierList().addAnnotation(shortClassNameAnnotation);

            var existing = importList.findSingleClassImportStatement(withSpanClass.getQualifiedName());
            if (existing == null) {
                var importStatement = psiFactory.createImportStatement(withSpanClass);
                importList.add(importStatement);
            }
        });
        return true;
    }

    @Nullable
    private Module getModuleOfMethodId(String methodCodeObjectId) {
        var psiMethod = findPsiMethodByMethodCodeObjectId(methodCodeObjectId);
        if (psiMethod == null) {
            Log.log(LOGGER::warn, "Failed to get PsiMethod from method id '{}'", methodCodeObjectId);
            return null;
        }

        var module = ModuleUtilCore.findModuleForPsiElement(psiMethod);
        if (module == null) {
            Log.log(LOGGER::warn, "Failed to get module from PsiMethod '{}'", methodCodeObjectId);
            return null;
        }

        return module;
    }

    @Override
    public void addDependencyToOtelLib(@NotNull Project project, @NotNull String methodId) {
        Module module = getModuleOfMethodId(methodId);
        if (module == null) {
            Log.log(LOGGER::warn, "Failed to add dependencies OTEL lib since could not lookup module by methodId='{}'", methodId);
            return;
        }
        if (!isSpringBootAndMicrometer(module)) {
            JavaBuildSystem moduleBuildSystem = BuildSystemChecker.Companion.determineBuildSystem(module);
            UnifiedDependency dependencyLib = MapBuildSystem2Dependency.get(moduleBuildSystem);

            var dependencyModifierService = DependencyModifierService.getInstance(project);

            dependencyModifierService.addDependency(module, dependencyLib);
            return;
        }
        // handling spring boot with micrometer tracing
        addDepsForSpringBootAndMicrometer(module);
    }

    protected void addDepsForSpringBootAndMicrometer(@NotNull Module module) {
        var modulesDepsService = ModulesDepsService.getInstance(project);
        var moduleExt = modulesDepsService.getModuleExt(module.getName());
        if (moduleExt == null) {
            Log.log(LOGGER::warn, "Failed add dependencies of Spring Boot Micrometer since could not lookup module ext by module name='{}'", module.getName());
            return;
        }
        var project = module.getProject();
        var springBootMicrometerConfigureDepsService = SpringBootMicrometerConfigureDepsService.getInstance(project);
        springBootMicrometerConfigureDepsService.addMissingDependenciesForSpringBootObservability(moduleExt);
    }

    /**
     * Navigate to any method in the project even if the file is not opened
     */
    @Override
    public void navigateToMethod(String methodId) {

        Log.log(LOGGER::debug, "got navigate to method request {}", methodId);
        if (methodId.indexOf("$_$") <= 0) {
            Log.log(LOGGER::debug, "method id in navigateToMethod does not contain $_$, can not navigate {}", methodId);
            return;
        }

        ReadActions.ensureReadAction(() -> {

            var className = methodId.substring(0, methodId.indexOf("$_$"));
            //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
            className = className.replace('$', '.');
            //searching in project scope will find only project classes
            Collection<PsiClass> psiClasses =
                    JavaFullClassNameIndex.getInstance().get(className, project, GlobalSearchScope.allScope(project));
            if (!psiClasses.isEmpty()) {
                //hopefully there is only one class by that name in the project
                Optional<PsiClass> psiClassOptional = psiClasses.stream().findAny();
                PsiClass psiClass = psiClassOptional.get();
                for (PsiMethod method : JavaPsiUtils.getMethodsOf(psiClass)) {
                    var id = JavaLanguageUtils.createJavaMethodCodeObjectId(method);
                    if (id.equals(methodId) && method.canNavigate()) {
                        Log.log(LOGGER::debug, "navigating to method {}", method);
                        method.navigate(true);
                        return;
                    }
                }
            }
        });

    }


    @Override
    public boolean isServiceFor(@NotNull Language language) {
        return JavaLanguage.class.equals(language.getClass());
    }

    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(List<String> codeObjectIds) {

        /*
        Use intellij index to find in which file a method exists.
        We don't need to search for the method, only for the class. and we don't need offset because
        the backend provides a line number to navigate to.

        We could use the class name that is returned from the backend in org.digma.intellij.plugin.model.rest.errordetails.Frame.
        but then we need to change the C# implementation too.
        but codeObjectIds is usually our identifier all over the plugin, so maybe it's better to just use it for all languages.
        the string parsing of codeObjectIds should not be expensive.
         */


        Map<String, String> workspaceUrls = new HashMap<>();

        codeObjectIds.forEach(methodId -> {

            if (methodId.contains("$_$")) {

                ReadActions.ensureReadAction(() -> {

                    var className = methodId.substring(0, methodId.indexOf("$_$"));

                    //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
                    className = className.replace('$', '.');

                    //searching in project scope will find only project classes
                    Collection<PsiClass> psiClasses =
                            JavaFullClassNameIndex.getInstance().get(className, project, GlobalSearchScope.projectScope(project));
                    if (!psiClasses.isEmpty()) {
                        //hopefully there is only one class by that name in the project
                        PsiClass psiClass = psiClasses.stream().findAny().get();
                        PsiFile psiFile = PsiTreeUtil.getParentOfType(psiClass, PsiFile.class);
                        if (psiFile != null) {
                            String url = PsiUtils.psiFileToUri(psiFile);
                            workspaceUrls.put(methodId, url);
                        }
                    }
                });

            } else {
                Log.log(LOGGER::debug, "method id in findWorkspaceUrisForCodeObjectIdsForErrorStackTrace does not contain $_$ {}", methodId);
            }
        });

        return workspaceUrls;
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForMethodCodeObjectIds(List<String> methodCodeObjectIds) {

        Map<String, Pair<String, Integer>> workspaceUrls = new HashMap<>();

        methodCodeObjectIds.forEach(methodId -> ReadActions.ensureReadAction(() -> {
            var psiMethod = findPsiMethodByMethodCodeObjectId(methodId);
            if (psiMethod != null) {
                String url = PsiUtils.psiFileToUri(psiMethod.getContainingFile());
                workspaceUrls.put(methodId, new Pair<>(url, psiMethod.getTextOffset()));
            }
        }));

        return workspaceUrls;
    }

    private @Nullable PsiMethod findPsiMethodByMethodCodeObjectId(@Nullable String methodId) {
        if (methodId == null) return null;

        if (!methodId.contains("$_$")) {
            Log.log(LOGGER::debug, "method id in findWorkspaceUrisForMethodCodeObjectIds does not contain $_$ {}", methodId);
            return null;
        }

        return ReadActions.ensureReadAction(() -> netoFindPsiMethodByMethodCodeObjectId(methodId));

    }

    private @Nullable PsiMethod netoFindPsiMethodByMethodCodeObjectId(@NotNull String methodId) {
        if (!methodId.contains("$_$")) {
            Log.log(LOGGER::debug, "method id in netoFindPsiMethodByMethodCodeObjectId does not contain $_$ {}", methodId);
            return null;
        }

        var className = methodId.substring(0, methodId.indexOf("$_$"));
        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        className = className.replace('$', '.');

        //searching in project scope will find only project classes
        Collection<PsiClass> psiClasses = JavaFullClassNameIndex.getInstance().get(className, project, GlobalSearchScope.projectScope(project));
        if (!psiClasses.isEmpty()) {
            //hopefully there is only one class by that name in the project
            PsiClass psiClass = psiClasses.stream().findAny().get();
            PsiFile psiFile = PsiTreeUtil.getParentOfType(psiClass, PsiFile.class);
            for (PsiMethod method : JavaPsiUtils.getMethodsOf(psiClass)) {
                String javaMethodCodeObjectId = JavaLanguageUtils.createJavaMethodCodeObjectId(method);
                if (javaMethodCodeObjectId.equals(methodId) && psiFile != null) {
                    return method;
                }
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(@NotNull List<String> spanIds) {
        return JavaSpanNavigationProvider.getInstance(project).getUrisForSpanIds(spanIds);
    }

    @Override
    public Set<EndpointInfo> lookForDiscoveredEndpoints(String endpointId) {
        return JavaEndpointNavigationProvider.getInstance(project).getEndpointInfos(endpointId);
    }

    @Override
    public void environmentChanged(String newEnv, boolean refreshInsightsView) {

        if (refreshInsightsView) {
            EDT.ensureEDT(() -> {
                var fileEditor = FileEditorManager.getInstance(project).getSelectedEditor();
                if (fileEditor != null) {
                    var file = fileEditor.getFile();
                    var psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile != null && isRelevant(psiFile.getVirtualFile())) {
                        var selectedTextEditor = EditorUtils.getSelectedTextEditorForFile(file, FileEditorManager.getInstance(project));
                        if (selectedTextEditor != null) {
                            int offset = selectedTextEditor.getCaretModel().getOffset();
                            var methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedTextEditor, offset);
                            CaretContextService.getInstance(project).contextChanged(methodUnderCaret);
                        }
                    }
                }
            });
        }

        JavaCodeLensService.getInstance(project).environmentChanged(newEnv);
    }


    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile) {
        Log.log(LOGGER::debug, "got buildDocumentInfo request for {}", psiFile);
        //must be PsiJavaFile , this method should be called only for java files
        if (psiFile instanceof PsiJavaFile psiJavaFile) {
            return JavaCodeObjectDiscovery.buildDocumentInfo(project, psiJavaFile, endpointDiscoveryList);
        } else {
            Log.log(LOGGER::debug, "psi file is noy java, returning empty DocumentInfo for {}", psiFile);
            return new DocumentInfo(PsiUtils.psiFileToUri(psiFile), new HashMap<>());
        }
    }

    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, @Nullable FileEditor newEditor) {
        return buildDocumentInfo(psiFile);
    }


    @Override
    public boolean isRelevant(VirtualFile file) {

        if (file.isDirectory() || !file.isValid()) {
            return false;
        }

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return false;
        }

        return isRelevant(psiFile);
    }


    @Override
    public boolean isRelevant(PsiFile psiFile) {
        return psiFile.isValid() &&
                psiFile.isWritable() &&
                projectFileIndex.isInSourceContent(psiFile.getVirtualFile()) &&
                !projectFileIndex.isInLibrary(psiFile.getVirtualFile()) &&
                !projectFileIndex.isExcluded(psiFile.getVirtualFile()) &&
                isSupportedFile(project, psiFile) &&
                !JavaDocumentInfoIndex.namesToExclude.contains(psiFile.getVirtualFile().getName());
    }

    @Override
    public void refreshMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor selectedEditor, int offset) {
        MethodUnderCaret methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedEditor, offset);
        CaretContextService.getInstance(project).contextChanged(methodUnderCaret);
    }

    @Override
    public boolean isCodeVisionSupported() {
        return true;
    }

    @Override
    public @NotNull List<Pair<TextRange, CodeVisionEntry>> getCodeLens(@NotNull PsiFile psiFile) {
        return JavaCodeLensService.getInstance(project).getCodeLens(psiFile);
    }

    private static final class JavaCanInstrumentMethodResult extends CanInstrumentMethodResult {
        private final String methodId;
        private final PsiMethod psiMethod;
        private final PsiClass withSpanClass;
        private final PsiJavaFile psiJavaFile;

        private JavaCanInstrumentMethodResult(String methodId, PsiMethod psiMethod, PsiClass withSpanClass,
                                              PsiJavaFile psiJavaFile) {
            this.methodId = methodId;
            this.psiMethod = psiMethod;
            this.withSpanClass = withSpanClass;
            this.psiJavaFile = psiJavaFile;
        }
    }
}
