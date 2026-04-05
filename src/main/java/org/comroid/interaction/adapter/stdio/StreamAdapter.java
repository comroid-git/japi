package org.comroid.interaction.adapter.stdio;

import lombok.Value;
import lombok.extern.java.Log;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.ext.Context;
import org.comroid.api.tree.Component;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.component.error.ErrorHandler;
import org.comroid.interaction.component.response.ResponseChain;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.model.Response;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@Log
@Value
public class StreamAdapter extends Component.Base implements ErrorHandler, Runnable, ResponseChain<String> {
    public static void main(String... args) throws IOException {
        try (
                var core = new InteractionCore(Context.root()); var isr = new InputStreamReader(System.in); var br = new BufferedReader(isr);
                var pw = new PrintWriter(System.out)
        ) {
            var adapter = new StreamAdapter(core, br, pw);
            core.addChild(adapter);

            Arrays.stream(args).map(ThrowingFunction.fallback(Class::forName)).filter(Objects::nonNull).forEach(core::register);

            adapter.run();
        }
    }

    InteractionCore core;
    BufferedReader  input;
    PrintWriter     output;

    @Override
    public Class<String> getResponseType() {
        return String.class;
    }

    @Override
    public void sendResponse(InteractionContext context, String response) {
        output.println(response);
    }

    @Override
    public @Nullable String convertResponse(Object object) {
        return String.valueOf(object);
    }

    @Override
    public @Nullable String convertResponse(Response response) {
        return response.content();
    }

    @Override
    public void start() {
        var thread = new Thread(new InputListener());
        addChild(thread);
        thread.start();
    }

    @Override
    public ErrorHandler.State handle(InteractionContext context, Throwable error) {
        log.severe("Encountered an error during interaction execution; " + context);
        error.printStackTrace(output);

        return ErrorHandler.State.UNCHANGED;
    }

    @Override
    public void run() {
        new InputListener().run();
    }

    private InteractionContext createContext(String line) {
        return InteractionContext.basic(core, line.split("\\w+")).build();
    }

    @Value
    private class InputListener implements Runnable {
        Queue<String> queue = new LinkedBlockingQueue<>();

        @Override
        public void run() {
            InteractionContext context = null;
            output.write("> ");

            while (true) try {
                var line = input.readLine();
                if ("exit".equalsIgnoreCase(line)) return;

                (context = createContext(line)).invoke();
                output.write("> ");
            } catch (Throwable t) {
                handle(context, t);
            }
        }
    }
}
