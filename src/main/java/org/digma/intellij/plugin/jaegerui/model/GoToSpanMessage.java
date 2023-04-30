package org.digma.intellij.plugin.jaegerui.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record GoToSpanMessage(@JsonProperty("action") String action,
                              @JsonProperty("payload") Span payload) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GoToSpanMessage {
        //nothing to do , for jackson only
    }
}

