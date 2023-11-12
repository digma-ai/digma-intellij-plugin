package org.digma.intellij.plugin.dashboard.outgoing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record DashboardError(@JsonProperty(value = "data",required = true) JsonNode data,
                             @JsonProperty(value = "error",required = true) ErrorPayload error,
                             @JsonProperty(value = "type",required = true) String type ) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DashboardError {
        //nothing to do , for jackson only
    }
}