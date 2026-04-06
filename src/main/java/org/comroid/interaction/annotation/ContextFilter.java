package org.comroid.interaction.annotation;

import org.comroid.annotations.Instance;
import org.comroid.api.attr.Named;
import org.comroid.interaction.model.InteractionContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiPredicate;

/// by default, falls back to checking for any present context key
@Target({ })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContextFilter {
    /// context key
    String key();

    /// check type to perform against filter
    Check check() default Check.ANY;

    /// value to check against
    String filter() default "";

    /// when set, an instance of the given {@link Provider provider class is used to evaluate
    Class<? extends Provider> provider() default Provider.Default.class;

    enum Check implements Named, BiPredicate<ContextFilter, InteractionContext> {
        /// target value must be {@link Objects#equals equal to} {@link #filter filter} value
        EQUALS {
            @Override
            public boolean test(ContextFilter requirement, InteractionContext context) {
                var target = context.getValue(requirement.key());
                var filter = requirement.filter();

                return Objects.equals(target, filter);
            }
        },
        /// target value be {@link String#equalsIgnoreCase(String)} equal (case-insensitive) to} or within delta <= 0.1 in reference to {@link #filter filter value}
        SIMILAR {
            @Override
            public boolean test(ContextFilter requirement, InteractionContext context) {
                var target = context.getValue(requirement.key());
                var filter = requirement.filter();

                if (target instanceof CharSequence) return filter.equalsIgnoreCase(target.toString());
                if (target instanceof Number num) {
                    var x = Double.parseDouble(filter);
                    var y = num.doubleValue();
                    return Math.max(x, y) - Math.min(x, y) <= 0.1;
                }
                return Objects.equals(target, filter);
            }
        },
        /// target value must contain any non-null {@link #filter filter} value
        ANY {
            @Override
            public boolean test(ContextFilter requirement, InteractionContext context) {
                var target = context.getValue(requirement.key());
                var filter = requirement.filter();

                return Check.any(target, filter);
            }
        },
        /// inverted variant of {@link #ANY}
        ABSENT {
            @Override
            public boolean test(ContextFilter requirement, InteractionContext context) {
                var target = context.getValue(requirement.key());
                var filter = requirement.filter();

                return !Check.any(target, filter);
            }
        };

        private static boolean any(Object target, String filter) {
            if (target instanceof CharSequence chars) return filter.isBlank() ? !chars.isEmpty() : target.toString().contains(filter);
            if (target instanceof Collection<?> col) return filter.isBlank() ? !col.isEmpty() : col.contains(filter);
            return filter.isBlank() && target != null;
        }
    }

    interface Provider extends BiPredicate<ContextFilter, InteractionContext> {
        /// @return true when the requirement is met by context
        @Override
        boolean test(ContextFilter requirement, InteractionContext context);

        enum Default implements Provider {
            @Instance INSTANCE;

            @Override
            public boolean test(ContextFilter requirement, InteractionContext context) {
                return requirement.check().test(requirement, context);
            }
        }
    }
}
