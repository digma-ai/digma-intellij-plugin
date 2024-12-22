package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.ui.jcef.*;
import org.jetbrains.annotations.*;

import java.io.InputStream;
import java.util.Collections;

public class JaegerUiResourceHandler extends BaseResourceHandler {

    public JaegerUiResourceHandler(@NotNull CefBrowser browser, @NotNull String path) {
        super(path, browser);
    }

    @NotNull
    @Override
    public String getResourceFolderName() {
        return JaegerUIConstants.JAEGER_UI_RESOURCE_FOLDER_NAME;
    }

    @Nullable
    @Override
    public InputStream buildEnvJsFromTemplate(@NotNull String path) {
        Project project = JBcefBrowserPropertiesKt.getProject(getBrowser());
        if (project == null) {
            Log.log(getLogger()::warn, "project is null , should never happen");
            ErrorReporter.getInstance().reportError(null, "JaegerUiResourceHandler.buildEnvJsFromTemplate", "project is null", Collections.emptyMap());
            return null;
        }
        JaegerUIVirtualFile file = (JaegerUIVirtualFile) JBcefBrowserPropertiesKt.getProperty(getBrowser(), JBcefBrowserPropertiesKt.JCEF_JAEGER_UI_FILE_PROPERTY_NAME);
        if (file == null) {
            Log.log(getLogger()::warn, "JaegerUIVirtualFile is null , should never happen");
            ErrorReporter.getInstance().reportError(null, "JaegerUiResourceHandler.buildEnvJsFromTemplate", "JaegerUIVirtualFile is null", Collections.emptyMap());
            return null;
        }
        return new JaegerUiEnvJsTemplateBuilder(file, path).build(project);
    }
}
