package org.digma.intellij.plugin.insights;

import com.intellij.openapi.diagnostic.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.digma.intellij.plugin.jcef.common.JCefTemplateUtils;
import org.digma.intellij.plugin.log.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

class InsightsIndexTemplateBuilder {

    private final Logger logger = Logger.getInstance(InsightsIndexTemplateBuilder.class);

    private static final String INDEX_TEMPLATE_NAME = "insightstemplate.ftl";

    private final Configuration freemarketConfiguration = new Configuration(Configuration.VERSION_2_3_30);

    public InsightsIndexTemplateBuilder() {
        freemarketConfiguration.setClassForTemplateLoading(this.getClass(), InsightsPanel.RESOURCE_FOLDER_NAME);
        freemarketConfiguration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarketConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarketConfiguration.setNumberFormat("computer");
    }

    public InputStream build() {

        try {
            var data = new HashMap<String, Object>();
            JCefTemplateUtils.addCommonEnvVariables(data);

            Template template = freemarketConfiguration.getTemplate(INDEX_TEMPLATE_NAME);
            StringWriter stringWriter = new StringWriter();
            template.process(data, stringWriter);
            return new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            Log.debugWithException(logger, e, "error creating template for index.html");
            return null;
        }
    }

}
