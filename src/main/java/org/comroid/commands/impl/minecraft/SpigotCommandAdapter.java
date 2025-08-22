package org.comroid.commands.impl.minecraft;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.comroid.commands.autofill.AutoFillOption;
import org.comroid.commands.impl.AbstractCommandAdapter;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.impl.CommandUsage;
import org.comroid.commands.model.CommandCapability;
import org.comroid.commands.model.CommandContextProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.stream.Stream.*;
import static java.util.stream.Stream.of;
import static net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.*;
import static org.comroid.api.func.util.Streams.*;

@Value
@NonFinal
@RequiredArgsConstructor
public class SpigotCommandAdapter extends AbstractCommandAdapter
        implements MinecraftResponseHandler, TabCompleter, CommandExecutor, CommandContextProvider {
    CommandManager manager;
    Set<CommandCapability> capabilities = Set.of();
    JavaPlugin             plugin;

    @Override
    public void initialize() {
        super.initialize();

        manager.addChild(this);
    }

    @Override
    public Stream<?> expandContext(Object it) {
        if (it instanceof Player player) return of(player.getUniqueId());
        return empty();
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String alias,
            @NotNull String[] args
    ) {
        if (alias.contains(":")) alias = alias.substring(alias.indexOf(':') + 1);
        var strings = strings(alias, args);
        var usage   = manager.createUsageBase(this, strings, expandContext(sender).toArray());
        return manager.autoComplete(usage, String.valueOf(args.length - 1), strings[strings.length - 1])
                .map(AutoFillOption::key)
                .toList();
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label,
            @NotNull String[] args
    ) {
        if (label.contains(":")) label = label.substring(label.indexOf(':') + 1);
        var strings = strings(label, args);
        var usage   = manager.createUsageBase(this, strings, expandContext(sender).toArray());
        manager.execute(usage, null);
        return true;
    }

    @Override
    public void handleResponse(CommandUsage command, @NotNull Object response, Object... args) {
        if (response instanceof CompletableFuture<?> future) {
            future.thenAcceptAsync(late -> handleResponse(command, late, args));
            return;
        }
        var sender = Arrays.stream(args).flatMap(cast(CommandSender.class)).findAny().orElseThrow();
        if (response instanceof Component component) sender.spigot().sendMessage(get().serialize(component));
        else sender.sendMessage(String.valueOf(response));
    }
}
