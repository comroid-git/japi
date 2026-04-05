package org.comroid.eval;

import org.comroid.api.data.seri.type.StandardValueType;
import org.jspecify.annotations.Nullable;

/// todo
public class MinimalExpression {
    public static @Nullable Object evaluate(String expr) {
        return StandardValueType.findGoodType(expr);
    }

    private MinimalExpression() {
        throw new UnsupportedOperationException();
    }
}
