package org.comroid.api.data.seri.type;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.comroid.annotations.Default;
import org.comroid.api.Polyfill;
import org.comroid.api.func.ext.StreamSupplier;
import org.comroid.api.html.form.HtmlSelectDesc;
import org.comroid.api.java.ReflectionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class EnumValueType<T extends Enum<? super T>> extends BoundValueType<T> implements StreamSupplier<T>, HtmlSelectDesc {
    EnumValueType(Class<T> type) {
        super(type);
    }

    @Override
    public String getHtmlTagName() {
        return HtmlSelectDesc.super.getHtmlTagName();
    }

    @Override
    public Map<String, String> getHtmlSelectOptions() {
        return stream().map(Enum::name)
                .collect(Collectors.toMap(Function.identity(), Function.identity()));
    }

    @Override
    public Stream<T> stream() {
        return Stream.of(targetClass.getEnumConstants());
    }

    @Override
    public @Nullable Object defaultValue() {
        return Optional.ofNullable(super.defaultValue())
                .orElseGet(() -> targetClass.getEnumConstants()[0]);
    }

    @Override
    public T parse(final String data) {
        return stream()
                .filter(it -> it.name().equalsIgnoreCase(data))
                .findAny()
                .map(Polyfill::<T>uncheckedCast)
                .or(() -> ReflectionHelper.fieldWithAnnotation(targetClass, Default.class).stream()
                        .flatMap(fld -> Stream.ofNullable(ReflectionHelper.<T>forceGetField(null, fld)))
                        .findAny())
                .orElse(null);
    }
}
