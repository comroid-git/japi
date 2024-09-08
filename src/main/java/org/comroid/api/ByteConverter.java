package org.comroid.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public interface ByteConverter<T> {
    byte[] toBytes(T it);
    T fromBytes(byte[] bytes);

    static <T> ByteConverter<T> jackson(Class<T> type) {
        return new ByteConverter<T>() {
            @Override
            @SneakyThrows
            public byte[] toBytes(T it) {
                return new ObjectMapper().writeValueAsBytes(it);
            }

            @Override
            @SneakyThrows
            public T fromBytes(byte[] bytes) {
                return new ObjectMapper().readValue(bytes, type);
            }
        };
    }
}
