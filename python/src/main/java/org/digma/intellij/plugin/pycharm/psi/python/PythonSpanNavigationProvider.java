package org.digma.intellij.plugin.pycharm.psi.python;

import com.intellij.openapi.project.Project;
import kotlin.Pair;

import java.util.List;
import java.util.Map;

public class PythonSpanNavigationProvider {
    public static PythonSpanNavigationProvider getInstance(Project project) {
        return project.getService(PythonSpanNavigationProvider.class);
    }

    public Map<String, Pair<String, Integer>> getUrisForSpanIds(List<String> spanIds) {
        return null;
    }
}
