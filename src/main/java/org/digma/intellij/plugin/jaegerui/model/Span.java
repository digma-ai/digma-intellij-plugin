package org.digma.intellij.plugin.jaegerui.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record Span(@JsonProperty(value = "id",required = true) String id,
                   @JsonProperty(value = "name",required = true) String name,
                   @JsonProperty(value = "instrumentationLibrary",required = true) String instrumentationLibrary,
                   @JsonProperty("function") String function,
                   @JsonProperty("namespace") String namespace) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Span {
        //nothing to do , for jackson only
    }
}