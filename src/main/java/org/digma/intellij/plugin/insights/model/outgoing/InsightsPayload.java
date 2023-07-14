package org.digma.intellij.plugin.insights.model.outgoing;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.ui.model.UIInsightsStatus;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record InsightsPayload(List<CodeObjectInsight> insights,
                              List<Span> spans,
                              String assetId,
                              String serviceName,
                              String environment,
                              String insightsStatus,
                              String viewMode,
                              List<Method> methods,
                              boolean hasMissingDependency) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public InsightsPayload {
        //nothing to do , for jackson only
    }


    public static final InsightsPayload EMPTY_INSIGHTS = new InsightsPayload(Collections.emptyList(), Collections.emptyList(),
            "", "", "", UIInsightsStatus.NoInsights.name(), ViewMode.INSIGHTS.name(), Collections.emptyList(), false);
    public static final InsightsPayload EMPTY_PREVIEW = new InsightsPayload(Collections.emptyList(), Collections.emptyList(),
            "", "", "", UIInsightsStatus.NoInsights.name(), ViewMode.PREVIEW.name(), Collections.emptyList(), false);
}
