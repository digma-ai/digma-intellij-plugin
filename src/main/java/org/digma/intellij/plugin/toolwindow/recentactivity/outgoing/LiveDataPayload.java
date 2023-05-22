package org.digma.intellij.plugin.toolwindow.recentactivity.outgoing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.digma.intellij.plugin.model.rest.livedata.DurationData;
import org.digma.intellij.plugin.model.rest.livedata.LiveDataRecord;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record LiveDataPayload(@JsonProperty(value = "liveDataRecords",required = true) List<LiveDataRecord> liveDataRecords,
                              @JsonProperty(value = "durationData",required = true) DurationData durationData){
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public LiveDataPayload {
        //nothing to do , for jackson only
    }

}