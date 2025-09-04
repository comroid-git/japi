package org.comroid.commands.impl.system;

import lombok.SneakyThrows;
import lombok.Value;
import org.comroid.commands.impl.AbstractCommandAdapter;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.impl.CommandUsage;
import org.comroid.commands.model.CommandCapability;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.comroid.api.func.util.Debug.*;

@Value
public class ConsoleCommandAdapter extends AbstractCommandAdapter {
    CommandManager manager;
    InputStream    in;
    PrintStream out;

    public ConsoleCommandAdapter(CommandManager manager) {
        this(manager, System.in, System.out);
    }

    public ConsoleCommandAdapter(CommandManager manager, InputStream in, OutputStream out) {
        this.manager = manager;
        this.in      = in;
        this.out     = out instanceof PrintStream ps ? ps : new PrintStream(out);
    }

    @Override
    public Set<CommandCapability> getCapabilities() {
        return Set.of();
    }

    @Override
    public void initialize() {
        CompletableFuture.supplyAsync(this::inputReader, Executors.newSingleThreadExecutor())
                .exceptionally(exceptionLogger("A fatal error occurred in the Input reader"));
    }

    @Override
    public void handleResponse(CommandUsage command, @NotNull Object response, Object... args) {
        out.println(response);
    }

    @SneakyThrows
    private Void inputReader() {
        try (
                var isr = new InputStreamReader(in); var br = new BufferedReader(isr)
        ) {
            String line;
            do {
                line = br.readLine();
                if ("exit".equals(line)) break;
                manager.execute(this, line.split(" "), Map.of(), manager);
            } while (true);
            System.exit(0);
        }
        return null;
    }
}
