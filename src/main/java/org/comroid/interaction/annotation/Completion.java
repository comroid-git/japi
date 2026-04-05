package org.comroid.interaction.annotation;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.annotations.Instance;
import org.comroid.api.attr.Described;
import org.comroid.api.attr.Named;
import org.comroid.api.data.seri.type.EnumValueType;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.node.ParameterNode;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.stream.Stream;

@Target({ })
@Retention(RetentionPolicy.RUNTIME)
public @interface Completion {
    /// constant strings for autocompletion
    String[] strings() default { };

    /// additional provider for more completion options
    Class<? extends Provider> provider() default Provider.Default.class;

    interface Provider {
        Stream<Option> findCompletionOptions(InteractionContext context, ParameterNode parameter, String currentValue);

        /// default automated implementation for
        enum Default implements Provider.OfStrings {
            @Instance INSTANCE;

            @Override
            public Stream<String> findCompletionValues(InteractionContext context, ParameterNode parameter, String currentValue) {
                var type = ValueType.of(parameter.getReflect().getType());

                if (StandardValueType.BOOLEAN.equals(type)) return Stream.of("true", "false");
                if (type instanceof EnumValueType) return Arrays.stream(type.getTargetClass().getEnumConstants()).map(Named::$);

                return Stream.empty();
            }
        }

        interface OfStrings extends Provider {
            @Override
            default Stream<Option> findCompletionOptions(InteractionContext context, ParameterNode parameter, String currentValue) {
                return findCompletionValues(context, parameter, currentValue).map(Option::new);
            }

            Stream<String> findCompletionValues(InteractionContext context, ParameterNode parameter, String currentValue);
        }
    }

    record Option(CharSequence key, @Nullable CharSequence display) {
        public Option(CharSequence value) {
            this(value, value);
        }

        public <X extends Named & Described> Option(X it) {
            this(it.getName(), it.getDescription());
        }
    }

    @Value
    @NonFinal
    @SuppressWarnings("ClassExplicitlyAnnotation")
    class ConstantStrings implements Completion {
        String[] strings;

        @Override
        public String[] strings() {
            return strings;
        }

        @Override
        public Class<? extends Provider> provider() {
            return Provider.class;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Completion.class;
        }
    }
}
