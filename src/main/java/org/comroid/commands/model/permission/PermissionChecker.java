package org.comroid.commands.model.permission;

import org.comroid.api.data.seri.type.StandardValueType;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandUsage;
import org.comroid.commands.model.CommandError;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static java.util.function.Predicate.*;
import static org.comroid.api.func.util.Streams.*;

public interface PermissionChecker {
    static PermissionChecker minecraft(final PermissionAdapter adapter) {
        return new PermissionChecker() {
            @Override
            public boolean acceptPermission(String key) {
                return key.matches("[0-4]|\\w+(\\.\\w+)+");
            }

            @Override
            public boolean userHasPermission(CommandUsage usage, Object key) {
                if (key.toString().matches("\\d")) key = Integer.parseInt(key.toString());
                var userId = usage.getContext().stream().flatMap(cast(UUID.class)).findAny().orElseThrow();
                return key instanceof Integer level
                       ? adapter.checkOpLevel(userId, level)
                       : adapter.checkPermission(userId, key.toString(), false).toBooleanOrElse(false);
            }
        };
    }

    static CommandError insufficientPermissions(@Nullable String detail) {
        return new CommandError("You dont have permission to do this " + (detail == null ? "\b" : detail));
    }

    default boolean userHasPermission(CommandUsage usage) {
        return getPermissionKey(usage).filter(key -> userHasPermission(usage, key)).isPresent();
    }

    default Optional<Object> getPermissionKey(CommandUsage usage) {
        return Optional.of(usage.getStackTrace().peek().getAttribute().permission())
                .filter(Predicate.<String>not(Command.EmptyAttribute::equals).and(not(String::isBlank)))
                .map(StandardValueType::findGoodType);
    }

    boolean acceptPermission(String key);
    boolean userHasPermission(CommandUsage usage, Object key);
}
