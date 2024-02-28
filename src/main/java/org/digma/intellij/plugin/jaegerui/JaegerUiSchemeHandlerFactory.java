package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.project.Project;
import org.cef.handler.CefResourceHandler;
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class JaegerUiSchemeHandlerFactory extends BaseSchemeHandlerFactory {

    private final Project project;
    private final JaegerUIVirtualFile file;

    public JaegerUiSchemeHandlerFactory(Project project, JaegerUIVirtualFile file) {
        this.project = project;
        this.file = file;
    }


    @NotNull
    @Override
    public CefResourceHandler createResourceHandler(@NotNull String resourceName, boolean resourceExists) {
        if (resourceExists) {
            return new JaegerUiResourceHandler(project, resourceName, file);
        } else {
            return new JaegerUiResourceHandler(project, JaegerUIConstants.JAEGER_UI_RESOURCE_FOLDER_NAME + "/index.html", file);
        }
    }

    @NotNull
    @Override
    public String getSchema() {
        return "https";
    }

    @NotNull
    @Override
    public String getDomain() {
        return JaegerUIConstants.JAEGER_UI_DOMAIN_NAME;
    }

    @NotNull
    @Override
    public String getResourceFolderName() {
        return JaegerUIConstants.JAEGER_UI_RESOURCE_FOLDER_NAME;
    }
}
