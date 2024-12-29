package org.comroid.api.model.minecraft.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.comroid.api.attr.Named;

import java.io.IOException;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public
enum DefaultPermissionValue implements Named {
    FALSE("false"),
    NOT_OP("not op"),
    OP("op"),
    TRUE("true");

    String ident;

    @NoArgsConstructor
    public static class Serializer extends JsonSerializer<DefaultPermissionValue> {
        @Override
        public void serialize(DefaultPermissionValue value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.ident);
        }
    }

    @Value
    @NoArgsConstructor
    public static class Deserializer extends JsonDeserializer<DefaultPermissionValue> {
        @Override
        public DefaultPermissionValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var id = p.getValueAsString();
            return Arrays.stream(values())
                    .filter(item -> item.ident.equals(id))
                    .findAny()
                    .orElse(DefaultPermissionValue.OP);
        }
    }
}
