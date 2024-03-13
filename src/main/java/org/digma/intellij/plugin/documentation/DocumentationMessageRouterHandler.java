package org.digma.intellij.plugin.documentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler;
import org.jetbrains.annotations.NotNull;

public class DocumentationMessageRouterHandler extends BaseMessageRouterHandler {


    public DocumentationMessageRouterHandler(Project project) {
        super(project);
    }


    @NotNull
    @Override
    public String getOriginForTroubleshootingEvent() {
        return "documentation";
    }

    @Override
    public boolean doOnQuery(@NotNull Project project, @NotNull CefBrowser browser, @NotNull JsonNode requestJsonNode, @NotNull String rawRequest, @NotNull String action) throws Exception {
        return true;
    }
}
