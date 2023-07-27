package org.comroid.api;

import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Command {
    String EmptyName = "@@@";

    String value() default EmptyName;

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Arg {
        String value() default EmptyName;
    }

    interface Handler {
        void handleResponse(String text);
        void handleError(Throwable t, Delegate cmd, String[] args);
    }

    @Data
    class Manager {
        private final Map<String, Delegate> commands = new ConcurrentHashMap<>();
        private final Handler handler;

        public Manager() {
            this(new Command.Handler(){
                @Override
                public void handleResponse(String text) {
                    System.out.println(text);
                }

                @Override
                public void handleError(Throwable t, Command.Delegate cmd, String[] args) {
                    t.printStackTrace(System.err);
                }
            });
        }

        public Manager(Handler handler) {
            register(this.handler = handler);
        }

        @SuppressWarnings("UnusedReturnValue")
        public int register(final Object target) {
            var cls = target.getClass();
            return (int) Arrays.stream(cls.getMethods())
                    .filter(mtd -> mtd.isAnnotationPresent(Command.class))
                    .map(mtd -> new Delegate(
                            Invocable.ofMethodCall(target, mtd),
                            Optional.of(mtd.getAnnotation(Command.class))
                                    .map(Command::value)
                                    .filter(name -> !name.equals(EmptyName))
                                    .orElseGet(mtd::getName)))
                    .peek(cmd -> commands.put(cmd.name, cmd))
                    .count();
        }

        public @Nullable String execute(String command, Object... extraArgs) {
            var split = command.split(" ");
            var name = split[0];
            var cmd = commands.get(name);
            var args = Arrays.stream(split).skip(1).toArray(String[]::new);
            try {
                String str;
                if ("help".equals(name) && !commands.containsKey("help")) {
                    var sb = new StringBuilder("Commands");
                    for (var each : commands.values())
                        sb.append("\n\t- ").append(each.name);
                    str = sb.toString();
                } else str = String.valueOf(cmd.delegate.autoInvoke(Stream.concat(Stream.of(extraArgs),
                        Stream.of(cmd, args)).toArray()));

                handler.handleResponse(str);
                return str;
            } catch (Throwable t) {
                handler.handleError(t, cmd, args);
            }
            return null;
        }
    }

    @Value
    @SuppressWarnings("ClassExplicitlyAnnotation")
    class Delegate implements Command, Named {
        Invocable<?> delegate;
        String name;

        @Override
        public String value() {
            return null;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Command.class;
        }
    }
}
