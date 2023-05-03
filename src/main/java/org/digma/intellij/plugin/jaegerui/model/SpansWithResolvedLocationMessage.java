package org.digma.intellij.plugin.jaegerui.model;


import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record SpansWithResolvedLocationMessage(@JsonProperty(value = "type",required = true) String type,
                                               @JsonProperty(value = "action",required = true)  String action,
                                               @JsonProperty(value = "payload",required = true) Map<String,Importance> payload ) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SpansWithResolvedLocationMessage {
        //nothing to do , for jackson only
    }
}
