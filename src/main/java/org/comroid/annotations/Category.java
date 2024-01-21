package org.comroid.annotations;

import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.annotations.internal.Annotations;
import org.comroid.annotations.internal.Inherit;
import org.comroid.api.attr.Described;
import org.comroid.api.attr.Named;
import org.comroid.api.func.util.Cache;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.util.Arrays.stream;

@Retention(RetentionPolicy.RUNTIME)
@Inherit(Inherit.Type.FromBoth)
public @interface Category {
    Adapter None = new Adapter();

    Comparator<Category> COMPARATOR = Comparator.comparingInt(cat -> cat.order()[0].value());

    String value() default "";

    Description[] desc() default {};

    Order[] order() default {};

    @SuppressWarnings("ClassExplicitlyAnnotation")
    @Value
    class Adapter implements Category, Named, Described {
        Category cat;
        List<Description> descriptions = new ArrayList<>();
        @NonFinal
        @Setter
        @Nullable Order order;

        private Adapter() {
            this(null);
        }

        public Adapter(Category cat) {
            this.cat = cat;
        }

        @Override
        public String getName() {
            return value();
        }

        @Override
        public String getDescription() {
            return Annotations.toString(descriptions.toArray(Description[]::new));
        }

        @Override
        public String value() {
            return cat == null ? "" : cat.value();
        }

        @Override
        public Description[] desc() {
            return descriptions.toArray(Description[]::new);
        }

        @Override
        public Order[] order() {
            return order == null ? new Order[0] : new Order[]{order};
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Category.class;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Category c && hashCode() == c.value().hashCode();
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        public static Category.Adapter wrap(Category cat) {
            if (cat instanceof Adapter adp)
                return adp;
            return Cache.<String, Category.Adapter>compute("@Category('" + cat.value() + "')", (k, v) -> {
                final Category.Adapter adp = (v == null ? new Category.Adapter(cat) : v);
                if (adp.order().length < cat.order().length)
                    adp.setOrder(cat.order()[0]);
                stream(cat.desc())
                        .filter(l -> adp.descriptions.stream().noneMatch(
                                r -> l.mode() == r.mode() && Arrays.equals(l.value(), r.value())))
                        .forEachOrdered(adp.descriptions::add);
                return adp;
            });
        }
    }
}
