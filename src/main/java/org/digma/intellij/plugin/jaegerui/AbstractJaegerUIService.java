package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.apache.commons.collections.CollectionUtils;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.reload.*;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.jaegerui.model.Insight;
import org.digma.intellij.plugin.ui.model.TraceSample;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.digma.intellij.plugin.jaegerui.JaegerUIVirtualFile.JAEGER_UI_EDITOR_KEY;

//todo: move all code to org.digma.intellij.plugin.ui.jaegerui.JaegerUIService
public class AbstractJaegerUIService implements Disposable, ReloadableJCefContainer {

    protected final Logger logger = Logger.getInstance(getClass());

    protected final Project project;


    public AbstractJaegerUIService(Project project) {
        this.project = project;
        ApplicationManager.getApplication().getService(ReloadService.class).register(this, this);
    }

    @Override
    public void dispose() {
        //nothing to do, used as parent disposable
    }

    public void openEmbeddedJaeger(@NotNull String traceId, @NotNull String spanName, @Nullable String spanCodeObjectId, boolean sendSearchQuery) {

        Log.log(logger::trace, "openEmbeddedJaeger called, traceId {}, spanName {}", traceId, spanName);

        if (showExisting(traceId, spanName)) {
            return;
        }

        var jaegerQueryUrl = SettingsState.getInstance().getJaegerQueryUrl();
        if (jaegerQueryUrl == null || jaegerQueryUrl.isBlank()) {
            return;
        }

        EDT.ensureEDT(() -> {
            var file = JaegerUIVirtualFile.createVirtualFile(jaegerQueryUrl, traceId, spanName, spanCodeObjectId, sendSearchQuery);
            FileEditorManager.getInstance(project).openFile(file, true, true);
        });

    }


    public void openEmbeddedJaeger(@NotNull List<TraceSample> traceSamples, @NotNull String spanName, @Nullable String spanCodeObjectId, boolean sendSearchQuery) {

        Log.log(logger::trace, "openEmbeddedJaeger called, traceSamples {}, spanName {}", traceSamples, spanName);

        if (showExisting(traceSamples, spanName, spanCodeObjectId)) {
            return;
        }

        var jaegerQueryUrl = SettingsState.getInstance().getJaegerQueryUrl();
        if (jaegerQueryUrl == null || jaegerQueryUrl.isBlank()) {
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


    @NotNull
    protected Map<String, List<Insight>> getInsights(@NotNull List<String> spanIds) {

        if (spanIds.size() > 500) {
            return Collections.emptyMap();
        }

        var insightsMap = new HashMap<String, List<Insight>>();

        try {
            var spanInsights = AnalyticsService.getInstance(project).getInsightsForJaeger(spanIds);

            spanInsights.forEach(o -> {
                List<Insight> insights = o.getInsights().stream().map(s -> new Insight(s.getType(), s.getImportance())).toList();
                insightsMap.put(o.getSpanCodeObjectId(), insights);
            });
            return insightsMap;
        } catch (AnalyticsServiceException e) {
            Log.debugWithException(logger, e, "Exception in getInsights {}", e.getMessage());
            return Collections.emptyMap();
        }
    }


    @Override
    public void reload() {

        Log.log(logger::trace, "reload called, reloading all jaeger ui files");

        var files = Arrays.stream(FileEditorManager.getInstance(project).getOpenFiles()).filter(JaegerUIVirtualFile::isJaegerUIVirtualFile).toList();

        var newFiles = new ArrayList<JaegerUIVirtualFile>();

        files.forEach(oldFile -> {
            if (oldFile instanceof JaegerUIVirtualFile jaegerUIVirtualFile) {
                var newFile = new JaegerUIVirtualFile(jaegerUIVirtualFile.getName());
                newFile.setJaegerBaseUrl(jaegerUIVirtualFile.getJaegerBaseUrl());
                newFile.setTraceId(jaegerUIVirtualFile.getTraceId());
                newFile.setTraceSamples(jaegerUIVirtualFile.getTraceSamples());
                newFile.setSpanName(jaegerUIVirtualFile.getSpanName());
                newFile.setSpanCodeObjectId(jaegerUIVirtualFile.getSpanCodeObjectId());
                newFile.setSendSearchQuery(jaegerUIVirtualFile.isSendSearchQuery());
                JAEGER_UI_EDITOR_KEY.set(newFile, JaegerUIFileEditorProvider.JAEGER_UI_EDITOR_TYPE);
                newFiles.add(newFile);
            }
        });

        files.forEach(virtualFile -> {
            if (virtualFile instanceof JaegerUIVirtualFile jaegerUIVirtualFile) {
                jaegerUIVirtualFile.setValid(false);
                FileEditorManager.getInstance(project).closeFile(jaegerUIVirtualFile);
            }
        });

        newFiles.forEach(file -> FileEditorManager.getInstance(project).openFile(file, true, true));
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }
}
