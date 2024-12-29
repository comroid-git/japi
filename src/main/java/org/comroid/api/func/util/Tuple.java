package org.comroid.api.func.util;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public interface Tuple {
    @Data
    class N2<A, B> implements Tuple {
        public static <K, V> Collector<N2<K, V>, ?, @NotNull Map<K, @NotNull V>> toMap() {
            return Collectors.toMap(N2::getA, N2::getB);
        }

        public A a;
        public B b;

        public N2() {
            this(null, null);
        }

        public N2(Map.Entry<A, B> entry) {
            this(entry.getKey(), entry.getValue());
        }

        public N2(A a, B b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(a, b);
        }

        @Override
        public final boolean equals(Object obj) {
            return obj instanceof N2<?, ?> other && other.hashCode() == hashCode();
        }

        @Override
        public String toString() {
            return "( %s , %s )".formatted(a, b);
        }
    }
}
