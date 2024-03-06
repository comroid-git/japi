package org.comroid.api.data.seri.type;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.comroid.api.func.ext.StreamSupplier;
import org.comroid.api.html.form.HtmlSelectDesc;

import java.util.Map;
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
    public Stream<? extends T> stream() {
        return Stream.of(targetClass.getEnumConstants());
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
}
