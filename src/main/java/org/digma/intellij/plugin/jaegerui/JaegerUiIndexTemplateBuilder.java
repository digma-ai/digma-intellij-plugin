package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.jcef.BaseIndexTemplateBuilder;
import org.jetbrains.annotations.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JaegerUiIndexTemplateBuilder extends BaseIndexTemplateBuilder {

    private static final String JAEGER_URL_PARAM_NAME = "jaeger_url";
    private static final String JAEGER_QUERY_URL_CHANGED_FROM_DEFAULT_PARAM_NAME = "isUserChangedJaegerQueryUrl";
    private static final String INITIAL_ROUTE_PARAM_NAME = "initial_route";

    private final JaegerUIVirtualFile jaegerUIVirtualFile;

    public JaegerUiIndexTemplateBuilder(JaegerUIVirtualFile file) {
        super(JaegerUIConstants.JAEGER_UI_RESOURCE_FOLDER_NAME, JaegerUIConstants.JAEGER_UI_INDEX_TEMPLATE_NAME);
        this.jaegerUIVirtualFile = file;
    }


    @Override
    public void addAppSpecificEnvVariable(@NotNull Project project, @NotNull Map<String, Object> data) {

        var didUserChangeJaegerQueryUrl = !(SettingsState.DEFAULT_JAEGER_QUERY_URL.equalsIgnoreCase(SettingsState.getInstance().jaegerQueryUrl));
        data.put(JAEGER_URL_PARAM_NAME, jaegerUIVirtualFile.getJaegerBaseUrl());
        data.put(JAEGER_QUERY_URL_CHANGED_FROM_DEFAULT_PARAM_NAME, String.valueOf(didUserChangeJaegerQueryUrl));


        if (jaegerUIVirtualFile.getTraceId() != null) {
            var initialRoutePath = buildInitialRoutePath(jaegerUIVirtualFile.getTraceId(), jaegerUIVirtualFile.getSpanCodeObjectId(), jaegerUIVirtualFile.isSendSearchQuery());
            data.put(INITIAL_ROUTE_PARAM_NAME, initialRoutePath);
        } else if (jaegerUIVirtualFile.getTraceSamples() != null && !jaegerUIVirtualFile.getTraceSamples().isEmpty()) {

            if (jaegerUIVirtualFile.getTraceSamples().size() == 1 &&
                    jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId() != null &&
                    !Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId()).isBlank()) {

                var initialRoutePath = buildInitialRoutePath(Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId()), jaegerUIVirtualFile.getSpanCodeObjectId(), jaegerUIVirtualFile.isSendSearchQuery());
                data.put(INITIAL_ROUTE_PARAM_NAME, initialRoutePath);

            } else if (jaegerUIVirtualFile.getTraceSamples().size() == 2 &&
                    jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId() != null &&
                    !Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId()).isBlank() &&
                    jaegerUIVirtualFile.getTraceSamples().get(1).getTraceId() != null &&
                    !Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(1).getTraceId()).isBlank()) {

                var trace1 = Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(0).getTraceId()).toLowerCase();
                var trace2 = Objects.requireNonNull(jaegerUIVirtualFile.getTraceSamples().get(1).getTraceId()).toLowerCase();
                var initialRoutePath = "/trace/" + trace1 + "..." + trace2 + "?cohort=" + trace1 + "&cohort=" + trace2;
                if (jaegerUIVirtualFile.getSpanCodeObjectId() != null && jaegerUIVirtualFile.isSendSearchQuery()) {
                    initialRoutePath = initialRoutePath + "&uiFind=" + URLEncoder.encode("\"" + jaegerUIVirtualFile.getSpanCodeObjectId() + "\"", StandardCharsets.UTF_8);
                }
                data.put(INITIAL_ROUTE_PARAM_NAME, initialRoutePath);
            }
        } else {
            data.put(INITIAL_ROUTE_PARAM_NAME, "");
        }
    }

    private String buildInitialRoutePath(String traceId, @Nullable String spanCodeObjectId, boolean isSendSearchQuery) {
        var traceLowerCase = traceId.toLowerCase();
        var url = "/trace/" + traceLowerCase + "?cohort=" + traceLowerCase;
        if (spanCodeObjectId != null && isSendSearchQuery) {
            url = url + "&uiFind=" + URLEncoder.encode("\"" + spanCodeObjectId + "\"", StandardCharsets.UTF_8);
        }
        return url;
    }
}
