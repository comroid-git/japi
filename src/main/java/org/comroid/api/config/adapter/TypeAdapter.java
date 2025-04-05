package org.comroid.api.config.adapter;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.func.ext.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Value
@NonFinal
public abstract class TypeAdapter<T, S> {
    private static final Map<Class<?>, TypeAdapter<?, ?>> cache = new ConcurrentHashMap<>();
    public static final  Map<Class<?>, TypeAdapter<?, ?>> CACHE = Collections.unmodifiableMap(cache);

    Class<T>             type;
    StandardValueType<S> serialized;
    String               nameSuffix;

    protected TypeAdapter(Class<T> type, StandardValueType<S> serialized, String nameSuffix) {
        this.type       = type;
        this.serialized = serialized;
        this.nameSuffix = nameSuffix;

        cache.put(type, this);
    }

    public abstract @NotNull S toSerializable(Context context, T value);

    public abstract Optional<T> deserialize(Context context, S serialized);

    public abstract S parseSerialized(@Nullable String string);
}
