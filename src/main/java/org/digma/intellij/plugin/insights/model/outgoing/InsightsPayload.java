package org.digma.intellij.plugin.insights.model.outgoing;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record InsightsPayload(List<CodeObjectInsight> insights) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public InsightsPayload {
        //nothing to do , for jackson only
    }
}
