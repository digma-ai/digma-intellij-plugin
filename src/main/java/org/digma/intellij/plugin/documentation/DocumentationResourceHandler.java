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

    @NotNull
    @Override
    public String getResourceFolderName() {
        return DocumentationConstants.DOCUMENTATION_RESOURCE_FOLDER_NAME;
    }

    @Nullable
    @Override
    public InputStream buildEnvJsFromTemplate(@NotNull String path) {
        Project project = JBcefBrowserPropertiesKt.getProject(getBrowser());
        if (project == null) {
            Log.log(getLogger()::warn, "project is null , should never happen");
            ErrorReporter.getInstance().reportError(null, "DocumentationResourceHandler.buildEnvJsFromTemplate", "project is null", Collections.emptyMap());
            return null;
        }
        DocumentationVirtualFile file = (DocumentationVirtualFile) JBcefBrowserPropertiesKt.getProperty(getBrowser(), JBcefBrowserPropertiesKt.JCEF_DOCUMENTATION_FILE_PROPERTY_NAME);
        if (file == null) {
            Log.log(getLogger()::warn, "DocumentationVirtualFile is null , should never happen");
            ErrorReporter.getInstance().reportError(null, "DocumentationResourceHandler.buildEnvJsFromTemplate", "DocumentationVirtualFile is null", Collections.emptyMap());
            return null;
        }
        return new DocumentationEnvJsTemplateBuilder(file,path).build(project);
    }
}
