package org.digma.intellij.plugin.project;

import kotlin.Pair;

import java.util.List;
import java.util.Map;

public interface ProjectService {


    Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds);
    Map<String, Pair<String,Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds);
}
