package org.digma.intellij.plugin.jaegerui.model.outgoing;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record SpanData(@JsonProperty(value = "hasCodeLocation",required = true) boolean hasCodeLocation,
                       @JsonProperty(value = "insights",required = true) List<Insight> insights) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SpanData {
        //nothing to do , for jackson only
    }
}
