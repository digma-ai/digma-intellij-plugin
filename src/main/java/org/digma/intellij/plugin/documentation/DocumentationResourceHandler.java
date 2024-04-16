package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.ui.jcef.BaseResourceHandler;
import org.jetbrains.annotations.*;

import java.io.InputStream;

public class DocumentationResourceHandler extends BaseResourceHandler {

    private static final Logger LOGGER = Logger.getInstance(DocumentationResourceHandler.class);


    private final Project project;
    private final DocumentationVirtualFile documentationVirtualFile;

    public DocumentationResourceHandler(@NotNull Project project, @NotNull String path, @NotNull DocumentationVirtualFile file) {
        super(path);
        this.project = project;
        this.documentationVirtualFile = file;
    }

    @Override
    public boolean isIndexHtml(@NotNull String path) {
        return path.toLowerCase().endsWith("index.html");
    }

    @Nullable
    @Override
    public InputStream buildIndexFromTemplate(@NotNull String path) {
        return new DocumentationIndexTemplateBuilder(documentationVirtualFile).build(project);
    }
}
