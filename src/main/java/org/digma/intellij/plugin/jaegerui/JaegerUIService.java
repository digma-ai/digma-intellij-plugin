package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.apache.commons.collections.CollectionUtils;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.jaegerui.model.incoming.*;
import org.digma.intellij.plugin.jaegerui.model.outgoing.*;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator;
import org.digma.intellij.plugin.posthog.*;
import org.digma.intellij.plugin.psi.*;
import org.digma.intellij.plugin.scope.*;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.MainToolWindowCardsController;
import org.digma.intellij.plugin.ui.model.TraceSample;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.digma.intellij.plugin.common.CodeObjectsUtil.*;


public class JaegerUIService implements Disposable {

    private final Logger logger = Logger.getInstance(JaegerUIService.class);

    private final Project project;


    public JaegerUIService(Project project) {
        this.project = project;
    }

    @Override
    public void dispose() {
        //nothing to do, used as parent disposable
    }

    public static JaegerUIService getInstance(Project project) {
        return project.getService(JaegerUIService.class);
    }


    public void openEmbeddedJaeger(@NotNull String traceId, @NotNull String spanName, @Nullable String spanCodeObjectId, boolean sendSearchQuery) {

        if (showExisting(traceId, spanName)) {
            return;
        }

        var jaegerQueryUrl = SettingsState.getInstance().jaegerQueryUrl;
        if (jaegerQueryUrl.isBlank()) {
            return;
        }

        EDT.ensureEDT(() -> {
            var file = JaegerUIVirtualFile.createVirtualFile(jaegerQueryUrl, traceId, spanName, spanCodeObjectId, sendSearchQuery);
            FileEditorManager.getInstance(project).openFile(file, true, true);
        });

    }


    public void openEmbeddedJaeger(@NotNull List<TraceSample> traceSamples, @NotNull String spanName, @Nullable String spanCodeObjectId, boolean sendSearchQuery) {

        if (showExisting(traceSamples, spanName, spanCodeObjectId)) {
            return;
        }

        var jaegerQueryUrl = SettingsState.getInstance().jaegerQueryUrl;
        if (jaegerQueryUrl.isBlank()) {
            return;
        }

        if (traceSamples.isEmpty()) {
            return;
        }

        EDT.ensureEDT(() -> {
            var file = JaegerUIVirtualFile.createVirtualFile(jaegerQueryUrl, traceSamples, spanName, spanCodeObjectId, sendSearchQuery);
            FileEditorManager.getInstance(project).openFile(file, true, true);
        });

    }

