package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.ui.jcef.BaseIndexTemplateBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

class DocumentationIndexTemplateBuilder extends BaseIndexTemplateBuilder {

    private static final String DOCUMENTATION_PAGE = "documentationPage";
    private final DocumentationVirtualFile documentationVirtualFile;

    public DocumentationIndexTemplateBuilder(DocumentationVirtualFile documentationVirtualFile) {
        super(DocumentationConstants.DOCUMENTATION_RESOURCE_FOLDER_NAME, DocumentationConstants.DOCUMENTATION_INDEX_TEMPLATE_NAME);
        this.documentationVirtualFile = documentationVirtualFile;
    }


    @Override
    public void addAppSpecificEnvVariable(@NotNull Project project, @NotNull Map<String, Object> data) {
        data.put(DOCUMENTATION_PAGE, documentationVirtualFile.getDocumentationPage());
    }

}
