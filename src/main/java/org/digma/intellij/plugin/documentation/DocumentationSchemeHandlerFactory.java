package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

public class DocumentationSchemeHandlerFactory implements CefSchemeHandlerFactory {
    private final Project project;
    private final DocumentationVirtualFile documentationVirtualFile;

    public DocumentationSchemeHandlerFactory(Project project, DocumentationVirtualFile file) {
        this.project = project;
        this.documentationVirtualFile = file;
    }

    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        var url = getUrl(request);
        if (url != null) {
            var host = url.getHost();
            var file = url.getFile();
            if (DocumentationFileEditor.DOMAIN_NAME.equals(host) &&
                    DocumentationFileEditor.SCHEMA_NAME.equals(schemeName)) {
                var resourceName = DocumentationFileEditor.RESOURCE_FOLDER_NAME + file;
                var resource = getClass().getResource(resourceName);
                if (resource != null) {
                    return new DocumentationResourceHandler(project, resourceName, documentationVirtualFile);
                } else if (getClass().getResource("/webview/common" + file) != null) {
                    return new DocumentationResourceHandler(project, "/webview/common" + file, documentationVirtualFile);
                } else {
                    return new DocumentationResourceHandler(project, DocumentationFileEditor.RESOURCE_FOLDER_NAME + "/index.html", documentationVirtualFile);
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
