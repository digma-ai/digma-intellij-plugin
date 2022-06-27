package org.digma.intellij.plugin.project;

import java.util.List;
import java.util.Map;

public interface ProjectService {


    Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds);
}
