package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.ui.jcef.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

class DocumentationEnvJsTemplateBuilder extends BaseEnvJsTemplateBuilder {

    private static final String DOCUMENTATION_PAGE = "documentationPage";
    private final DocumentationVirtualFile documentationVirtualFile;

    public DocumentationEnvJsTemplateBuilder(DocumentationVirtualFile documentationVirtualFile,String templatePath) {
        super(templatePath);
        this.documentationVirtualFile = documentationVirtualFile;
    }


    @Override
    public void addAppSpecificEnvVariable(@NotNull Project project, @NotNull Map<String, Object> data) {
        data.put(DOCUMENTATION_PAGE, documentationVirtualFile.getDocumentationPage());
    }

}
