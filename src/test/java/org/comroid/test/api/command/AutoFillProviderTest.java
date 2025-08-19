package org.comroid.test.api.command;

import org.comroid.api.func.exc.ThrowingIntFunction;
import org.comroid.api.func.util.Command;
import org.comroid.util.TestUtil;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class AutoFillProviderTest {
    private static final Random                    RNG    = new Random();
    private static final TestUtil.AutoFillProvider helper = new TestUtil.AutoFillProvider();

    @Test
    @RepeatedTest(20)
    public void durationDirect() throws NoSuchMethodException {
        durationTest(t -> helper.directInvokeProvider(Command.AutoFillProvider.Duration.INSTANCE,
                String.valueOf(t)));
    }

    @Test
    @RepeatedTest(20)
    public void durationProcessed() throws NoSuchMethodException {
        durationTest(t -> helper.callAutoComplete(Command.AutoFillProvider.Duration.class, String.valueOf(t)));
    }

    private void durationTest(ThrowingIntFunction<String[], NoSuchMethodException> results)
    throws NoSuchMethodException {
        var t = RNG.nextInt(300);

        var postfix = new String[]{ "min", "h", "d", "w", "Mon", "y" };
        var output  = new String[postfix.length];
        for (var i = 0; i < postfix.length; i++) output[i] = t + postfix[i];

        assertArrayEquals(output, results.apply(t));
    }

    @Test
    public void enumNoFilter() throws NoSuchMethodException {
        enumTest("", TestEnum.values());
    }

    @Test
    public void enumManyMatchFilter() throws NoSuchMethodException {
        enumTest("Num", TestEnum.NumOne, TestEnum.NumTwo, TestEnum.NumThree, TestEnum.NumOnetyOne);
    }

    @Test
    public void enumOneMatchFilter() throws NoSuchMethodException {
        enumTest("NumOne", TestEnum.NumOne, TestEnum.NumOnetyOne);
    }

    @Test
    public void enumExactMatchFilter() throws NoSuchMethodException {
        enumTest("NumTwo", TestEnum.NumTwo);
    }

    private void enumTest(String currentValue, TestEnum... expect) throws NoSuchMethodException {
        var enums = TestEnum.values();
        var x     = enums[RNG.nextInt(enums.length)];

        var results = helper.directInvokeProvider(new Command.AutoFillProvider.Enum(TestEnum.class),
                currentValue);
        var expected = Arrays.stream(expect).map(TestEnum::name).toArray(String[]::new);
        assertArrayEquals(expected, results);
    }

    private enum TestEnum {
        NumOne, NumTwo, NumThree, NumOnetyOne, Unknown
    }
}
