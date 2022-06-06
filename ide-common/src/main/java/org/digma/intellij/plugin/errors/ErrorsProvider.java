package org.digma.intellij.plugin.errors;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class ErrorsProvider {

    private static final Logger LOGGER = Logger.getInstance(ErrorsProvider.class);

    private final AnalyticsService analyticsService;
    private final DocumentInfoService documentInfoService;


    public ErrorsProvider(Project project) {
        analyticsService = project.getService(AnalyticsService.class);
        documentInfoService = project.getService(DocumentInfoService.class);
    }

    public ErrorsListContainer getErrors(@NotNull MethodInfo methodInfo) {
        final List<CodeObjectError> codeObjectErrors = analyticsService.getErrorsOfCodeObject(methodInfo.getId());
        Log.log(LOGGER::debug, "CodeObjectErrors for {}: {}", methodInfo, codeObjectErrors);

        final List<ListViewItem<CodeObjectError>> lviList = codeObjectErrors
                .stream()
                .map(x -> new ListViewItem<CodeObjectError>(x, 1))
                .collect(Collectors.toList());

        return new ErrorsListContainer(lviList);
    }
}
