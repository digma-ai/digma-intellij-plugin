package org.digma.intellij.plugin.jaegerui.model.outgoing;


import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record Insight(@JsonProperty(value = "type",required = true) String type,
                      @JsonProperty(value = "importance",required = true)int importance) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Insight {
        //nothing to do , for jackson only
    }
}
