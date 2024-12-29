package org.comroid.api.text;

import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

@Value
@SuppressWarnings("unused")
public class Plural implements Word, Function<@NotNull Number, @NotNull String> {
    static final Map<String, Plural> cache = new ConcurrentHashMap<>();

    String singular;
    String plural;

    Plural(String singular, String plural) {
        this.singular = singular;
        this.plural   = plural;
    }

    @Override
    public String getPrimaryName() {
        return singular;
    }

    @Override
    public String getAlternateName() {
        return plural;
    }

    public Quantified q(Number quantity) {
        return new Quantified(quantity);
    }

    @Override
    public @NotNull String toString() {
        return plural;
    }

    @Override
    public @NotNull String apply(@NotNull Number value) {
        return (int) value == 1 ? singular : plural;
    }

    public final class Quantified implements Word, Supplier<@NotNull String> {
        public static final     String                    DEFAULT_FORMAT = "%d %s";
        private final @NotNull  Supplier<@NotNull Number> quantitySupplier;
        private final @Nullable Supplier<@NotNull String> formatSupplier;

        public Quantified(@NotNull Number quantity) {
            this(quantity, DEFAULT_FORMAT);
        }

        public Quantified(final @NotNull Number quantity, final @Nullable String format) {
            this(() -> quantity, format == null ? null : () -> format);
        }

        public Quantified(@NotNull Supplier<@NotNull Number> quantitySupplier) {
            this(quantitySupplier, () -> DEFAULT_FORMAT);
        }

        public Quantified(@NotNull Supplier<@NotNull Number> quantitySupplier, @Nullable Supplier<@NotNull String> formatSupplier) {
            this.quantitySupplier = quantitySupplier;
            this.formatSupplier   = formatSupplier;
        }

        @Override
        public String getAlternateName() {
            return apply(quantitySupplier.get());
        }

        @Override
        public Plural word() {
            return Plural.this;
        }

        @Override
        public @NotNull String toString() {
            var string = getAlternateName();
            if (formatSupplier != null)
                string = formatSupplier.get().replace("%d", String.valueOf(quantitySupplier.get())).replace("%s", string);
            return string;
        }

        @Override
        public @NotNull String get() {
            return toString();
        }
    }
}
