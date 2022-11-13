package org.digma.intellij.plugin.insights.view;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.document.CodeObjectsUtil;
import org.digma.intellij.plugin.model.rest.insights.SpanInfo;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SlowestSpansHelper {

    private SlowestSpansHelper() {
    }

    public static void findWorkspaceUrisForSpans(Project project, ListViewItem<?> theListView, @NotNull List<SpanInfo> spanInfos, @Nullable String methodCodeObjectId) {

        var spanIds = new ArrayList<String>();

        spanInfos.forEach(spanInfo -> {
            var spanId = CodeObjectsUtil.createSpanId(spanInfo.getInstrumentationLibrary(), spanInfo.getName());
            spanIds.add(spanId);
        });

        //when this method is called there is not always a related file.
        //if called while building SlowestSpansInsight then there is a method id and the related file is probably opened
        //and DocumentInfoService should find the method info and by that the language.
        //but if called from SummariesProvider there is no related file and probably no method id.
        var languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodCodeObjectId);

        var workspaceUris = languageService.findWorkspaceUrisForSpanIds(spanIds);

        workspaceUris.forEach((k, v) -> theListView.getMoreData().put(k, v));

    }
}
