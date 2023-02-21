package org.digma.intellij.plugin.pycharm.psi.python;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFunction;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class PythonCodeObjectsDiscovery {


    public static @NotNull DocumentInfo buildDocumentInfo(Project project, PsiFile psiFile) {

        var fileUri = PsiUtils.psiFileToUri(psiFile);
        var methods = new HashMap<String, MethodInfo>();

        PyFile psiPyFile = (PyFile) psiFile;
        var functions = psiPyFile.getTopLevelFunctions();
        var classes = psiPyFile.getTopLevelClasses();

        classes.forEach(pyClass -> {
            for (PyFunction method : pyClass.getMethods()) {
                //function name should probably never be null but the interface is Nullable so we need to check
                if (method.getName() != null) {
                    MethodInfo methodInfo = processFunction(project,fileUri, method);
                    methods.put(methodInfo.getId(), methodInfo);
                }
            }
        });

        functions.forEach(pyFunction -> {
            //function name should probably never be null but the interface is Nullable so we need to check
            if (pyFunction.getName() != null) {
                MethodInfo methodInfo = processFunction(project, fileUri, pyFunction);
                methods.put(methodInfo.getId(), methodInfo);
            }
        });

        return new DocumentInfo(fileUri, methods);
    }

    private static MethodInfo processFunction(Project project, String fileUri, PyFunction pyFunction) {
        Objects.requireNonNull(pyFunction);
        Objects.requireNonNull(pyFunction.getName());

        var methodId = PythonLanguageUtils.createPythonMethodCodeObjectId(project,pyFunction);
        List<SpanInfo> spans = discoverSpans(project,pyFunction);

        var name = pyFunction.getName();
        var className = pyFunction.getContainingClass() == null ? "" : pyFunction.getContainingClass().getName() == null ? "" : pyFunction.getContainingClass().getName();
        var namespace = pyFunction.getQualifiedName() == null ? "" : pyFunction.getQualifiedName().substring(0, pyFunction.getQualifiedName().lastIndexOf("."));

        var methodInfo = new MethodInfo(methodId, name, className, namespace, fileUri, pyFunction.getTextOffset(), spans);
        methodInfo.setAdditionalIdsProvider(new PythonAdditionalIdsProvider());
        return methodInfo;
    }

    private static List<SpanInfo> discoverSpans(Project project, PyFunction pyFunction) {

//        ReferencesSearch.search()
//        new PyStringReferenceSearch().processQuery();
//        PySearchUtil.computeElementNameForStringSearch()
//        PyPsiIndexUtil.findUsages()


//        var traceClass = findTracerClass(project)

        //PyFunction pyFunction1 = ((PyReferenceExpression)((PyWithStatement)pyFunction.getChildren()[2].getChildren()[0]).getWithItems()[0].getChildren()[0].getChildren()[0]).getReference().resolve();

        //((((PyCallExpression)((PyWithStatement)pyFunction.getChildren()[2].getChildren()[0]).getWithItems()[0].getChildren()[0]).getCallee().getChildren()[0].getReference().resolve().getReference().resolve()).getContext().getChildren()[0]
        return new ArrayList<>();
    }

    @Nullable
    public static PyFunction findMethodInFile(Project project, @NotNull PyFile pyFile, @NotNull String codeObjectId) {

        var classes = pyFile.getTopLevelClasses();
        var functions = pyFile.getTopLevelFunctions();

        for (PyClass aClass : classes) {
            for (PyFunction method : aClass.getMethods()) {
                var objectId = PythonLanguageUtils.createPythonMethodCodeObjectId(project,method);
                if (objectId.equals(codeObjectId)){
                    return method;
                }
            }
        }

        for (PyFunction function : functions) {
            var objectId = PythonLanguageUtils.createPythonMethodCodeObjectId(project,function);
            if (objectId.equals(codeObjectId)){
                return function;
            }
        }

        return null;
    }
}
