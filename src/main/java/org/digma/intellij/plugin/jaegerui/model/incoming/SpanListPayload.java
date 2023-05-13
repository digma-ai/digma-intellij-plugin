package org.digma.intellij.plugin.jaegerui.model.incoming;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record SpanListPayload(@JsonProperty(value = "spans",required = true) List<Span> spans) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SpanListPayload {
        //nothing to do , for jackson only
    }
}
