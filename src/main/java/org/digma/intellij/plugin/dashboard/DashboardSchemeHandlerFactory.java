package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.project.Project;
import org.cef.browser.*;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;
import org.digma.intellij.plugin.updates.ui.UIResourcesService;
import org.jetbrains.annotations.Nullable;

import java.net.*;


public class DashboardSchemeHandlerFactory implements CefSchemeHandlerFactory {
    private final Project project;
    private final DashboardVirtualFile dashboardVirtualFile;

    public DashboardSchemeHandlerFactory(Project project, DashboardVirtualFile file) {
        this.project = project;
        this.dashboardVirtualFile = file;
    }

    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        var url = getUrl(request);
        if (url != null) {
            var host = url.getHost();
            var file = url.getFile();
            if (DashboardFileEditor.DOMAIN_NAME.equals(host) &&
                    DashboardFileEditor.SCHEMA_NAME.equals(schemeName)) {

                String resourceName = file.replaceAll("^/+", "");
                boolean resourceExists = UIResourcesService.getInstance().isResourceExists(resourceName);
                if (resourceExists){
                    return new DashboardResourceHandler(project, resourceName, dashboardVirtualFile);
                }else{
                    return new DashboardResourceHandler(project, DashboardFileEditor.RESOURCE_FOLDER_NAME + "/index.html", dashboardVirtualFile);
                }
            }
        }
        return null;
    }

    @Nullable
    private URL getUrl(CefRequest request) {
        try {
            return new URL(request.getURL());
        } catch (MalformedURLException e) {
            return null;
        }
    }
}