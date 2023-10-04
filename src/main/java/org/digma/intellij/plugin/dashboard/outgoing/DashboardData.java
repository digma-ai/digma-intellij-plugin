package org.digma.intellij.plugin.dashboard.outgoing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.digma.intellij.plugin.jaegerui.model.outgoing.SpanData;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record DashboardData(@JsonProperty(value = "type",required = true) String type,
                            @JsonProperty(value = "action",required = true)  String action,
                            @JsonProperty(value = "payload",required = true) JsonNode payload ) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DashboardData {
        //nothing to do , for jackson only
    }
}
