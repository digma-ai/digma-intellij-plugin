package org.digma.intellij.plugin.assets;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.text.VersionComparatorUtil;
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

class AssetsIndexTemplateBuilder {

    private final Logger logger = Logger.getInstance(AssetsIndexTemplateBuilder.class);

    private static final String INDEX_TEMPLATE_NAME = "assetstemplate.ftl";
    private static final String ASSET_SEARCH_ENV_NAME = "assetsSearch";

    private final Configuration freemarketConfiguration = new Configuration(Configuration.VERSION_2_3_30);

    public AssetsIndexTemplateBuilder() {
        freemarketConfiguration.setClassForTemplateLoading(this.getClass(), AssetsService.RESOURCE_FOLDER_NAME);
        freemarketConfiguration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarketConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarketConfiguration.setNumberFormat("computer");
    }

    public InputStream build() {

        try {
            var data = new HashMap<String, Object>();
            JCefTemplateUtils.addCommonEnvVariables(data);

            var assetSearchEnabledForLinux = VersionComparatorUtil.compare(ApplicationInfo.getInstance().getMajorVersion(),"2023") >= 0;
            data.put(ASSET_SEARCH_ENV_NAME, SystemInfo.isLinux ? String.valueOf(assetSearchEnabledForLinux) : "true");


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
