package org.comroid.util;

import lombok.Getter;
import lombok.SneakyThrows;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.util.Invocable;
import org.comroid.api.java.Activator;
import org.comroid.api.text.StringMode;
import org.comroid.commands.Command;
import org.comroid.commands.autofill.AutoFillOption;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.impl.CommandUsage;
import org.comroid.commands.model.CommandPrivacyLevel;
import org.comroid.commands.model.CommandResponseHandler;
import org.comroid.commands.node.Call;
import org.comroid.commands.node.Callable;
import org.comroid.commands.node.Parameter;
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
                String name, IAutoFillProvider provider, String currentValue,
                String... expected
        ) {
            Assertions.assertArrayEquals(expected, directInvokeProvider(provider, currentValue), name);
        }

        public void testCaseCall(
                String name, Class<? extends IAutoFillProvider> provider,
                String currentValue,
                String... expected
        ) {
            Assertions.assertArrayEquals(expected, callAutoComplete(provider, currentValue), name);
        }

        public String[] directInvokeProvider(
                IAutoFillProvider provider, String currentValue) {
            try (var mgr = new CommandManager()) {
                var command = dummyCommandNode(provider);
                var usage   = dummyCommandUsage(mgr, currentValue, command);
                return provider.autoFill(usage, "parameter", currentValue).toArray(String[]::new);
            }
        }

        public String[] callAutoComplete(
                Class<? extends IAutoFillProvider> providerType,
                String currentValue
        ) {
            var provider = Activator.get(providerType).createInstance(new DataNode.Object());
            var command  = dummyCommandNode(provider);

            try (var mgr = new CommandManager()) {
                mgr.getBaseNodes().add(command);
                var usage = dummyCommandUsage(mgr, currentValue, command);
                return mgr.autoComplete(usage, "parameter", currentValue)
                        .map(AutoFillOption::key)
                        .toArray(String[]::new);
            }
        }

        private CommandUsage dummyCommandUsage(
                CommandManager mgr, String currentValue,
                Callable dummyCommandNode
        ) {
            return CommandUsage.builder()
                    .source(new DummyHandler())
                    .manager(mgr)
                    .fullCommand(new String[]{ "command", currentValue })
                    .context(context)
                    .baseNode(dummyCommandNode)
                    .build();
        }

        private Parameter dummyParameterNode(IAutoFillProvider provider) {
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
                public Class<? extends IAutoFillProvider>[] autoFillProvider() {
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

            return Parameter.builder()
                    .name("parameter")
                    .attribute(argAttr)
                    .param(mtd.getParameters()[0])
                    .autoFillProvider(provider)
                    .build();
        }

        private Call dummyCommandNode(IAutoFillProvider provider) {
            var dpn = dummyParameterNode(provider);
            return dummyCommandNode(dpn);
        }

        private Call dummyCommandNode(Parameter param) {
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
                public CommandPrivacyLevel privacy() {
                    return CommandPrivacyLevel.EPHEMERAL;
                }
            };
            return Call.builder()
                    .name("command")
                    .attribute(cmdAttr)
                    .method(mtd)
                    .callable(Invocable.ofMethodCall(mtd))
                    .parameter(param)
                    .build();
        }

        private static class DummyHandler implements CommandResponseHandler {
            @Override
            public void handleResponse(CommandUsage command, @NotNull Object response, Object... args) {
                System.out.println("response = " + response);
            }
        }
    }
}
