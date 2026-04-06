package org.comroid.interaction.annotation;

import org.comroid.annotations.internal.Annotations;
import org.comroid.api.attr.Described;
import org.comroid.interaction.registry.RegistryHelper;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Interaction {
    /// name override
    String value() default Annotations.EMPTY_ATTRIBUTE;

    /// whether to run this interaction in async mode
    boolean async() default false;

    /// privacy level to use for the interaction
    PrivacyLevel privacy() default PrivacyLevel.EPHEMERAL;

    ///  required permission checks
    ContextFilter[] filter() default { };

    /// context definitions to emit
    ContextDefinition[] definitions() default { };

    /// whether to detach this interaction from its parent container
    boolean detached() default false;

    enum PrivacyLevel {
        /// response is sent publicly
        PUBLIC,
        /// response is sent as ephemeral
        EPHEMERAL,
        /// response is sent privately
        PRIVATE
    }

    @SuppressWarnings("ClassExplicitlyAnnotation")
    record Resolved(
            String value, String description, boolean async, PrivacyLevel privacy, ContextFilter[] filter, ContextDefinition[] definitions, boolean detached
    ) implements Interaction, Described {
        public static Resolved of(Element element, @Nullable Element parent) {
            var detached = element.interaction.detached();
            return new Resolved(RegistryHelper.findName(element).orElseThrow(),
                    RegistryHelper.findDescription(element.annotated).orElse(null),
                    element.interaction.async() || parent != null && parent.interaction.async(),
                    element.interaction.privacy(),
                    Stream.concat(Arrays.stream(element.interaction.filter()),
                            Stream.ofNullable(parent)
                                    .filter($ -> !detached)
                                    .filter(Objects::nonNull)
                                    .map(Element::interaction)
                                    .flatMap(it -> Arrays.stream(it.filter()))).toArray(ContextFilter[]::new),
                    Stream.concat(Arrays.stream(element.interaction.definitions()),
                            Stream.ofNullable(parent)
                                    .filter($ -> !detached)
                                    .filter(Objects::nonNull)
                                    .map(Element::interaction)
                                    .flatMap(it -> Arrays.stream(it.definitions()))).toArray(ContextDefinition[]::new),
                    detached);
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

    record Element(Interaction interaction, AnnotatedElement annotated) {}
}
