package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.project.Project;
import org.cef.handler.CefResourceHandler;
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class DocumentationSchemeHandlerFactory extends BaseSchemeHandlerFactory {

    private final DocumentationVirtualFile documentationVirtualFile;

    public DocumentationSchemeHandlerFactory(Project project, DocumentationVirtualFile file) {
        super(project);
        this.documentationVirtualFile = file;
    }


    @NotNull
    @Override
    public CefResourceHandler createResourceHandler(@NotNull String resourceName, boolean resourceExists) {
        if (resourceExists) {
            return new DocumentationResourceHandler(getProject(), resourceName, documentationVirtualFile);
        } else {
            return new DocumentationResourceHandler(getProject(), DocumentationConstants.DOCUMENTATION_RESOURCE_FOLDER_NAME + "/index.html", documentationVirtualFile);
        }
    }

    @NotNull
    @Override
    public String getSchema() {
        return DocumentationConstants.DOCUMENTATION_SCHEMA;
    }

    @NotNull
    @Override
    public String getDomain() {
        return DocumentationConstants.DOCUMENTATION_DOMAIN_NAME;
    }

    @NotNull
    @Override
    public String getResourceFolderName() {
        return DocumentationConstants.DOCUMENTATION_RESOURCE_FOLDER_NAME;
    }
}
