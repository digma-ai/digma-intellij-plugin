package org.digma.intellij.plugin.rider.project;

import com.intellij.openapi.project.Project;
import kotlin.Pair;
import org.digma.intellij.plugin.project.ProjectService;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;

import java.util.List;
import java.util.Map;

public class RiderProjectService implements ProjectService {

    private final CodeObjectHost codeObjectHost;

    public RiderProjectService(Project project) {
        codeObjectHost = project.getService(CodeObjectHost.class);
    }

    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds) {
        return codeObjectHost.findWorkspaceUrisForCodeObjectIds(codeObjectIds);
    }

    @Override
    public Map<String, Pair<String,Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds) {
        return codeObjectHost.findWorkspaceUrisForSpanIds(spanIds);
    }
}
