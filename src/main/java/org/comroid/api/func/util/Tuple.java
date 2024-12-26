package org.comroid.api.func.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

public interface Tuple {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class N2<A, B> implements Tuple {
        public A a;
        public B b;

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
