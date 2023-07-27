package org.comroid.api;

import lombok.*;
import lombok.experimental.StandardException;
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
        void handleError(Error error);
    }

    @Value
    class Error extends RuntimeException {
        Delegate cmd;
        String[] args;

        public Error(Delegate cmd, String message, String... args) {
            super(message);
            this.cmd = cmd;
            this.args = args;
        }

        public Error(Delegate cmd, String message, Throwable cause, String... args) {
            super(message, cause);
            this.cmd = cmd;
            this.args = args;
        }
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
                public void handleError(Error error) {
                    error.printStackTrace(System.err);
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
            Error error = null;
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
            } catch (Error e) {
                error = e;
            } catch (Throwable t) {
                error = new Error(cmd, "A fatal error occurred during command execution", t, args);
            }
            handler.handleError(error);
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
