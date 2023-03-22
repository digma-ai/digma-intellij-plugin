package org.digma.intellij.plugin.insights.view;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WorkspaceUrisHelper {

    private WorkspaceUrisHelper() {
    }

    public static void findWorkspaceUrisForSpans(@NotNull Project project, ListViewItem<?> theListView, @NotNull List<String> spanIds, @NotNull String methodCodeObjectId) {
        var languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodCodeObjectId);
        findWorkspaceUrisForSpans(theListView, spanIds, languageService);
    }

    public static void findWorkspaceUrisForSpans(@NotNull Project project, ListViewItem<?> theListView, @NotNull List<String> spanIds, @NotNull MethodInfo methodInfo) {
        var languageService = LanguageService.findLanguageServiceByMethodInfo(project, methodInfo);
        findWorkspaceUrisForSpans(theListView, spanIds, languageService);
    }

    public static void findWorkspaceUrisForMethodCodeObjectIds(@NotNull Project project, ListViewItem<?> theListView, @NotNull List<String> codeObjectIds, @NotNull MethodInfo methodInfo) {
        var languageService = LanguageService.findLanguageServiceByMethodInfo(project, methodInfo);
        findWorkspaceUrisMethodCodeObjectIds(theListView, codeObjectIds, languageService);
    }

    private static void findWorkspaceUrisForSpans(ListViewItem<?> theListView, @NotNull List<String> spanIds, @NotNull LanguageService languageService) {
        var workspaceUris = languageService.findWorkspaceUrisForSpanIds(spanIds);
        workspaceUris.forEach((k, v) -> theListView.getMoreData().put(k, v));
    }

    private static void findWorkspaceUrisMethodCodeObjectIds(ListViewItem<?> theListView, @NotNull List<String> codeObjectIds, @NotNull LanguageService languageService) {
        var workspaceUris = languageService.findWorkspaceUrisForMethodCodeObjectIds(codeObjectIds);
        workspaceUris.forEach((k, v) -> theListView.getMoreData().put(k, v));
    }
}
