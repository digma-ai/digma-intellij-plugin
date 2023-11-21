package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.digma.intellij.plugin.common.DigmaVirtualFileMarker;
import org.digma.intellij.plugin.ui.model.TraceSample;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JaegerUIVirtualFile extends LightVirtualFile implements DigmaVirtualFileMarker {

    public static final Key<String> JAEGER_UI_EDITOR_KEY = Key.create("Digma.JAEGER_UI_EDITOR_KEY");
    private String jaegerBaseUrl;
    private String traceId;
    private List<TraceSample> traceSamples;
    private String spanName;
    private String spanCodeObjectId;

    public JaegerUIVirtualFile(String myTitle) {
        super(myTitle);
        setFileType(JaegerUIFileType.INSTANCE);
        setWritable(false);
        putUserData(FileEditorManagerImpl.FORBID_PREVIEW_TAB, true);
    }

    public static boolean isJaegerUIVirtualFile(@NotNull VirtualFile file) {
        return file instanceof JaegerUIVirtualFile;
    }

    @NotNull
    public static VirtualFile createVirtualFile(@NotNull String jaegerBaseUrl, @NotNull String traceId, String spanName, @Nullable String spanCodeObjectId) {
        var file = new JaegerUIVirtualFile(spanName);
        file.setJaegerBaseUrl(jaegerBaseUrl);
        file.setTraceId(traceId);
        file.setSpanName(spanName);
        file.setSpanCodeObjectId(spanCodeObjectId);
        JAEGER_UI_EDITOR_KEY.set(file, JaegerUIFileEditorProvider.JAEGER_UI_EDITOR_TYPE);
        return file;
    }


    public static VirtualFile createVirtualFile(String jaegerBaseUrl, @NotNull List<TraceSample> traceSamples, @NotNull String spanName, @Nullable String spanCodeObjectId) {
        var file = new JaegerUIVirtualFile(spanName);
        file.setJaegerBaseUrl(jaegerBaseUrl);
        file.setTraceSamples(traceSamples);
        file.setSpanName(spanName);
        file.setSpanCodeObjectId(spanCodeObjectId);
        JAEGER_UI_EDITOR_KEY.set(file, JaegerUIFileEditorProvider.JAEGER_UI_EDITOR_TYPE);
        return file;
    }

    private void setTraceSamples(List<TraceSample> traceSamples) {
        this.traceSamples = traceSamples;
    }

    private void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    private void setJaegerBaseUrl(String jaegerBaseUrl) {
        this.jaegerBaseUrl = jaegerBaseUrl;
    }

    public String getJaegerBaseUrl() {
        return jaegerBaseUrl;
    }

    public String getTraceId() {
        return traceId;
    }

    public List<TraceSample> getTraceSamples() {
        return traceSamples;
    }

    private void setSpanName(String spanName) {
        this.spanName = spanName;
    }

    public String getSpanName() {
        return spanName;
    }

    @Nullable
    public String getSpanCodeObjectId() {
        return spanCodeObjectId;
    }

    public void setSpanCodeObjectId(@Nullable String spanCodeObjectId) {
        this.spanCodeObjectId = spanCodeObjectId;
    }
}
