package org.comroid.interaction.adapter.jda;

import lombok.Value;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.comroid.interaction.component.permission.PermissionAdapter;
import org.comroid.interaction.model.InteractionContext;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Value
class DiscordPermissionAdapter implements PermissionAdapter {
    JdaAdapter adp;

    @Override
    public boolean verifyPermission(InteractionContext context) {
        var member = context.child(Member.class).orElse(null);
        var list = context.getNode()
                .getDefinitionValues(JdaAdapter.KEY_PERMISSION)
                .filter(Predicate.not(String::isBlank))
                .flatMap(DiscordPermissionAdapter::parsePermission)
                .toList();
        return member == null ? list.isEmpty() : member.hasPermission(list);
    }

    static Stream<Permission> parsePermission(String string) {
        return string.matches("\\d+")
               ? Permission.getPermissions(Long.parseLong(string)).stream()
               : Arrays.stream(Permission.values()).filter(it -> it.name().equalsIgnoreCase(string));
    }
}
