package org.comroid.api.data.seri.type;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.comroid.annotations.internal.Annotations;
import org.comroid.api.attr.IntegerAttribute;
import org.comroid.api.attr.LongAttribute;
import org.comroid.api.attr.Named;
import org.comroid.api.attr.UUIDContainer;
import org.comroid.api.func.ext.StreamSupplier;
import org.comroid.api.func.util.Bitmask;
import org.comroid.api.html.form.HtmlSelectDesc;

import jakarta.persistence.NamedAttributeNode;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
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
    public T parse(final String data) {
        Predicate<T> test = t -> {
            var parse = StandardValueType.findGoodType(data);
            if (parse instanceof UUID && t instanceof UUIDContainer attr)
                return attr.getUuid().equals(parse);
            if (parse instanceof Number) {
                if (t instanceof Bitmask.Attribute<?> attr)
                    return attr.isFlagSet((long)parse);
                if (t instanceof LongAttribute attr)
                    return attr.equals((long)parse);
                if (t instanceof IntegerAttribute attr)
                    return attr.equals((int)parse);
            }
            if (parse instanceof String)
                if (t instanceof Named attr)
                    return attr.getName().equals(parse);
            return t.toString().equals(data);
        };
        return Annotations.constants(getTargetClass())
                .filter(test)
                .findAny()
                .orElse(null);
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
