package org.digma.intellij.plugin.jaegerui.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record Span(@JsonProperty("id") String id,
                   @JsonProperty("name") String name,
                   @JsonProperty("instrumentationLibrary") String instrumentationLibrary) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Span {
        //nothing to do , for jackson only
    }
}
