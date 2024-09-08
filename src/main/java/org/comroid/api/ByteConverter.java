package org.comroid.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.comroid.api.func.util.Debug;
import org.comroid.api.info.Log;

import java.io.IOException;

public interface ByteConverter<T> {
    byte[] toBytes(T it);
    T fromBytes(byte[] bytes);

    static <T> ByteConverter<T> jackson(Class<T> type) {
        return new ByteConverter<>() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public byte[] toBytes(T it) {
                try {
                    return mapper.writeValueAsBytes(it);
                } catch (JsonProcessingException e) {
                    Debug.log(Log.get(), "Could not serialize " + type.getCanonicalName(), e);
                    return new byte[0];
                }
            }

            @Override
            public T fromBytes(byte[] bytes) {
                try {
                    return mapper.readValue(bytes, type);
                } catch (IOException e) {
                    Debug.log(Log.get(), "Could not deserialize " + type.getCanonicalName(), e);
                    return null;
                }
            }
        };
    }
}
