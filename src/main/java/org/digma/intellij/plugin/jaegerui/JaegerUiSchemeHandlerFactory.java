package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.project.Project;
import org.cef.handler.CefResourceHandler;
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory;
import org.jetbrains.annotations.NotNull;

import static org.digma.intellij.plugin.jaegerui.JaegerUIConstants.JAEGER_UI_SCHEMA;

public class JaegerUiSchemeHandlerFactory extends BaseSchemeHandlerFactory {

    private final JaegerUIVirtualFile file;

    public JaegerUiSchemeHandlerFactory(Project project, JaegerUIVirtualFile file) {
        super(project);
        this.file = file;
    }


    @NotNull
    @Override
    public CefResourceHandler createResourceHandler(@NotNull String resourceName, boolean resourceExists) {
        if (resourceExists) {
            return new JaegerUiResourceHandler(getProject(), resourceName, file);
        } else {
            return new JaegerUiResourceHandler(getProject(), JaegerUIConstants.JAEGER_UI_RESOURCE_FOLDER_NAME + "/index.html", file);
        }
    }

    @NotNull
    @Override
    public String getSchema() {
        return JAEGER_UI_SCHEMA;
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