    private boolean showExisting(List<TraceSample> traceSamples, String spanName, String spanCodeObjectId) {
        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
            var file = editor.getFile();
            if (file != null && JaegerUIVirtualFile.isJaegerUIVirtualFile(file)) {
                JaegerUIVirtualFile openFile = (JaegerUIVirtualFile) file;
                if (Objects.equals(openFile.getSpanName(), spanName) &&
                        Objects.equals(openFile.getSpanCodeObjectId(), spanCodeObjectId) &&
                        openFile.getTraceSamples() != null && CollectionUtils.isEqualCollection(openFile.getTraceSamples(), traceSamples)) {
                    EDT.ensureEDT(() -> FileEditorManager.getInstance(project).openFile(file, true, true));
                    return true;
                }
            }
        }
        return false;
    }


    private boolean showExisting(String traceId, String spanName) {
        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
            var file = editor.getFile();
            if (file != null && JaegerUIVirtualFile.isJaegerUIVirtualFile(file)) {
                JaegerUIVirtualFile openFile = (JaegerUIVirtualFile) file;
                if (Objects.equals(openFile.getSpanName(), spanName) && Objects.equals(openFile.getTraceId(), traceId)) {
                    EDT.ensureEDT(() -> FileEditorManager.getInstance(project).openFile(file, true, true));
                    return true;
                }
            }
        }
        return false;
    }


    public void navigateToCode(GoToSpanMessage goToSpanMessage) {

        Log.log(logger::debug, project, "goToSpan request {}", goToSpanMessage);

        var span = goToSpanMessage.payload();

        Log.log(logger::debug, project, "calling showInsightsForSpanOrMethodAndNavigateToCode from goToSpan for {}", span);

        ActivityMonitor.getInstance(project).registerSpanLinkClicked(MonitoredPanel.Jaeger);

        CodeNavigator.getInstance(project).maybeNavigateToSpanOrMethod(span.spanCodeObjectId(), span.methodCodeObjectId());

    }


    //show insight without navigating to source
    public void goToInsight(GoToSpanMessage goToSpanMessage) {

        Log.log(logger::trace, project, "goToInsight request {}", goToSpanMessage);

        var span = goToSpanMessage.payload();
        //if we're here then code location was not found
        ActivityMonitor.getInstance(project).registerSpanLinkClicked(MonitoredPanel.Jaeger);
        MainToolWindowCardsController.getInstance(project).closeAllNotificationsIfShowing();
        ScopeManager.getInstance(project).changeScope(new SpanScope(span.spanCodeObjectId()));
    }

    public Map<String, SpanData> getResolvedSpans(SpansMessage spansMessage) {

        //todo: change get insights with the new service

        var allSpans = new HashMap<String, SpanData>();

        var spanIds = spansMessage.payload().spans().stream()
                .map(Span::spanId).toList();
        var methodIds = spansMessage.payload().spans().stream()
                .map(Span::methodId)
                .filter(Objects::nonNull).toList();

        Map<String, List<Insight>> allInsights = getInsights(spanIds, methodIds);

        for (SupportedLanguages value : SupportedLanguages.values()) {
            var languageService = LanguageService.findLanguageServiceByName(project, value.getLanguageServiceClassName());
            if (languageService != null) {
                var spanWorkspaceUris = ReadActions.ensureReadAction(() -> languageService.findWorkspaceUrisForSpanIds(spanIds));
                var methodWorkspaceUris = ReadActions.ensureReadAction(() -> languageService.findWorkspaceUrisForMethodCodeObjectIds(methodIds));
                spansMessage.payload().spans().forEach(span -> {
                    var spanId = span.spanId();
                    var methodId = span.methodId();
                    var hasCodeLocation = (spanWorkspaceUris.containsKey(spanId) || methodWorkspaceUris.containsKey(methodId));

                    var spanData = allSpans.computeIfAbsent(span.id(), s -> new SpanData(hasCodeLocation, new ArrayList<>()));

                    addInsightsToSpanData(spanData, spanId, methodId, allInsights);
                });
            }
        }

        return allSpans;

    }

    private void addInsightsToSpanData(SpanData spanData, String spanId, String methodId, Map<String, List<Insight>> allInsights) {

        List<Insight> spanInsights = new ArrayList<>(spanData.insights());
        if (allInsights.get(spanId) != null) {
            spanInsights.addAll(allInsights.get(spanId));
        }
        if (allInsights.get(methodId) != null) {
            spanInsights.addAll(allInsights.get(methodId));
        }
        spanInsights = distinctByType(spanInsights);
        spanData.insights().clear();
        spanData.insights().addAll(spanInsights);
    }

    @NotNull
    private List<Insight> distinctByType(@NotNull List<Insight> spanInsights) {

        if (spanInsights.isEmpty()) {
            return spanInsights;
        }

        Map<String, List<Insight>> mappedByType = new HashMap<>();
        spanInsights.forEach(insight -> {
            var list = mappedByType.computeIfAbsent(insight.type(), s -> new ArrayList<>());
            list.add(insight);
        });
        List<Insight> insightList = new ArrayList<>();
        mappedByType.forEach((key, value) -> {
            var insightOptional = value.stream().reduce((insight1, insight2) -> (insight1.importance() < insight2.importance()) ? insight1 : insight2);
            insightOptional.ifPresent(insightList::add);
        });


        return insightList;
    }

    @NotNull
    private Map<String, List<Insight>> getInsights(@NotNull List<String> spanIds, @NotNull List<String> methodIds) {

        if (spanIds.size() > 500) {
            return Collections.emptyMap();
        }

        var ids = new ArrayList<>(addSpanTypeToIds(spanIds));
        ids.addAll(addMethodTypeToIds(methodIds));

        var insights = new HashMap<String, List<Insight>>();

        try {
            var insightsFromBackend = AnalyticsService.getInstance(project).getInsightsInfo(ids);
            insightsFromBackend.forEach(codeObjectInsight -> {
                var id = codeObjectInsight.getCodeObjectId();
                var objectInsights = insights.computeIfAbsent(id, s -> new ArrayList<>());
                objectInsights.add(new Insight(codeObjectInsight.getType(), codeObjectInsight.getImportance()));
            });
            return insights;
        } catch (AnalyticsServiceException e) {
            Log.debugWithException(logger, e, "Exception in getInsights {}", e.getMessage());
            return Collections.emptyMap();
        }
    }


}
