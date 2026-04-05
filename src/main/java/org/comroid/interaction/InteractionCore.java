package org.comroid.interaction;

import lombok.Value;
import lombok.extern.java.Log;
import org.comroid.api.tree.Component;
import org.comroid.interaction.component.RegistryHandler;
import org.comroid.interaction.component.error.ErrorHandler;
import org.comroid.interaction.component.response.ResponseConverter;
import org.comroid.interaction.component.response.ResponseHandler;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.model.InteractionTree;
import org.comroid.interaction.registry.InstanceRegistry;
import org.comroid.interaction.registry.RegistrySource;
import org.comroid.interaction.registry.TypeRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

@Log
@Value
public class InteractionCore extends Component.Base implements RegistryHandler {
    public static final String NO_DESCRIPTION = "No description";

    Set<InteractionTree> registered = new HashSet<>();

    @Override
    public Collection<InteractionTree> getRegistered() {
        return Collections.unmodifiableSet(registered);
    }

    @Override
    public void register(InteractionTree tree) {
        components(RegistryHandler.class).forEach(registry -> registry.register(tree));
        registered.add(tree);
    }

    public void register(RegistrySource source) {
        register(source.loadTree());
    }

    public void register(Class<?> target) {
        register(new TypeRegistry(target));
    }

    public void register(Object target) {
        register(new InstanceRegistry(target));
    }

    /// verify minimum required components
    @Override
    protected void $lateInitialize() {
        verifyComponentExists(ResponseConverter.class);
        verifyComponentExists(ResponseHandler.class);
    }

    private void verifyComponentExists(Class<?> type) {
        child(type).assertion("No component of type %s was found".formatted(type.getCanonicalName()));
    }

    public void handle(InteractionContext context, Throwable error) {
        log.log(Level.SEVERE, "Encountered an error during interaction handling", error);

        var handlers = context.children(ErrorHandler.class).iterator();
        if (!handlers.hasNext()) return;

        do {
            var state = handlers.next().handle(context, error);
            if (state != ErrorHandler.State.UNCHANGED) break;
        } while (handlers.hasNext());
    }
}
