package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

public class JaegerUISchemeHandlerFactory implements CefSchemeHandlerFactory {
    private final Project project;
    private final JaegerUIVirtualFile jaegerUIVirtualFile;

    public JaegerUISchemeHandlerFactory(Project project, JaegerUIVirtualFile file) {
        this.project = project;
        this.jaegerUIVirtualFile = file;
    }

    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        var url = getUrl(request);
        if (url != null) {
            var host = url.getHost();
            var file = url.getFile();
            if (JaegerUIFileEditor.DOMAIN_NAME.equals(host) &&
                    JaegerUIFileEditor.SCHEMA_NAME.equals(schemeName)){
                var resourceName = JaegerUIFileEditor.RESOURCE_FOLDER_NAME+file;
                var resource = getClass().getResource(resourceName);
                if (resource != null) {
                    return new JaegerUIResourceHandler(project, resourceName,jaegerUIVirtualFile);
                }else{
                    return new JaegerUIResourceHandler(project, JaegerUIFileEditor.RESOURCE_FOLDER_NAME+"/index.html",jaegerUIVirtualFile);
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
