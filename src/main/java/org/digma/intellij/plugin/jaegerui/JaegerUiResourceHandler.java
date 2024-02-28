package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.ui.jcef.BaseResourceHandler;
import org.jetbrains.annotations.*;

import java.io.InputStream;

public class JaegerUiResourceHandler extends BaseResourceHandler {

    private final Project project;
    private final JaegerUIVirtualFile file;

    public JaegerUiResourceHandler(Project project, @NotNull String path, JaegerUIVirtualFile file) {
        super(path);
        this.project = project;
        this.file = file;
    }

    @Override
    public boolean isIndexHtml(@NotNull String path) {
        return path.toLowerCase().endsWith("index.html");
    }

    @Nullable
    @Override
    public InputStream buildIndexFromTemplate(@NotNull String path) {
        return new JaegerUiIndexTemplateBuilder(file).build(project);
    }
}
