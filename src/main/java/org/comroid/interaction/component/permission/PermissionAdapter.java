package org.comroid.interaction.component.permission;

import org.comroid.interaction.model.InteractionContext;

public interface PermissionAdapter {
    boolean verifyPermission(InteractionContext context);
}
