package org.digma.intellij.plugin.psi.python;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import com.jetbrains.python.psi.PyAssignmentStatement;
import com.jetbrains.python.psi.PyCallExpression;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.jetbrains.python.psi.PyStringLiteralExpression;
import com.jetbrains.python.psi.PyTargetExpression;
import com.jetbrains.python.psi.stubs.PyFunctionNameIndex;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PythonCodeObjectsDiscovery {

    private static final Logger LOGGER = Logger.getInstance(PythonCodeObjectsDiscovery.class);

    private static final String UNKNOWN_INST_LIBRARY = "UNKNOWN_INST_LIBRARY";
    private static final String UNKNOWN_SPAN_NAME = "UNKNOWN_SPAN_NAME";

    public static @NotNull DocumentInfo buildDocumentInfo(@NotNull Project project, @NotNull PyFile pyFile) {
        var stopWatch = StopWatch.createStarted();

        try {
            return buildDocumentInfoImpl(project, pyFile);
        } finally {
            stopWatch.stop();
            Log.log(LOGGER::debug, "buildDocumentInfo for {} took {} milliseconds", pyFile.getName(), stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }


    private static @NotNull DocumentInfo buildDocumentInfoImpl(@NotNull Project project, @NotNull PyFile pyFile) {

        var fileUri = PsiUtils.psiFileToUri(pyFile);
        var methods = new HashMap<String, MethodInfo>();

        var functions = pyFile.getTopLevelFunctions();
        var classes = pyFile.getTopLevelClasses();

        classes.forEach(pyClass -> {
            for (PyFunction method : pyClass.getMethods()) {
                //function name should probably never be null but the interface is Nullable, so we need to check
                if (method.getName() != null) {
                    MethodInfo methodInfo = functionDiscovery(project, fileUri, method);
                    methods.put(methodInfo.getId(), methodInfo);
                }
            }
        });

        functions.forEach(pyFunction -> {
            //function name should probably never be null but the interface is Nullable, so we need to check
            if (pyFunction.getName() != null) {
                MethodInfo methodInfo = functionDiscovery(project, fileUri, pyFunction);
                methods.put(methodInfo.getId(), methodInfo);
            }
        });

        spanDiscovery(project, pyFile, fileUri, methods);
        return new DocumentInfo(fileUri, methods);
    }


    @NotNull
    private static MethodInfo functionDiscovery(@NotNull Project project, @NotNull String fileUri, @NotNull PyFunction pyFunction) {
        Objects.requireNonNull(pyFunction);
        Objects.requireNonNull(pyFunction.getName());

        var methodId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, pyFunction);

        var name = pyFunction.getName();
        var className = pyFunction.getContainingClass() == null ? "" : pyFunction.getContainingClass().getName() == null ? "" : pyFunction.getContainingClass().getName();
        var namespace = pyFunction.getQualifiedName() == null ? "" : pyFunction.getQualifiedName();
        if (namespace.lastIndexOf(".py") > 0){
            namespace = namespace.substring(0, pyFunction.getQualifiedName().lastIndexOf(".py"));
        }

        var methodInfo = new MethodInfo(methodId, name, className, namespace, fileUri, pyFunction.getTextOffset());
        methodInfo.setAdditionalIdsProvider(new PythonAdditionalIdsProvider());
        return methodInfo;
    }


    private static void spanDiscovery(@NotNull Project project, @NotNull PyFile pyFile, String fileUri, @NotNull Map<String, MethodInfo> methodInfoMap) {
        spanDiscovery(project, pyFile, fileUri, Constants.OPENTELEMETRY_START_AS_CURRENT_SPAN_FUNC_NAME, methodInfoMap);
        spanDiscovery(project, pyFile, fileUri, Constants.OPENTELEMETRY_START_SPAN_FUNC_NAME, methodInfoMap);
    }

    private static void spanDiscovery(@NotNull Project project, @NotNull PyFile pyFile, String fileUri, @NotNull String tracerMethodName, @NotNull Map<String, MethodInfo> methodInfoMap) {

        var functions = PyFunctionNameIndex.find(tracerMethodName, project,GlobalSearchScope.allScope(project));

        //for some reason the search returns two identical functions, so just choose the first one.
        // I expect only one and don't know why there are two.
        PyFunction startSpanFunction = functions.stream().filter(pyFunction -> pyFunction.getContainingClass() != null &&
                Constants.OPENTELEMETRY_TRACER_FQN.equals(pyFunction.getContainingClass().getQualifiedName())).findFirst().orElse(null);

        if (startSpanFunction == null) {
            return;
        }

        Query<PsiReference> references = ReferencesSearch.search(startSpanFunction, GlobalSearchScope.fileScope(pyFile));
        references.forEach(psiReference -> {
            Log.log(LOGGER::debug, "found reference to {} function {}", tracerMethodName, psiReference.getElement().getText());
            var pyCallExpression = PsiTreeUtil.getParentOfType(psiReference.getElement(), PyCallExpression.class);
            if (pyCallExpression != null) {
                Log.log(LOGGER::debug, "call expression to {} function is {} ", tracerMethodName, pyCallExpression.getText());
                //span discovery may return more than one SpanInfo, but they all should belong
                // to the same method. it happens when a span has more than one id.
                List<SpanInfo> spanInfos = discoverSpanFromStartSpanMethodCallExpression(project, pyFile, pyCallExpression, fileUri);
                if (spanInfos.size() > 0) {
                    MethodInfo methodInfo = methodInfoMap.get(spanInfos.get(0).getContainingMethodId());
                    if (methodInfo != null) {
                        methodInfo.getSpans().addAll(spanInfos);
                    }
                }
            }
        });
    }


    /**
     * this method discovers a span from a call to one of the open telemetry start span methods.
     * the call expression is something like tracer.start_span("span name")
     */
    @NotNull
    public static List<SpanInfo> discoverSpanFromStartSpanMethodCallExpression(@NotNull Project project, @NotNull PsiFile pyFile, @NotNull PyCallExpression pyCallExpression, String fileUri) {

        var result = new ArrayList<SpanInfo>();
        var args = pyCallExpression.getArguments();
        var function = PsiTreeUtil.getParentOfType(pyCallExpression, PyFunction.class);

        //don't know how to compute span name if there are no arguments or if receiver is null or if the span is not inside a function block
        if (args.length >= 1 && function != null && pyCallExpression.getReceiver(null) != null) {

            var methodId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, function);
            //span name is always the first argument to start_span or start_as_current_span
            var spanName = getSpanNameFromNameArgument(args[0]);
            //the receiver is the tracer object in this expression: tracer.start_span
            var receiver = pyCallExpression.getReceiver(null);
            //receiver should not be null, tested above
            var instLibrary = getInstLibraryFromReceiverExpression(Objects.requireNonNull(receiver));

            //in case the tracer name is the __name__ variable, create 3 spans with different instrumentation library names
            if (Constants.PYTHON_MODULE_NAME_VARIABLE.equals(instLibrary)) {

                //first instrumentation library name is "__main__"
                var spanId = PythonLanguageUtils.createSpanId("__main__", spanName);
                result.add(new SpanInfo(spanId, spanName, methodId, fileUri));

                //second instrumentation library is the relative path from project root or site-packages ,without the py extension.
                var relativePath = PythonLanguageUtils.getRelativePath(project,pyFile);
                if (relativePath.lastIndexOf(".py") >= 0){
                    relativePath = relativePath.substring(0, relativePath.lastIndexOf(".py"));
                }
                relativePath = relativePath.replace(File.separator, "."); //should work on linux and windows
                spanId = PythonLanguageUtils.createSpanId(relativePath, spanName);
                result.add(new SpanInfo(spanId, spanName, methodId, fileUri));

                //third instrumentation library is the relative path without its first segment if any
                if (relativePath.contains(".")) {
                    relativePath = relativePath.substring(relativePath.indexOf(".") + 1);
                    spanId = PythonLanguageUtils.createSpanId(relativePath, spanName);
                    result.add(new SpanInfo(spanId, spanName, methodId, fileUri));
                }

            } else {
                var spanId = PythonLanguageUtils.createSpanId(instLibrary, spanName);
                result.add(new SpanInfo(spanId, spanName, methodId, fileUri));
            }
        }

        return result;
    }


    //receiver expression is the tracer in tracer.start_span()
    @NotNull
    private static String getInstLibraryFromReceiverExpression(@NotNull PyExpression receiver) {

        if (receiver instanceof PyReferenceExpression referenceExpression) {
            return getInstLibraryFromReferenceExpression(referenceExpression);
        }else if(receiver instanceof PyCallExpression callExpression){
            return getInstLibraryFromCallExpression(callExpression);
        }

        return UNKNOWN_INST_LIBRARY;
    }


    @NotNull
    private static String getInstLibraryFromReferenceExpression(@NotNull PyReferenceExpression referenceExpression) {

        var psiElement = referenceExpression.getReference().resolve();
        if (psiElement != null) {
            PsiElement context = psiElement.getContext();
            if (context instanceof PyAssignmentStatement assignmentStatement) {
                return getInstLibraryFromAssignmentStatement(assignmentStatement);
            }
        }

        return UNKNOWN_INST_LIBRARY;
    }

    //assignmentStatement is the assignment in tracer = trace.get_tracer
    @NotNull
    private static String getInstLibraryFromAssignmentStatement(@NotNull PyAssignmentStatement assignmentStatement) {
        var assignedValue = assignmentStatement.getAssignedValue();
        if (assignedValue instanceof PyCallExpression callExpression) {
            return getInstLibraryFromCallExpression(callExpression);
        }

        return UNKNOWN_INST_LIBRARY;
    }

    //callExpression is the call to trace.get_tracer, we want the argument of get_tracer
    @NotNull
    private static String getInstLibraryFromCallExpression(@NotNull PyCallExpression callExpression) {
        var callee = callExpression.getCallee();
        if (callee != null && callee.getReference() != null) {
            var psiElement = callee.getReference().resolve();
            if (psiElement instanceof PyFunction pyFunction && Constants.OPENTELEMETRY_GET_TRACER_FUNC_NAME.equals(pyFunction.getName())) {
                var arguments = callExpression.getArguments();
                if (arguments.length >= 1) {
                    var arg = arguments[0];
                    return getInstLibraryFromGetTracerArgument(arg);
                }
            }
        }

        return UNKNOWN_INST_LIBRARY;
    }


    @NotNull
    private static String getInstLibraryFromGetTracerArgument(@NotNull PyExpression arg) {
        if (arg instanceof PyStringLiteralExpression stringLiteralExpression) {
            return getStringFromStringLiteralExpression(stringLiteralExpression);
        } else if (arg instanceof PyReferenceExpression referenceExpression) {
            return getInstLibraryFromArgumentReferenceExpression(referenceExpression);
        }

        return UNKNOWN_INST_LIBRARY;
    }


    @NotNull
    private static String getInstLibraryFromArgumentReferenceExpression(@NotNull PyReferenceExpression referenceExpression) {

        if (Constants.PYTHON_MODULE_NAME_VARIABLE.equals(referenceExpression.getText())) {
            return Constants.PYTHON_MODULE_NAME_VARIABLE;
        }

        var pyElement = referenceExpression.getReference().resolve();
        if (pyElement instanceof PyTargetExpression targetExpression){
            var assignedValue = targetExpression.findAssignedValue();
            if (assignedValue instanceof PyStringLiteralExpression stringLiteralExpression){
                return getStringFromStringLiteralExpression(stringLiteralExpression);
            }else if (assignedValue instanceof PyReferenceExpression pyReferenceExpressionAssignedValue){
                return getInstLibraryFromArgumentReferenceExpression(pyReferenceExpressionAssignedValue);
            }
        }

        return UNKNOWN_INST_LIBRARY;
    }


    @NotNull
    private static String getSpanNameFromNameArgument(@NotNull PyExpression pyExpression) {
        if (pyExpression instanceof PyStringLiteralExpression stringLiteralExpression) {
            return getStringFromStringLiteralExpression(stringLiteralExpression);
        }else if (pyExpression instanceof PyReferenceExpression referenceExpression){
            return getSpanNameFromReferenceExpression(referenceExpression);
        }

        return UNKNOWN_SPAN_NAME;
    }


    @NotNull
    private static String getSpanNameFromReferenceExpression(@NotNull PyReferenceExpression referenceExpression) {

        if (Constants.PYTHON_MODULE_NAME_VARIABLE.equals(referenceExpression.getText())) {
            return Constants.PYTHON_MODULE_NAME_VARIABLE;
        }

        var pyElement = referenceExpression.getReference().resolve();
        if (pyElement instanceof PyTargetExpression targetExpression){
            var assignedValue = targetExpression.findAssignedValue();
            if (assignedValue instanceof PyStringLiteralExpression stringLiteralExpression){
                return getStringFromStringLiteralExpression(stringLiteralExpression);
            }else if (assignedValue instanceof PyReferenceExpression pyReferenceExpressionAssignedValue){
                return getSpanNameFromReferenceExpression(pyReferenceExpressionAssignedValue);
            }
        }

        return UNKNOWN_SPAN_NAME;
    }


    @NotNull
    private static String getStringFromStringLiteralExpression(@NotNull PyStringLiteralExpression stringLiteralExpression) {
        return stringLiteralExpression.getStringValue();
    }


}
