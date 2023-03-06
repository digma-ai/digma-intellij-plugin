package org.digma.intellij.plugin.pycharm.psi.python;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFunction;
import org.digma.intellij.plugin.common.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;

public class PythonLanguageUtils {

    @NotNull
    public static String createPythonMethodCodeObjectId(@NotNull Project project, @NotNull PyFunction pyFunction) {
        var name = pyFunction.getName();
        var filePath = buildFilePath(project, pyFunction);
        return filePath + "$_$" + name;
    }


    @NotNull
    private static String buildFilePath(@NotNull Project project, @NotNull PyFunction pyFunction) {
        return getRelativePath(project, pyFunction.getContainingFile());
    }


    @NotNull
    public static String createSpanId(@NotNull String instLibrary, @NotNull String spanName) {
        return instLibrary + "$_$" + spanName;
    }

    @NotNull
    public static String extractFunctionNameFromCodeObjectId(@NotNull String methodId) {

        if (methodId.lastIndexOf("$_$") >= 0){
            return methodId.substring(methodId.lastIndexOf("$_$") + 3);
        }
        return methodId;
    }


    @NotNull
    public static String getRelativePath(@NotNull Project project, @NotNull PsiFile pyFile) {
        var spliterator = pyFile.getVirtualFile().toNioPath().spliterator();
        List<String> pathElements = StreamSupport.stream(spliterator, false).map(Path::toString).toList();
        if (pathElements.contains(Constants.SITE_PACKAGES_DIR_NAME)) {
            return pyFile.getVirtualFile().toNioPath().subpath(pathElements.indexOf(Constants.SITE_PACKAGES_DIR_NAME), pathElements.size()).toString();
        }
        if (pathElements.contains(project.getName())) {
            return pyFile.getVirtualFile().toNioPath().subpath(pathElements.indexOf(project.getName()), pathElements.size()).toString();
        }
        return pyFile.getVirtualFile().toNioPath().toString();
    }

    @Nullable
    public static PyFunction findMethodInFile(Project project, @NotNull PyFile pyFile, @NotNull String codeObjectId) {

        var classes = pyFile.getTopLevelClasses();
        var functions = pyFile.getTopLevelFunctions();

        for (PyClass aClass : classes) {
            for (PyFunction method : aClass.getMethods()) {
                var objectId = createPythonMethodCodeObjectId(project, method);
                if (objectId.equals(codeObjectId)) {
                    return method;
                }
            }
        }

        for (PyFunction function : functions) {
            var objectId = createPythonMethodCodeObjectId(project, function);
            if (objectId.equals(codeObjectId)) {
                return function;
            }
        }

        return null;
    }


    public static boolean isProjectFile(@NotNull Project project, @NotNull VirtualFile virtualFile) {

        if (FileUtils.isLightVirtualFileBase(virtualFile)){
            return false;
        }

        var spliterator = virtualFile.toNioPath().spliterator();
        List<String> pathElements = StreamSupport.stream(spliterator, false).map(Path::toString).toList();

        return !pathElements.contains(Constants.SITE_PACKAGES_DIR_NAME) &&
                pathElements.contains(project.getName());
    }

    public static boolean isProjectFile(@NotNull Project project, @NotNull PsiFile psiFile) {
        return isProjectFile(project, psiFile.getVirtualFile());
    }

}
