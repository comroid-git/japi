package org.comroid.api.info;

import org.comroid.api.IntegerAttribute;
import org.comroid.api.Named;
import org.comroid.util.Bitmask;
import org.jetbrains.annotations.NotNull;

public interface NamedGroup extends Named, IntegerAttribute {
    @Override
    String getName();

    @Override
    @NotNull
    Integer getValue();

    static NamedGroup of(String name) {
        return of(name, Bitmask.nextFlag(1));
    }

    static NamedGroup of(String name, int value) {
        return new Support.Of(name, value);
    }

    final class Support {
        private static final class Of extends Base implements NamedGroup {
            private final int value;

            @Override
            public @NotNull Integer getValue() {
                return value;
            }

            private Of(String name, int value) {
                super(name);

                this.value = value;
            }
        }
    }
}
