package org.digma.intellij.plugin.troubleshooting;

import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;
import org.digma.intellij.plugin.updates.ui.UIResourcesService;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

class TroubleshootingSchemeHandlerFactory implements CefSchemeHandlerFactory {
    private final Project project;

    public TroubleshootingSchemeHandlerFactory(Project project) {
        this.project = project;
    }

    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        var url = getUrl(request);
        if (url != null) {
            var host = url.getHost();
            var file = url.getPath();
            if (TroubleshootingService.DOMAIN_NAME.equals(host) &&
                    TroubleshootingService.SCHEMA_NAME.equals(schemeName)) {

                String resourceName = file.replaceAll("^/+", "");
                boolean resourceExists = UIResourcesService.getInstance().isResourceExists(resourceName);
                if (resourceExists){
                    return new TroubleshootingResourceHandler(project, resourceName);
                }else{
                    return new TroubleshootingResourceHandler(project, TroubleshootingService.RESOURCE_FOLDER_NAME + "/index.html");
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
