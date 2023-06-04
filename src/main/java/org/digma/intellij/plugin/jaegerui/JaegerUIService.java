package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.collections.CollectionUtils;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.common.ReadActions;
import org.digma.intellij.plugin.jaegerui.model.incoming.GoToSpanMessage;
import org.digma.intellij.plugin.jaegerui.model.incoming.Span;
import org.digma.intellij.plugin.jaegerui.model.incoming.SpansMessage;
import org.digma.intellij.plugin.jaegerui.model.outgoing.Insight;
import org.digma.intellij.plugin.jaegerui.model.outgoing.SpanData;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.navigation.codeless.CodelessNavigationKt;
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.SupportedLanguages;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.model.TraceSample;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.digma.intellij.plugin.document.CodeObjectsUtil.addMethodTypeToIds;
import static org.digma.intellij.plugin.document.CodeObjectsUtil.addSpanTypeToIds;


public class JaegerUIService {

    private final Logger LOGGER = Logger.getInstance(JaegerUIService.class);

    private final Project project;

    //Configuration should be singleton
    private final Configuration freemarketConfiguration = new Configuration(Configuration.VERSION_2_3_30);

    private static final String INDEX_TEMPLATE_NAME = "jaegeruitemplate.ftl";

    private static final String INITIAL_ROUTE_PARAM_NAME = "initial_route";
    private static final String JAEGER_URL_PARAM_NAME = "jaeger_url";
    private static final String JAEGER_QUERY_URL_CHANGED_FROM_DEFAULT_PARAM_NAME = "isUserChangedJaegerQueryUrl";


    public JaegerUIService(Project project) {
        this.project = project;
        freemarketConfiguration.setClassForTemplateLoading(this.getClass(), JaegerUIFileEditor.RESOURCE_FOLDER_NAME);
        freemarketConfiguration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarketConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarketConfiguration.setNumberFormat("computer");
    }


    public static JaegerUIService getInstance(Project project) {
        return project.getService(JaegerUIService.class);
    }

    boolean isIndexHtml(String path) {
        return path.endsWith("index.html");
    }

