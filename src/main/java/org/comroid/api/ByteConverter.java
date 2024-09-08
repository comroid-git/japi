package org.comroid.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public interface ByteConverter<T> {
    byte[] toBytes(T it);
    T fromBytes(byte[] bytes);

    static <T> ByteConverter<T> jackson(Class<T> type) {
        return new ByteConverter<>() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            @SneakyThrows
            public byte[] toBytes(T it) {
                return mapper.writeValueAsBytes(it);
            }

            @Override
            @SneakyThrows
            public T fromBytes(byte[] bytes) {
                return mapper.readValue(bytes, type);
            }
        };
    }
}
