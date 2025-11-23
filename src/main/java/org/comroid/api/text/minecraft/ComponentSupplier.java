package org.comroid.api.text.minecraft;

import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ComponentSupplier {
    ComponentLike toComponent();

    default ComponentLike toComponent(UUID playerId) {
        return toComponent();
    }

    interface PlayerFocused extends ComponentSupplier {
        @Override
        TextComponent toComponent();

        @Override
        default ComponentLike toComponent(@Nullable UUID playerId) {
            var component = toComponent();
            if (playerId != null) return specifyComponent(component, playerId);
            return component;
        }

        ComponentLike specifyComponent(TextComponent component, @Nullable UUID playerId);
    }
}