    public InputStream buildIndexFromTemplate(String path, JaegerUIVirtualFile jaegerUIVirtualFile) {

        if (!isIndexHtml(path)) {
            //should not happen
            return null;
        }

        try {

            var didUserChangeJaegerQueryUrl = !(SettingsState.DEFAULT_JAEGER_QUERY_URL.equalsIgnoreCase(SettingsState.getInstance().jaegerQueryUrl));

            var data = new HashMap<String, String>();
            data.put(JAEGER_URL_PARAM_NAME, jaegerUIVirtualFile.getJaegerBaseUrl());
            data.put(JAEGER_QUERY_URL_CHANGED_FROM_DEFAULT_PARAM_NAME, String.valueOf(didUserChangeJaegerQueryUrl));


            if (jaegerUIVirtualFile.getTraceId() != null) {
                var initialRoutePath = buildInitialRoutePath(jaegerUIVirtualFile.getTraceId());
                data.put(INITIAL_ROUTE_PARAM_NAME, initialRoutePath);
            } else if (jaegerUIVirtualFile.getTraceSamples() != null && !jaegerUIVirtualFile.getTraceSamples().isEmpty()) {

                if (jaegerUIVirtualFile.getTraceSamples().size() == 1 &&
                        jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId() != null &&
                        !Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId()).isBlank()) {

                    var initialRoutePath = buildInitialRoutePath(Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId()));
                    data.put(INITIAL_ROUTE_PARAM_NAME, initialRoutePath);

                } else if (jaegerUIVirtualFile.getTraceSamples().size() == 2 &&
                        jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId() != null &&
                        !Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId()).isBlank() &&
                        jaegerUIVirtualFile.getTraceSamples().get(1).getTraceId() != null &&
                        !Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(1).getTraceId()).isBlank()) {

                    var trace1 = Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId()).toLowerCase();
                    var trace2 = Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(1).getTraceId()).toLowerCase();
                    var initialRoutePath = "/trace/" + trace1 + "..." + trace2 + "?cohort=" + trace1 + "&cohort=" + trace2;
                    data.put(INITIAL_ROUTE_PARAM_NAME, initialRoutePath);
                }
            } else {
                data.put(INITIAL_ROUTE_PARAM_NAME, "");
            }


            Template template = freemarketConfiguration.getTemplate(INDEX_TEMPLATE_NAME);
            StringWriter stringWriter = new StringWriter();
            template.process(data, stringWriter);
            return new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            Log.debugWithException(LOGGER, e, "error creating template for index.html");
            return null;
        }
    }

    private String buildInitialRoutePath(String traceId) {
        var traceLowerCase = traceId.toLowerCase();
        return "/trace/" + traceLowerCase + "?cohort=" + traceLowerCase;
    }


    public void openEmbeddedJaeger(@NotNull String traceId, @NotNull String spanName) {

        if (showExisting(traceId, spanName)) {
            return;
        }

        var jaegerQueryUrl = SettingsState.getInstance().jaegerQueryUrl;
        if (jaegerQueryUrl.isBlank()) {
            return;
        }

        EDT.ensureEDT(() -> {
            var file = JaegerUIVirtualFile.createVirtualFile(jaegerQueryUrl, traceId, spanName);
            FileEditorManager.getInstance(project).openFile(file, true, true);
        });

    }


    public void openEmbeddedJaeger(@NotNull List<TraceSample> traceSamples, @NotNull String spanName) {

        if (showExisting(traceSamples, spanName)) {
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
            var file = JaegerUIVirtualFile.createVirtualFile(jaegerQueryUrl, traceSamples, spanName);
            FileEditorManager.getInstance(project).openFile(file, true, true);
        });

    }

    private boolean showExisting(List<TraceSample> traceSamples, String spanName) {
        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
            var file = editor.getFile();
            if (file != null && JaegerUIVirtualFile.isJaegerUIVirtualFile(file)) {
                JaegerUIVirtualFile openFile = (JaegerUIVirtualFile) file;
                if (Objects.equals(openFile.getSpanName(), spanName) &&
                        openFile.getTraceSamples() != null &&
                        CollectionUtils.isEqualCollection(openFile.getTraceSamples(), traceSamples)) {
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


    public void goToSpan(GoToSpanMessage goToSpanMessage) {

        Log.log(LOGGER::debug, project, "goToSpan request {}", goToSpanMessage);

        var span = goToSpanMessage.payload();

        Log.log(LOGGER::debug, project, "calling CodeNavigator.maybeNavigateToSpan from goToSpan for {}", span);

        if (project.getService(CodeNavigator.class).maybeNavigateToSpan(span.spanId(), span.methodId())) {
            Log.log(LOGGER::debug, project, "CodeNavigator.maybeNavigateToSpan did navigate to span {}", span);
        } else {
            Log.log(LOGGER::warn, project, "could not find code location in goToSpan for {}", goToSpanMessage);
        }
    }


    public void goToInsight(GoToSpanMessage goToSpanMessage) {

        Log.log(LOGGER::debug, project, "goToInsight request {}", goToSpanMessage);

        var span = goToSpanMessage.payload();
        //if we're here then code location was not found
        CodelessNavigationKt.showInsightsForSpan(project, span.spanId(), span.methodId());
    }

    public Map<String, SpanData> getResolvedSpans(SpansMessage spansMessage) {

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
                    var spanId = span.id();
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

        List<Insight> spanInsights = new ArrayList<>();
        if (allInsights.get(spanId) != null) {
            spanInsights.addAll(allInsights.get(spanId));
        }
        if (allInsights.get(methodId) != null) {
            spanInsights.addAll(allInsights.get(methodId));
        }
        spanInsights = distinctByType(spanInsights);
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
            var insightsFromBackend = AnalyticsService.getInstance(project).getInsights(ids).
                    stream().filter(codeObjectInsight -> codeObjectInsight.getType() != InsightType.Unmapped);
            insightsFromBackend.forEach(codeObjectInsight -> {
                var id = codeObjectInsight.getCodeObjectId();
                var objectInsights = insights.computeIfAbsent(id, s -> new ArrayList<>());
                objectInsights.add(new Insight(codeObjectInsight.getType().name(), codeObjectInsight.getImportance()));
            });
            return insights;
        } catch (AnalyticsServiceException e) {
            Log.debugWithException(LOGGER, e, "Exception in getInsights {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
