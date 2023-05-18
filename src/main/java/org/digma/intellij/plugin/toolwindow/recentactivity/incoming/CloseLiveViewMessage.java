package org.digma.intellij.plugin.toolwindow.recentactivity.incoming;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record CloseLiveViewMessage(@JsonProperty(value = "action",required = true) String action,
                                   @JsonProperty(value = "payload",required = true) CloseLiveViewPayload payload){

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CloseLiveViewMessage {
        //nothing to do , for jackson only
    }
}