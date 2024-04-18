package org.digma.intellij.plugin.documentation;

public interface DocumentationConstants {

    String DOCUMENTATION_DOMAIN_NAME = "documentation";
    String DOCUMENTATION_SCHEMA = "http";
    String DOCUMENTATION_URL = DOCUMENTATION_SCHEMA + "://" + DOCUMENTATION_DOMAIN_NAME + "/index.html";
    String DOCUMENTATION_RESOURCE_FOLDER_NAME = "/webview/documentation";
    String DOCUMENTATION_INDEX_TEMPLATE_NAME = "documentation.ftl";

}