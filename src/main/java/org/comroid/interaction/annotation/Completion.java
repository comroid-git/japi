package org.comroid.interaction.annotation;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.annotations.Instance;
import org.comroid.api.attr.Named;
import org.comroid.api.data.seri.type.EnumValueType;
import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.api.data.seri.type.ValueType;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.node.ParameterNode;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@Target({ })
@Retention(RetentionPolicy.RUNTIME)
public @interface Completion {
    /// constant strings for autocompletion
    String[] strings() default { };

    /// additional provider for more completion options
    Class<? extends Provider> provider() default Provider.Default.class;

    interface Provider extends BiFunction<InteractionContext, ParameterNode, Stream<? extends CharSequence>> {
        @Override
        Stream<? extends CharSequence> apply(InteractionContext context, ParameterNode parameter);

        /// default automated implementation for
        enum Default implements Provider {
            @Instance INSTANCE;

            @Override
            public Stream<? extends CharSequence> apply(InteractionContext context, ParameterNode parameter) {
                var type = ValueType.of(parameter.getReflect().getType());

                if (StandardValueType.BOOLEAN.equals(type)) return Stream.of("true", "false");
                if (type instanceof EnumValueType) return Arrays.stream(type.getTargetClass().getEnumConstants()).map(Named::$);

                return Stream.empty();
            }
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
