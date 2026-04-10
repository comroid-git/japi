package org.comroid.interaction.annotation;

import org.comroid.annotations.internal.Annotations;
import org.comroid.api.attr.Described;
import org.comroid.api.func.util.Streams;
import org.comroid.interaction.registry.RegistryHelper;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    /// name override
    String value() default Annotations.EMPTY_ATTRIBUTE;

    /// whether this parameter is required
    boolean required() default true;

    /// auto-completion provider
    Completion[] completion() default { };

    /// parser for unknown types that take a string as input
    Class<? extends Parser> parser() default Parser.class;

    interface Parser<T> {
        T parse(String string);
    }

    @SuppressWarnings({ "ClassExplicitlyAnnotation", "rawtypes" })
    record Resolved(String value, @Nullable String description, boolean required, Completion[] completion, @Nullable Class<? extends Parser> parser)
            implements Parameter, Described {
        public static Resolved of(Element element) {
            var completions = Stream.concat(Stream.of(element.annotated.getType())
                    .filter(Class::isEnum)
                    .map(eType -> Arrays.stream(eType.getEnumConstants())
                            .flatMap(Streams.cast(Enum.class))
                            .map(Enum::name)
                            .filter(Predicate.not("unknown"::equalsIgnoreCase))
                            .toArray(String[]::new))
                    .map(Completion.ConstantStrings::new), Arrays.stream(element.parameter.completion())).toArray(Completion[]::new);

            return new Resolved(RegistryHelper.findName(element).orElseThrow(),
                    RegistryHelper.findDescription(element.annotated).orElse(null),
                    element.parameter.required() || !element.annotated.isAnnotationPresent(Nullable.class),
                    completions,
                    Optional.ofNullable(element.parameter.parser()).filter(Predicate.not(Parser.class::equals)).orElse(null));
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Interaction.class;
        }
    }

    record Element(Parameter parameter, java.lang.reflect.Parameter annotated) {}
}
