package org.digma.intellij.plugin.jaegerui.model.incoming;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.digma.intellij.plugin.common.CodeObjectsUtil;
import org.jetbrains.annotations.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record Span(
        @JsonProperty(value = "id", required = true) String id,
        @JsonProperty(value = "name", required = true) String name,
        @JsonProperty(value = "instrumentationLibrary", required = true) String instrumentationLibrary,
        //TODO: spanCodeObjectId - add 'required = true' once backend version is out
        @JsonProperty(value = "spanCodeObjectId") String spanCodeObjectId,
        @JsonProperty(value = "methodCodeObjectId") String methodCodeObjectId, // can be null or empty
        // deprecated - use methodCodeObjectId
        @JsonProperty("function") String function,
        // deprecated - use methodCodeObjectId
        @JsonProperty("namespace") String namespace
) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Span {
        //nothing to do , for jackson only
    }

    @NotNull
    public String spanId() {
        if (spanCodeObjectId != null) { // TODO: once spanCodeObjectId is required remove this if
            return CodeObjectsUtil.stripSpanPrefix(spanCodeObjectId);
        }
        // backward compatibility ,but note its not accurate since span name might be different than span name at spanCodeObjectId
        return CodeObjectsUtil.createSpanId(instrumentationLibrary, name);
    }

    @Nullable
    public String methodId() {
        if (methodCodeObjectId != null) {
            return CodeObjectsUtil.stripMethodPrefix(methodCodeObjectId);
        }
        if (function != null && namespace != null) {
            return CodeObjectsUtil.createMethodCodeObjectId(namespace, function);
        }
        return null;
    }

}