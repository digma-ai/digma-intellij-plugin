package org.digma.intellij.plugin.insights.model.outgoing;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record Span(String spanCodeObjectId, String spanDisplayName) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Span {
        //nothing to do , for jackson only
    }
}
