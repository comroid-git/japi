package org.comroid.api.info;

import org.comroid.api.attr.LongAttribute;
import org.comroid.api.attr.Named;
import org.comroid.api.func.util.Bitmask;
import org.jetbrains.annotations.NotNull;

@Deprecated(forRemoval = true)
public interface NamedGroup extends Named, LongAttribute {
    static NamedGroup of(String name) {
        return of(name, Bitmask.nextFlag(1));
    }

    static NamedGroup of(String name, long value) {
        return new Support.Of(name, value);
    }

    final class Support {
        private static final class Of extends Base implements NamedGroup {
            private final long value;

            private Of(String name, long value) {
                super(name);

                this.value = value;
            }

            @Override
            public @NotNull Long getValue() {
                return value;
            }
        }
    }
}
