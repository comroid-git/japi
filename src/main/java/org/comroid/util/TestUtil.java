package org.comroid.util;

import lombok.Getter;
import lombok.SneakyThrows;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.util.Command;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.java.Activator;
import org.comroid.api.text.StringMode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TestUtil {
    public static final Random rng = new Random();

    @SneakyThrows
    public static String fillWithText(File file, int length) {
        var buf = new StringWriter();
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            while (length-- > 0) {
                byte c = (byte) (rng.nextInt('A', 'z' + 1) % 256);
                buf.append((char) c);
                fos.write(c);
            }

            fos.flush();
            return buf.toString();
        }
    }

    public static class AutoFillProvider extends TestUtil {
        public static void $dummy(Object x) {}

        final @Getter Set<Object> context = new HashSet<>();

        public void testCaseDirect(
                String name, Command.AutoFillProvider provider, String currentValue,
                String... expected
        ) {
            Assertions.assertArrayEquals(expected, directInvokeProvider(provider, currentValue), name);
        }

        public void testCaseCall(
                String name, Class<? extends Command.AutoFillProvider> provider, String currentValue,
                String... expected
        ) {
            Assertions.assertArrayEquals(expected, callAutoComplete(provider, currentValue), name);
        }

        public String[] directInvokeProvider(Command.AutoFillProvider provider, String currentValue) {
            try (var mgr = new Command.Manager()) {
                var command = dummyCommandNode(provider);
                var usage   = dummyCommandUsage(mgr, currentValue, command);
                return provider.autoFill(usage, "parameter", currentValue).toArray(String[]::new);
            }
        }

        public String[] callAutoComplete(Class<? extends Command.AutoFillProvider> providerType, String currentValue) {
            var provider = Activator.get(providerType).createInstance(new DataNode.Object());
            var command  = dummyCommandNode(provider);

            try (var mgr = new Command.Manager()) {
                mgr.getBaseNodes().add(command);
                var usage = dummyCommandUsage(mgr, currentValue, command);
                return mgr.autoComplete(usage, "parameter", currentValue)
                        .map(Command.AutoFillOption::key)
                        .toArray(String[]::new);
            }
        }

        private Command.Usage dummyCommandUsage(
                Command.Manager mgr, String currentValue,
                Command.Node.Callable dummyCommandNode
        ) {
            return Command.Usage.builder()
                    .source(new DummyHandler())
                    .manager(mgr)
                    .fullCommand(new String[]{ "command", currentValue })
                    .context(context)
                    .baseNode(dummyCommandNode)
                    .build();
        }

        private Command.Node.Parameter dummyParameterNode(Command.AutoFillProvider provider) {
            Method mtd = null;
            try {
                mtd = AutoFillProvider.class.getMethod("$dummy", Object.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

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

        private Command.Node.Call dummyCommandNode(Command.AutoFillProvider provider) {
            var dpn = dummyParameterNode(provider);
            return dummyCommandNode(dpn);
        }

        private Command.Node.Call dummyCommandNode(Command.Node.Parameter param) {
            Method mtd = null;
            try {
                mtd = AutoFillProvider.class.getMethod("$dummy", Object.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

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
}
