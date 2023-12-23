package org.comroid.api.info;

import org.comroid.api.IntegerAttribute;
import org.comroid.api.LongAttribute;
import org.comroid.api.Named;
import org.comroid.util.Bitmask;
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

            @Override
            public @NotNull Long getValue() {
                return value;
            }

            private Of(String name, long value) {
                super(name);

                this.value = value;
            }
        }
    }
}
