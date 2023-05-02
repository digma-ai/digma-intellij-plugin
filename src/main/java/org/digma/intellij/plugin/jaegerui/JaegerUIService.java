package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import kotlin.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.common.ReadActions;
import org.digma.intellij.plugin.jaegerui.model.GoToSpanMessage;
import org.digma.intellij.plugin.jaegerui.model.Importance;
import org.digma.intellij.plugin.jaegerui.model.SpansMessage;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.SupportedLanguages;
import org.digma.intellij.plugin.service.EditorService;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.model.TraceSample;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JaegerUIService {

    private static final Logger LOGGER = Logger.getInstance(JaegerUIService.class);

    private final Project project;

    //Configuration should be singleton
    private final Configuration freemarketConfiguration = new Configuration(Configuration.VERSION_2_3_30);

    private static final String INDEX_TEMPLATE_NAME = "jaegeruitemplate.ftl";

    private static final String INITIAL_ROUTE_PARAM_NAME = "initial_route";
    private static final String JAEGER_URL_PARAM_NAME = "jaeger_url";



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

        if (!isIndexHtml(path)){
            //should not happen
            return null;
        }

        try {
            var data = new HashMap<String, String>();
            data.put(JAEGER_URL_PARAM_NAME, jaegerUIVirtualFile.getJaegerBaseUrl());

            if (jaegerUIVirtualFile.getTraceId() != null){
                var initialRoutePath = buildInitialRoutePath(jaegerUIVirtualFile.getTraceId());
                data.put(INITIAL_ROUTE_PARAM_NAME,initialRoutePath);
            }else if(jaegerUIVirtualFile.getTraceSamples() != null && !jaegerUIVirtualFile.getTraceSamples().isEmpty()){

                if (jaegerUIVirtualFile.getTraceSamples().size() == 1 &&
                        jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId() != null &&
                        !Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId()).isBlank()){

                    var initialRoutePath = buildInitialRoutePath(Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId()));
                    data.put(INITIAL_ROUTE_PARAM_NAME,initialRoutePath);

                }else if (jaegerUIVirtualFile.getTraceSamples().size() == 2 &&
                        jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId() != null &&
                        !Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId()).isBlank() &&
                        jaegerUIVirtualFile.getTraceSamples().get(1).getTraceId() != null &&
                        !Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(1).getTraceId()).isBlank()){

                    var trace1 = Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId()).toLowerCase();
                    var trace2 = Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(1).getTraceId()).toLowerCase();
                    var initialRoutePath = "/trace/"+trace1+"..."+trace2+"?cohort="+trace1+"&cohort="+trace2;
                    data.put(INITIAL_ROUTE_PARAM_NAME,initialRoutePath);
                }
            }
            else{
                data.put(INITIAL_ROUTE_PARAM_NAME,"");
            }


            Template template = freemarketConfiguration.getTemplate(INDEX_TEMPLATE_NAME);
            StringWriter stringWriter = new StringWriter();
            template.process(data, stringWriter);
            return new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            Log.debugWithException(LOGGER,e,"error creating template for index.html");
            return null;
        }
    }

    private String buildInitialRoutePath(String traceId) {
        var traceLowerCase = traceId.toLowerCase();
        return  "/trace/"+traceLowerCase+"?cohort="+traceLowerCase;
    }


    public void openEmbeddedJaeger(@NotNull String traceId, @NotNull String spanName) {

        if (showExisting(traceId,spanName)){
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

        if (showExisting(traceSamples,spanName)){
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
            var file = JaegerUIVirtualFile.createVirtualFile(jaegerQueryUrl, traceSamples,spanName);
            FileEditorManager.getInstance(project).openFile(file, true, true);
        });

    }

    private boolean showExisting(List<TraceSample> traceSamples, String spanName) {
        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
            var file = editor.getFile();
            if (file != null && JaegerUIVirtualFile.isJaegerUIVirtualFile(file)) {
                JaegerUIVirtualFile openFile = (JaegerUIVirtualFile) file;
                if (Objects.equals(openFile.getSpanName(),spanName) &&
                        openFile.getTraceSamples() != null &&
                        CollectionUtils.isEqualCollection(openFile.getTraceSamples(),traceSamples)) {
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
                if (Objects.equals(openFile.getSpanName(),spanName) && Objects.equals(openFile.getTraceId(),traceId)) {
                    EDT.ensureEDT(() -> FileEditorManager.getInstance(project).openFile(file, true, true));
                    return true;
                }
            }
        }
        return false;
    }


    public void goToSpan(GoToSpanMessage goToSpanMessage) {

        var span = goToSpanMessage.payload();

        for (SupportedLanguages value : SupportedLanguages.values()) {
            var languageService = LanguageService.findLanguageServiceByName(project,value.getLanguageServiceClassName());
            if (languageService != null){
                var spanId = span.instrumentationLibrary() + "$_$" + span.name();
                var spanWorkspaceUris = ReadActions.ensureReadAction(() -> languageService.findWorkspaceUrisForSpanIds(Collections.singletonList(spanId)));

                if (spanWorkspaceUris.containsKey(spanId)) {
                    Pair<String, Integer> location = spanWorkspaceUris.get(spanId);
                    EditorService editorService = project.getService(EditorService.class);
                    EDT.ensureEDT(() -> editorService.openWorkspaceFileInEditor(location.getFirst(), location.getSecond()));
                    return;
                }else if (span.function() != null && span.namespace() != null){
                    var methodId = span.namespace() + "$_$" + span.function();
                    var methodWorkspaceUris = ReadActions.ensureReadAction(() -> languageService.findWorkspaceUrisForMethodCodeObjectIds(Collections.singletonList(methodId)));

                    if (methodWorkspaceUris.containsKey(methodId)){
                        Pair<String, Integer> location = methodWorkspaceUris.get(methodId);
                        EditorService editorService = project.getService(EditorService.class);
                        EDT.ensureEDT(() -> editorService.openWorkspaceFileInEditor(location.getFirst(), location.getSecond()));
                        return;
                    }
                }
            }
        }
    }

    public Map<String, Importance> getResolvedSpans(SpansMessage spansMessage) {

        var resolvedSpans = new HashMap<String, Importance>();

        var spanIds = spansMessage.payload().spans().stream()
                .map(span -> span.instrumentationLibrary() + "$_$" + span.name()).toList();
        var methodIds = spansMessage.payload().spans().stream()
                .filter(span -> span.function() != null && span.namespace() != null)
                .map(span -> span.namespace() + "$_$" + span.function()).toList();

        Map<String,Integer> importanceMap = getImportance(spanIds);

        for (SupportedLanguages value : SupportedLanguages.values()) {
            var languageService = LanguageService.findLanguageServiceByName(project,value.getLanguageServiceClassName());
            if (languageService != null){
                var spanWorkspaceUris = languageService.findWorkspaceUrisForSpanIds(spanIds);
                var methodWorkspaceUris = languageService.findWorkspaceUrisForMethodCodeObjectIds(methodIds);
                spansMessage.payload().spans().forEach(span -> {
                    var spanId = span.instrumentationLibrary() + "$_$" + span.name();
                    var methodId = (span.function() == null || span.namespace() == null) ? "" : span.namespace() + "$_$" + span.function();
                    if (spanWorkspaceUris.containsKey(spanId)) {
                        resolvedSpans.put(span.id(), new Importance(importanceMap.getOrDefault(spanId, 9)));
                    }else if(methodWorkspaceUris.containsKey(methodId)){
                        resolvedSpans.put(span.id(), new Importance(importanceMap.getOrDefault(spanId, 9)));
                    }
                });
            }
        }

        return resolvedSpans;

    }

    private Map<String, Integer> getImportance(List<String> spanIds) {

        if (spanIds.size() > 500){
            return Collections.emptyMap();
        }

        var importance = new HashMap<String,Integer>();

        try {
            var insights = AnalyticsService.getInstance(project).getInsights(spanIds.stream().map(s -> "span:"+s).toList());
            insights.forEach(codeObjectInsight -> {
                var spanId = codeObjectInsight.getCodeObjectId();
                importance.put(spanId,codeObjectInsight.getImportance());
            });
            return importance;
        } catch (AnalyticsServiceException e) {
            Log.debugWithException(LOGGER,e,"Exception in getInsights {}",e.getMessage());
            return Collections.emptyMap();
        }
    }
}


