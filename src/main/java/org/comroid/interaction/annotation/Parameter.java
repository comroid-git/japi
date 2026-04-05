package org.comroid.interaction.annotation;

import org.comroid.annotations.internal.Annotations;
import org.comroid.api.attr.Described;
import org.comroid.api.func.util.Streams;
import org.comroid.interaction.registry.RegistryHelper;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Optional;

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
    record Resolved(String value, String description, boolean required, Completion[] completion, Class<? extends Parser> parser)
            implements Parameter, Described {
        public static Resolved of(Element element) {
            return new Resolved(RegistryHelper.findName(element).orElseThrow(),
                    RegistryHelper.findDescription(element.annotated).orElse(null),
                    element.parameter.required(),
                    Optional.of(element.annotated.getType())
                            .filter(Class::isEnum)
                            .map(eType -> Arrays.stream(eType.getEnumConstants()).flatMap(Streams.cast(Enum.class)).map(Enum::name).toArray(String[]::new))
                            .map(Completion.ConstantStrings::new)
                            .map(it -> new Completion[]{ it })
                            .orElseGet(element.parameter::completion),
                    element.parameter.parser());
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
