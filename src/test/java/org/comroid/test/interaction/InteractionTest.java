package org.comroid.test.interaction;

import org.comroid.interaction.adapter.stdio.StreamAdapter;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.annotation.ContextFilter;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.junit.Test;

import java.io.IOException;
import java.util.function.IntBinaryOperator;

public class InteractionTest {
    @Interaction(filter = { @ContextFilter(value = "permission.discord", filter = "8") })
    public static int math(
            @Parameter Operator operator, @Parameter(completion = { @Completion(strings = { "1", "2" }) }) int x,
            @Parameter(completion = { @Completion(strings = { "3", "4" }) }) int y
    ) {
        return operator.applyAsInt(x, y);
    }

    public static void main(String... args) throws IOException {
        StreamAdapter.main(InteractionTest.class.getCanonicalName());
    }

    @Test
    public void testStdio() throws IOException {
        main();
    }

    public enum Operator implements IntBinaryOperator {
        plus {
            @Override
            public int applyAsInt(int left, int right) {
                return left + right;
            }
        }, minus {
            @Override
            public int applyAsInt(int left, int right) {
                return left - right;
            }
        }, multiply {
            @Override
            public int applyAsInt(int left, int right) {
                return left * right;
            }
        }, divide {
            @Override
            public int applyAsInt(int left, int right) {
                return left / right;
            }
        }
    }
}
