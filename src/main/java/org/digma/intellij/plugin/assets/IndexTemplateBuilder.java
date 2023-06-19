package org.digma.intellij.plugin.assets;

import com.intellij.openapi.diagnostic.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.digma.intellij.plugin.jaegerui.JaegerUIService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.toolwindow.common.ThemeUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class IndexTemplateBuilder {

    private final Logger logger = Logger.getInstance(JaegerUIService.class);

    private static final String INDEX_TEMPLATE_NAME = "assetstemplate.ftl";
    private static final String THEME_ENV_NAME = "theme";

    private final Configuration freemarketConfiguration = new Configuration(Configuration.VERSION_2_3_30);

    public IndexTemplateBuilder(String templatePath) {
        freemarketConfiguration.setClassForTemplateLoading(this.getClass(), AssetsPanel.RESOURCE_FOLDER_NAME);
        freemarketConfiguration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarketConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarketConfiguration.setNumberFormat("computer");
    }

    public InputStream build() {

        try {
            var data = new HashMap<String, String>();
            data.put(THEME_ENV_NAME, ThemeUtil.getCurrentThemeName());

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
