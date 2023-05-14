package org.digma.intellij.plugin.jaegerui.model.incoming;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record SpansMessage(@JsonProperty(value = "action",required = true) String action,
                           @JsonProperty(value = "payload",required = true) SpanListPayload payload) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SpansMessage {
        //nothing to do , for jackson only
    }
}

