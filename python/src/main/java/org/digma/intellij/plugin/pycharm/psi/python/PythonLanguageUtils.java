package org.digma.intellij.plugin.pycharm.psi.python;

import com.intellij.openapi.project.Project;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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


    public static String createSpanId(@NotNull String instLibrary,@NotNull String spanName){
        return instLibrary + "$_$" + spanName;
    }

    public static String extractFunctionNameFromCodeObjectId(@NotNull String methodId) {
        return methodId.substring(methodId.lastIndexOf("$_$") + 3);
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


}
