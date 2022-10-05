package org.digma.intellij.plugin.insights.view;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.document.CodeObjectsUtil;
import org.digma.intellij.plugin.model.rest.insights.SpanInfo;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.ArrayList;
import java.util.List;

public class SlowestSpansHelper {

    private SlowestSpansHelper() {
    }

    public static void findWorkspaceUrisForSpans(Project project, ListViewItem<?> theListView, List<SpanInfo> spanInfos) {

        var spanIds = new ArrayList<String>();

        spanInfos.forEach(spanInfo -> {
            var spanId = CodeObjectsUtil.createSpanId(spanInfo.getInstrumentationLibrary(), spanInfo.getName());
            spanIds.add(spanId);
        });


        LanguageService languageService = project.getService(LanguageServiceLocator.class).getDominantLanguageService();
        var workspaceUris = languageService.findWorkspaceUrisForSpanIds(spanIds);

        workspaceUris.forEach((k, v) -> theListView.getMoreData().put(k, v));

    }
}
