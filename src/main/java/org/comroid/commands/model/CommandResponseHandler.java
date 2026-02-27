package org.comroid.commands.model;

import org.comroid.api.func.util.DelegateStream;
import org.comroid.api.java.StackTraceUtils;
import org.comroid.api.text.Capitalization;
import org.comroid.commands.impl.CommandUsage;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

@FunctionalInterface
public interface CommandResponseHandler extends CommandErrorHandler {
    @Deprecated
    default Capitalization getDesiredKeyCapitalization() {
        return Capitalization.IDENTITY;
    }

    void handleResponse(CommandUsage command, @NotNull Object response, Object... args);

    @Override
    default Optional<String> handleThrowable(CommandUsage usage, Throwable throwable) {
        while (throwable instanceof InvocationTargetException itex) throwable = throwable.getCause();
        var msg = "%s: %s".formatted(throwable instanceof CommandError
                                     ? throwable.getClass().getSimpleName()
                                     : StackTraceUtils.lessSimpleName(throwable.getClass()),
                throwable.getMessage() == null
                ? throwable.getCause() == null
                  ? "Internal Error"
                  : throwable.getCause() instanceof CommandError
                    ? throwable.getCause()
                            .getMessage()
                    : throwable.getCause()
                              .getClass()
                              .getSimpleName() + ": " + throwable.getCause().getMessage()
                : throwable.getMessage());
        if (throwable instanceof CommandError) return Optional.of(msg);
        var buf = new StringWriter();
        var out = new PrintStream(new DelegateStream.Output(buf));
        out.println(msg);
        Throwable cause = throwable;
        do {
            var c = cause.getCause();
            if (c == null) break;
            cause = c;
        } while (cause instanceof InvocationTargetException || (cause instanceof RuntimeException && cause.getCause() instanceof InvocationTargetException));
        StackTraceUtils.wrap(cause, out, true);
        var str = buf.toString();
        if (str.length() > 1950) str = str.substring(0, 1950);
        return Optional.of(str);
    }
}
