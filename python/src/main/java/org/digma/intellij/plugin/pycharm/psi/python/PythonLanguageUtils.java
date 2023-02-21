package org.digma.intellij.plugin.pycharm.psi.python;

import com.intellij.openapi.project.Project;
import com.jetbrains.python.psi.PyFunction;
import org.jetbrains.annotations.NotNull;

public class PythonLanguageUtils {

    @NotNull
    public static String createPythonMethodCodeObjectId(@NotNull Project project, @NotNull PyFunction pyFunction) {
        var name = pyFunction.getName();
        var filePath = buildFilePath(project,pyFunction);
        return filePath + "$_$" + name;
    }


    private static String buildFilePath(@NotNull Project project, @NotNull PyFunction pyFunction) {
        var path = pyFunction.getContainingFile().getVirtualFile().getPath();
        var index = path.lastIndexOf(project.getName());
        return path.substring(index);
    }


    public static String extractFunctionNameFromCodeObjectId(@NotNull String methodId) {
        return methodId.substring(methodId.lastIndexOf("$_$") + 3);
    }
}
