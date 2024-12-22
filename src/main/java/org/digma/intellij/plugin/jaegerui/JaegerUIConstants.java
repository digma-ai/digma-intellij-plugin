package org.digma.intellij.plugin.jaegerui;

public interface JaegerUIConstants {

    String JAEGER_UI_APP_NAME = "JaegerUI";
    String JAEGER_UI_DOMAIN_NAME = "jaegerui";
    String JAEGER_UI_SCHEMA = "http";
    String JAEGER_UI_RESOURCE_FOLDER_NAME = "jaeger-ui";
    String JAEGER_UI_URL = JAEGER_UI_SCHEMA + "://" + JAEGER_UI_DOMAIN_NAME + "/" + JAEGER_UI_RESOURCE_FOLDER_NAME + "/index.html";
    String JAEGER_API_URL = JAEGER_UI_SCHEMA + "://" + JAEGER_UI_DOMAIN_NAME;

}