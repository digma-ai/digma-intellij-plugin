package org.digma.intellij.plugin.toolwindow.recentactivity.outgoing;


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
public record LiveDataMessage(@JsonProperty(value = "type",required = true) String type,
                              @JsonProperty(value = "action",required = true)  String action,
                              @JsonProperty(value = "payload",required = true) LiveDataPayload payload ) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public LiveDataMessage {
        //nothing to do , for jackson only
    }
}



