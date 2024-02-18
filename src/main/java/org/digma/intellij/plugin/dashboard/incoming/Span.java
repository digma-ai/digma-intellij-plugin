package org.digma.intellij.plugin.dashboard.incoming;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record Span(
        @JsonProperty(value = "spanCodeObjectId") String spanCodeObjectId,
        @JsonProperty(value = "type") String type,
        @JsonProperty(value = "environment") String environment
) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Span {
        //nothing to do , for jackson only
    }
}
