package org.comroid.commands.model.permission;

import net.kyori.adventure.util.TriState;
import org.intellij.lang.annotations.MagicConstant;

import java.util.UUID;

public interface PermissionAdapter {
    default boolean checkOpLevel(UUID playerId) {
        return checkOpLevel(playerId, 1);
    }

    boolean checkOpLevel(UUID playerId, @MagicConstant(intValues = { 1, 2, 3, 4 }) int minimum);

    default TriState checkPermission(UUID playerId, String key) {return checkPermission(playerId, key, false);}

    TriState checkPermission(UUID playerId, String key, boolean explicit);
}
