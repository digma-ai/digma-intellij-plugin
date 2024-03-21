package org.digma.intellij.plugin.jaegerui;

public interface JaegerUIConstants {

    String JAEGER_UI_DOMAIN_NAME = "jaegerui";
    String JAEGER_UI_SCHEMA = "http";
    String JAEGER_UI_URL = JAEGER_UI_SCHEMA + "://" + JAEGER_UI_DOMAIN_NAME + "/index.html";
    String JAEGER_UI_RESOURCE_FOLDER_NAME = "/webview/jaegerui";
    String JAEGER_UI_INDEX_TEMPLATE_NAME = "jaegeruitemplate.ftl";

}