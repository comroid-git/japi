package org.comroid.api.iter;

import java.util.function.IntUnaryOperator;

@Deprecated
public final class Operator {
    public static IntUnaryOperator intOrder(int... values) {
        class Ordered implements IntUnaryOperator {
            private final int[] arr = values;
            private int i = 0;

            @Override
            public int applyAsInt(int operand) {
                if (i < arr.length) {
                    return arr[i++];
                }

                return -1;
            }
        }

        return new Ordered();
    }
}
