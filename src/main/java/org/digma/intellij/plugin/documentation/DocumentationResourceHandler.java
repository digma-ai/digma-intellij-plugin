package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.ui.jcef.*;
import org.jetbrains.annotations.*;

import java.io.InputStream;
import java.util.Collections;

public class DocumentationResourceHandler extends BaseResourceHandler {

    public DocumentationResourceHandler(@NotNull CefBrowser browser, @NotNull String path) {
        super(path, browser);
    }

    @Override
    public boolean isIndexHtml(@NotNull String path) {
        return path.toLowerCase().endsWith("index.html");
    }

    @Nullable
    @Override
    public InputStream buildIndexFromTemplate(@NotNull String path) {
        Project project = JBcefBrowserPropertiesKt.getProject(getBrowser());
        if (project == null) {
            Log.log(getLogger()::warn, "project is null , should never happen");
            ErrorReporter.getInstance().reportError(null, "DocumentationResourceHandler.buildIndexFromTemplate", "project is null", Collections.emptyMap());
            return null;
        }
        DocumentationVirtualFile file = (DocumentationVirtualFile) JBcefBrowserPropertiesKt.getProperty(getBrowser(), JBcefBrowserPropertiesKt.JCEF_DOCUMENTATION_FILE_PROPERTY_NAME);
        if (file == null) {
            Log.log(getLogger()::warn, "DocumentationVirtualFile is null , should never happen");
            ErrorReporter.getInstance().reportError(null, "DocumentationResourceHandler.buildIndexFromTemplate", "DocumentationVirtualFile is null", Collections.emptyMap());
            return null;
        }
        return new DocumentationIndexTemplateBuilder(file).build(project);
    }
}
