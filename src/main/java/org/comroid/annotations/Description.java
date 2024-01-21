package org.comroid.annotations;

import org.comroid.annotations.internal.Inherit;
import org.comroid.api.attr.IntegerAttribute;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;

@Retention(RetentionPolicy.RUNTIME)
@Inherit(Inherit.Type.FromSupertype)
@Repeatable(Description.List.class)
public @interface Description {
    Comparator<Description> COMPARATOR = Comparator.comparingInt(desc -> desc.mode().getAsInt());

    String[] value() default {};

    Mode mode() default Mode.Lines;

    @Retention(RetentionPolicy.RUNTIME)
    @Inherit(Inherit.Type.FromSupertype)
    @interface List {
        Description[] value();
    }

    enum Mode implements IntegerAttribute { Usage, Lines, Steps }
}
