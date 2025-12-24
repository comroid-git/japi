package org.comroid.commands.model.permission;

import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface MinecraftPermissionAdapter {
    static MinecraftPermissionAdapter spigot() {
        return new MinecraftPermissionAdapter() {
            @Override
            public boolean checkOpLevel(UUID playerId, int $) {
                return Bukkit.getOperators().stream().map(OfflinePlayer::getUniqueId).anyMatch(playerId::equals);
            }

            @Override
            public TriState checkPermission(UUID playerId, String key, boolean explicit) {
                var player = Bukkit.getPlayer(playerId);
                if (player == null) return TriState.FALSE;
                var perms = player.getEffectivePermissions()
                        .stream()
                        .map(PermissionAttachmentInfo::getPermission)
                        .toList();
                return TriState.byBoolean(explicit
                                          ? perms.contains(key)
                                          : perms.stream().anyMatch(perm -> perm.startsWith(key)));
            }
        };
    }

    default boolean checkOpLevel(UUID playerId) {
        return checkOpLevel(playerId, 1);
    }

    boolean checkOpLevel(UUID playerId, @MagicConstant(intValues = { 1, 2, 3, 4 }) int minimum);

    default TriState checkPermission(UUID playerId, @NotNull String key) {return checkPermission(playerId, key, false);}

    TriState checkPermission(UUID playerId, @NotNull String key, boolean explicit);
}
