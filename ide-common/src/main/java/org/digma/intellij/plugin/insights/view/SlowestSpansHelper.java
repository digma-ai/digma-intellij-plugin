package org.digma.intellij.plugin.insights.view;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.document.CodeObjectsUtil;
import org.digma.intellij.plugin.model.rest.insights.SpanInfo;
import org.digma.intellij.plugin.project.ProjectService;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.ArrayList;
import java.util.List;

public class SlowestSpansHelper {

    public static void findWorkspaceUrisForSpans(Project project, ListViewItem<?> theListView, List<SpanInfo> spanInfos) {

        var spanIds = new ArrayList<String>();

        spanInfos.forEach(spanInfo -> {
            var spanId = CodeObjectsUtil.createSpanId(spanInfo.getInstrumentationLibrary(), spanInfo.getName());
            spanIds.add(spanId);
        });


        var workspaceUris = project.getService(ProjectService.class)
                .findWorkspaceUrisForSpanIds(spanIds);

        workspaceUris.forEach((k, v) -> theListView.getMoreData().put(k,v));

    }
}
