package org.digma.intellij.plugin.jaegerui.model;


import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record SpansWithResolvedLocationMessage(@JsonProperty("type") String type,
                                               @JsonProperty("action")  String action,
                                               @JsonProperty("payload") Map<String,Importance> payload ) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SpansWithResolvedLocationMessage {
        //nothing to do , for jackson only
    }
}
