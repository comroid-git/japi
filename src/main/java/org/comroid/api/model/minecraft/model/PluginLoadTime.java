package org.comroid.api.model.minecraft.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.comroid.api.attr.Named;

import java.io.IOException;

public enum PluginLoadTime implements Named {
    STARTUP, POSTWORLD;

    @NoArgsConstructor
    public static class Serializer extends JsonSerializer<PluginLoadTime> {
        @Override
        public void serialize(PluginLoadTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.name());
        }
    }

    @Value
    @NoArgsConstructor
    public static class Deserializer extends JsonDeserializer<PluginLoadTime> {
        @Override
        public PluginLoadTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return PluginLoadTime.valueOf(p.getValueAsString());
        }
    }
}
