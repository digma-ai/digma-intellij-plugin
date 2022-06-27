package org.digma.intellij.plugin.insights.view;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.document.CodeObjectsUtil;
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight;
import org.digma.intellij.plugin.project.ProjectService;
import org.digma.intellij.plugin.ui.model.insights.InsightListViewItem;

import java.util.ArrayList;

class SlowestSpansHelper {

    static void findWorkspaceUrisForSpans(Project project, InsightListViewItem<?> theListView, SlowestSpansInsight insight) {

        var spanIds = new ArrayList<String>();

        insight.getSpans().forEach(slowSpanInfo -> {
            var spanId = CodeObjectsUtil.createSpanId(slowSpanInfo.getSpanInfo().getInstrumentationLibrary(),
                    slowSpanInfo.getSpanInfo().getName());
            spanIds.add(spanId);
        });


        var workspaceUris = project.getService(ProjectService.class)
                .findWorkspaceUrisForSpanIds(spanIds);

        workspaceUris.forEach((k, v) -> theListView.getMoreData().put(k,v));

    }
}
