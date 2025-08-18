package org.comroid.test.api.command;

import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.exc.ThrowingIntFunction;
import org.comroid.api.func.util.Command;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.java.Activator;
import org.comroid.api.text.StringMode;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class AutoFillProviderTest {
    private static final Random RNG = new Random();

    public static void dummy(Object x) {}

    @Test
    @RepeatedTest(20)
    public void durationDirect() throws NoSuchMethodException {
        durationTest(t -> directInvokeProvider(Command.AutoFillProvider.Duration.INSTANCE,
                "parameter",
                String.valueOf(t)));
    }

    @Test
    @RepeatedTest(20)
    public void durationProcessed() throws NoSuchMethodException {
        durationTest(t -> callAutoComplete(Command.AutoFillProvider.Duration.class, String.valueOf(t)));
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

        var results = directInvokeProvider(new Command.AutoFillProvider.Enum(TestEnum.class),
                "parameter",
                currentValue);
        var expected = Arrays.stream(expect).map(TestEnum::name).toArray(String[]::new);
        assertArrayEquals(expected, results);
    }

    private enum TestEnum {
        NumOne, NumTwo, NumThree, NumOnetyOne, Unknown
    }

    private static String[] directInvokeProvider(Command.AutoFillProvider provider, String argName, String currentValue)
    throws NoSuchMethodException {
        try (var mgr = new Command.Manager()) {
            var dcn = dummyCommandNode(provider);
            var usage = Command.Usage.builder()
                    .source(new DummyHandler())
                    .manager(mgr)
                    .fullCommand(new String[]{ "command", currentValue })
                    .baseNode(dcn)
                    .node(dcn)
                    .build();
            return provider.autoFill(usage, argName, currentValue).toArray(String[]::new);
        }
    }

    private static String[] callAutoComplete(
            Class<? extends Command.AutoFillProvider> providerType,
            String currentValue
    ) throws NoSuchMethodException {
        var provider = Activator.get(providerType).createInstance(new DataNode.Object());
        var command  = dummyCommandNode(provider);

        try (var mgr = new Command.Manager()) {
            mgr.getBaseNodes().add(command);
            return mgr.autoComplete(new DummyHandler(),
                    new String[]{ "command", currentValue },
                    "parameter",
                    currentValue).map(Command.AutoFillOption::key).toArray(String[]::new);
        }
    }

    private static Command.Node.Parameter dummyParameterNode(Command.AutoFillProvider provider)
    throws NoSuchMethodException {
        var mtd = AutoFillProviderTest.class.getMethod("dummy", Object.class);

        var argAttr = new Command.Arg() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Command.Arg.class;
            }

            @Override
            public String value() {
                return "parameter";
            }

            @Override
            public int index() {
                return 0;
            }

            @Override
            public String[] autoFill() {
                return new String[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends Command.AutoFillProvider>[] autoFillProvider() {
                return new Class[]{ provider.getClass() };
            }

            @Override
            public boolean required() {
                return false;
            }

            @Override
            public StringMode stringMode() {
                return StringMode.GREEDY;
            }
        };

        return Command.Node.Parameter.builder()
                .name("parameter")
                .attribute(argAttr)
                .param(mtd.getParameters()[0])
                .autoFillProvider(provider)
                .build();
    }

    private static Command.Node.Call dummyCommandNode(Command.AutoFillProvider provider) throws NoSuchMethodException {
        var dpn = dummyParameterNode(provider);
        return dummyCommandNode(dpn);
    }

    private static Command.Node.Call dummyCommandNode(Command.Node.Parameter param) throws NoSuchMethodException {
        var mtd = AutoFillProviderTest.class.getMethod("dummy", Object.class);

        var cmdAttr = new Command() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Command.class;
            }

            @Override
            public String value() {
                return "command";
            }

            @Override
            public String usage() {
                return "command <parameter>";
            }

            @Override
            public String permission() {
                return EmptyAttribute;
            }

            @Override
            public PrivacyLevel privacy() {
                return PrivacyLevel.EPHEMERAL;
            }
        };
        return Command.Node.Call.builder()
                .name("command")
                .attribute(cmdAttr)
                .method(mtd)
                .callable(Invocable.ofMethodCall(mtd))
                .parameter(param)
                .build();
    }

    private static class DummyHandler implements Command.Handler {
        @Override
        public void handleResponse(Command.Usage command, @NotNull Object response, Object... args) {
            System.out.println("response = " + response);
        }
    }
}
